package dev.hrx.libregadgets.core.communication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.DefineComponent
import dagger.hilt.android.AndroidEntryPoint
import dev.hrx.libregadgets.core.data.message.Measurement
import dev.hrx.libregadgets.core.data.message.Thresholds
import dev.hrx.libregadgets.core.interfaces.MeasurementListener
import dev.hrx.libregadgets.core.storage.GlucoseMeasurement
import dev.hrx.libregadgets.core.storage.GlucoseThresholds
import dev.hrx.libregadgets.core.storage.SharedStorage
import dev.hrx.libregadgets.core.utils.isMeasurementStale
import dev.hrx.libregadgets.core.utils.toProtoTimestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@AndroidEntryPoint
class GlucoseEventReceiver : BroadcastReceiver() {
    private var storage: SharedStorage? = null
    private val json = Json { ignoreUnknownKeys = true }
    private var lastMeasurement: GlucoseMeasurement? = null
    private var lastThresholds: GlucoseThresholds? = null

    @Inject
    lateinit var measurementListener: MeasurementListener

    @Inject
    lateinit var wearDataSource: GlucoseWearSyncDataSource

    override fun onReceive(context: Context?, intent: Intent?) = goAsync {
        Log.d("GlucoseEventReceiver", "new broadcast!! ${intent?.action}")
        if (context == null || intent == null) {
            return@goAsync // not much we can do, not enough data
        }

        if (storage == null) storage = SharedStorage(context)

        val latestMeasurement = storage!!.latestMeasurement;

        val isStale = latestMeasurement != null && isMeasurementStale(
            System.currentTimeMillis(), latestMeasurement
        )

        val rawMeasurement = intent.getStringExtra("measurement")
        val rawThresholds = intent.getStringExtra("thresholds")

        val hasNewMeasurement = handleMeasurement(rawMeasurement)
        val hasNewThresholds = handleThresholds(rawThresholds)

        if (hasNewMeasurement || hasNewThresholds || isStale) {
            measurementListener.onMeasurementReceived(context)

            if (hasNewMeasurement && lastMeasurement != null) {
                val protoMeasurement =
                    Measurement.GlucoseMeasurement.newBuilder().apply {
                        value = lastMeasurement!!.value
                        evaluation =
                            Measurement.MeasurementEvaluation.forNumber(lastMeasurement!!.evaluation.ordinal)
                        trend =
                            Measurement.MeasurementTrend.forNumber(lastMeasurement!!.trend.ordinal)
                        timestamp = lastMeasurement!!.timestamp.toProtoTimestamp()
                    }
                wearDataSource.notifyNewMeasurement(protoMeasurement.build())
            }

            if (hasNewThresholds && lastThresholds != null) {
                val protoThresholds =
                    Thresholds.GlucoseThresholds.newBuilder().apply {
                        low = lastThresholds!!.low
                        high = lastThresholds!!.high
                        //timestamp = lastThresholds!!.timestamp.toProtoTimestamp()
                    }
                wearDataSource.notifyNewThresholds(protoThresholds.build())
            }
        }
    }

    private fun handleMeasurement(rawMeasurement: String?): Boolean {
        if (rawMeasurement == null) return false

        val measurement = json.decodeFromString<GlucoseMeasurement>(rawMeasurement)

        if (lastMeasurement != null && measurement == lastMeasurement) return false

        storage?.latestMeasurement = measurement
        lastMeasurement = measurement

        return true
    }

    private fun handleThresholds(rawThresholds: String?): Boolean {
        if (rawThresholds == null) return false

        val thresholds = json.decodeFromString<GlucoseThresholds>(rawThresholds)

        if (lastThresholds != null && thresholds == lastThresholds) return false

        storage?.glucoseThresholds = thresholds
        lastThresholds = thresholds

        return true
    }

    private fun BroadcastReceiver.goAsync(
        context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> Unit
    ) {
        val pendingResult = goAsync()
        @OptIn(DelicateCoroutinesApi::class) // Must run globally; there's no teardown callback.
        GlobalScope.launch(context) {
            try {
                block()
            } finally {
                pendingResult.finish()
            }
        }
    }
}