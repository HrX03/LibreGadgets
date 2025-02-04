package dev.hrx.libregadgets.core.communication

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import dev.hrx.libregadgets.core.api.LibreLinkApi
import dev.hrx.libregadgets.core.storage.GlucoseMeasurement
import dev.hrx.libregadgets.core.storage.GlucoseThresholds
import dev.hrx.libregadgets.core.storage.MeasurementEvaluation
import dev.hrx.libregadgets.core.storage.MeasurementTrend
import dev.hrx.libregadgets.core.storage.SharedStorage
import dev.hrx.libregadgets.core.utils.defaultEventDelay
import dev.hrx.libregadgets.core.utils.getDelayForNextEvent
import dev.hrx.libregadgets.core.utils.getTimestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json.Default.encodeToString
import android.icu.text.DateFormat
import dagger.hilt.android.AndroidEntryPoint
import dev.hrx.libregadgets.core.interfaces.NotificationHelper
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class GlucosePollService : Service() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val repeatingIntentBroadcastReceiver = RepeatingIntentBroadcastReceiver()

    private lateinit var api: LibreLinkApi
    private lateinit var storage: SharedStorage
    private lateinit var alarmManager: AlarmManager

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private inner class RepeatingIntentBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Time to take a measurement")
            scope.launch {
                takeMeasurement()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationHelper.createNotificationChannel(this)

        ServiceCompat.startForeground(
            this,
            1,
            notificationHelper.buildNotification(this),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            else 0
        )

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "rise and shine")

        api = LibreLinkApi(this)
        storage = SharedStorage(this)
        alarmManager = getSystemService<AlarmManager>()!!

        val recIntent = ContextCompat.registerReceiver(
            this,
            repeatingIntentBroadcastReceiver,
            IntentFilter("dev.hrx.libregadgets.broadcast.GET_MEASUREMENT"),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        Log.d(TAG, "Received broadcast receiver with intent $recIntent")

        startMeasurements()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "OUGH")

        unregisterReceiver(repeatingIntentBroadcastReceiver)

        job.cancel()
        scope.coroutineContext.cancelChildren()
    }

    private fun startMeasurements() {
        val intent =
            Intent("dev.hrx.libregadgets.broadcast.GET_MEASUREMENT").setPackage(packageName)
        sendBroadcast(intent)
    }

    private suspend fun takeMeasurement() {
        val connectionResponse = api.getConnection()
        if (connectionResponse == null) {
            // We send an empty measurement because the broadcast receiver is also in charge
            // of checking if the current data we hold is stale or not, if we don't have net
            // we still need to send this broadcast as a "ping" event to make sure it still
            // gets checked on.
            val intent = Intent().also { intent ->
                intent.setAction("dev.hrx.libregadgets.broadcast.NEW_MEASUREMENT")
                intent.setPackage("dev.hrx.libregadgets")
            }

            this@GlucosePollService.sendBroadcast(intent)
            scheduleNextIntent(storage.latestMeasurement)
            return
        }

        val patient = connectionResponse.data.first()

        val measurement = GlucoseMeasurement(
            value = patient.glucoseMeasurement.value,
            evaluation = when {
                patient.glucoseMeasurement.isHigh -> MeasurementEvaluation.High
                patient.glucoseMeasurement.isLow -> MeasurementEvaluation.Low
                else -> MeasurementEvaluation.Normal
            },
            trend = when (patient.glucoseMeasurement.trendArrow) {
                1 -> MeasurementTrend.FallQuick
                2 -> MeasurementTrend.Fall
                3 -> MeasurementTrend.Normal
                4 -> MeasurementTrend.Rise
                5 -> MeasurementTrend.RiseQuick
                else -> MeasurementTrend.Unknown
            },
            timestamp = getTimestamp(patient.glucoseMeasurement.timestamp)
        )

        val thresholds = GlucoseThresholds(
            low = patient.targetLow,
            high = patient.targetHigh,
        )

        Log.d(TAG, "emitting measurement")

        val intent = Intent().also { intent ->
            intent.setAction("dev.hrx.libregadgets.broadcast.NEW_MEASUREMENT")
            intent.putExtra(
                "measurement",
                encodeToString(GlucoseMeasurement.serializer(), measurement)
            )
            intent.putExtra(
                "thresholds",
                encodeToString(GlucoseThresholds.serializer(), thresholds)
            )
            intent.setPackage("dev.hrx.libregadgets")
        }

        val previousMeasurement = storage.latestMeasurement
        this@GlucosePollService.sendBroadcast(intent)
        scheduleNextIntent(measurement, previousMeasurement)
    }

    @SuppressLint("MissingPermission")
    private fun scheduleNextIntent(
        measurement: GlucoseMeasurement?,
        previousMeasurement: GlucoseMeasurement? = null
    ) {
        val intent =
            Intent("dev.hrx.libregadgets.broadcast.GET_MEASUREMENT").setPackage(packageName)
        val currentTime = System.currentTimeMillis()

        val hasMeasurement = measurement != null
        val hasPreviousMeasurement = previousMeasurement != null
        val measurementIsNewer =
            hasMeasurement && hasPreviousMeasurement && measurement!!.timestamp > previousMeasurement!!.timestamp;

        var delayForNextEvent: Long
        if (measurementIsNewer || hasMeasurement && !hasPreviousMeasurement) {
            delayForNextEvent = getDelayForNextEvent(
                currentTime,
                measurement!!.timestamp
            )
        } else if (hasMeasurement) {
            val timeDiff = currentTime - measurement!!.timestamp

            /// Check if the interval between current time and measured timestamp is within the
            /// default event delay value + 4 seconds (net debounce - 1 second).
            /// The check is required because we want to do a fast net retry only once, else we could
            /// potentially spam the system with requests (not that good)

            if (timeDiff < Duration.of(defaultEventDelay + 4000, ChronoUnit.MILLIS).toMillis()) {
                Log.d(TAG, "Catched a net debounce!! Calculated offset is $timeDiff")
                /// If we see the current time is within the allowed net debouncing time we just
                /// set the next request to be 5 seconds from now
                delayForNextEvent = 5000
            } else {
                Log.d(TAG, "Net debounce failed as offset is too large :( Calculated offset is $timeDiff")
                /// We potentially already did a net debounce check, don't do it again and wait
                /// for the next measurement
                delayForNextEvent = defaultEventDelay
            }
        } else {
            delayForNextEvent = defaultEventDelay
        }

        Log.d(
            TAG,
            "Calculated delay of ${
                DateFormat.getInstanceForSkeleton("mm ss").format(Date(delayForNextEvent))
            }, raw $delayForNextEvent"
        )
        if (delayForNextEvent.floorDiv(1000) <= 0) {
            /// Such a situation can happen when there is no new measurement (no net for example)
            /// and as such if we didn't do this check we would be spamming the system with requests
            /// (not that good).
            /// We set the delay to half the default one to eventually re-sync in case of delayed uploads
            delayForNextEvent = defaultEventDelay / 2
        }

        val formatter = DateFormat.getInstanceForSkeleton("h mm ss")

        Log.d(TAG, "Current value: ${measurement?.value ?: "idk"}")
        Log.d(
            TAG,
            "Measurement time: ${if (measurement != null) formatter.format(Date(measurement.timestamp)) else "none"}"
        )
        Log.d(TAG, "Current time: ${formatter.format(Date(currentTime))}")
        Log.d(TAG, "Next check time: ${formatter.format(Date(currentTime + delayForNextEvent))}")

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC,
            currentTime + delayForNextEvent,
            PendingIntent.getBroadcast(
                this@GlucosePollService,
                0,
                intent,
                FLAG_IMMUTABLE,
            )
        )
    }

    companion object {
        private const val TAG = "GlucosePollService"
    }
}
