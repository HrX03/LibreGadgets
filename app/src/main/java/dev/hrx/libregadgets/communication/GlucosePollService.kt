package dev.hrx.libregadgets.communication

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
import dev.hrx.libregadgets.MainActivity
import dev.hrx.libregadgets.R
import dev.hrx.libregadgets.api.LibreLinkApi
import dev.hrx.libregadgets.storage.GlucoseMeasurement
import dev.hrx.libregadgets.storage.GlucoseThresholds
import dev.hrx.libregadgets.storage.MeasurementEvaluation
import dev.hrx.libregadgets.storage.MeasurementTrend
import dev.hrx.libregadgets.utils.getTimestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json.Default.encodeToString
import java.text.DateFormat
import java.util.Date
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class GlucosePollService : Service() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val repeatingIntentBroadcastReceiver = RepeatingIntentBroadcastReceiver()

    private lateinit var api: LibreLinkApi
    private lateinit var alarmManager: AlarmManager

    private inner class RepeatingIntentBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Time to take a measurement")
            takeMeasurement()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationsHelper.createNotificationChannel(this)

        ServiceCompat.startForeground(
            this,
            1,
            NotificationsHelper.buildNotification(this),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            else 0
        )

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "rise and shine")

        api = LibreLinkApi(this)
        alarmManager = getSystemService<AlarmManager>()!!

        val recIntent = ContextCompat.registerReceiver(
            this,
            repeatingIntentBroadcastReceiver,
            IntentFilter("dev.hrx.libregadgets.broadcast.get_measurement"),
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

    private fun startMeasurements(
        delay: Duration = TICKER_PERIOD_DELAY,
    ) {
        val intent =
            Intent("dev.hrx.libregadgets.broadcast.get_measurement").setPackage(packageName)
        sendBroadcast(intent)
        alarmManager.setRepeating(
            AlarmManager.RTC,
            System.currentTimeMillis() + delay.inWholeMilliseconds,
            delay.inWholeMilliseconds,
            PendingIntent.getBroadcast(
                this@GlucosePollService,
                0,
                intent,
                FLAG_IMMUTABLE,
            )
        )
    }

    private fun takeMeasurement() {
        scope.launch {
            val connectionResponse = api.getConnection()
            if (connectionResponse == null) {
                // We send an empty measurement because the broadcast receiver is also in charge
                // of checking if the current data we hold is stale or not, if we don't have net
                // we still need to send this broadcast as a "ping" event to make sure it still
                // gets checked on.
                val intent = Intent().also { intent ->
                    intent.setAction("dev.hrx.libregadgets.broadcast.new_measurement")
                    intent.setPackage("dev.hrx.libregadgets")
                }

                this@GlucosePollService.sendBroadcast(intent)
                return@launch
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
                intent.setAction("dev.hrx.libregadgets.broadcast.new_measurement")
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

            this@GlucosePollService.sendBroadcast(intent)
        }
    }

    companion object {
        private const val TAG = "GlucosePollService"
        private val TICKER_PERIOD_DELAY = 30.seconds
    }
}

private object NotificationsHelper {
    private const val NOTIFICATION_CHANNEL_ID = "general_notification_channel"

    fun createNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService<NotificationManager>()

        // create the notification channel
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.foreground_service_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager?.createNotificationChannel(channel)
    }

    fun buildNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.foreground_service_notification_title))
            .setContentText(context.getString(R.string.foreground_service_notification_description))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(Intent(
                context, MainActivity::class.java
            ).let { notificationIntent ->
                PendingIntent.getActivity(
                    context, 0, notificationIntent, FLAG_IMMUTABLE
                )
            }).build()
    }
}