package dev.hrx.libregadgets.communication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.hrx.libregadgets.gadgets.updateComplication
import dev.hrx.libregadgets.gadgets.updateWidget
import dev.hrx.libregadgets.storage.GlucoseMeasurement
import dev.hrx.libregadgets.storage.GlucoseThresholds
import dev.hrx.libregadgets.storage.SharedStorage
import dev.hrx.libregadgets.utils.isMeasurementStale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class GlucoseEventReceiver : BroadcastReceiver() {
    private var storage: SharedStorage? = null
    private val json = Json { ignoreUnknownKeys = true }
    private var lastMeasurement: GlucoseMeasurement? = null
    private var lastThresholds: GlucoseThresholds? = null

    override fun onReceive(context: Context?, intent: Intent?) = goAsync {
        Log.d("GlucoseEventReceiver", "new broadcast!! ${intent?.action}")
        if (context == null || intent == null) {
            return@goAsync // not much we can do, not enough data
        }

        if (storage == null) storage = SharedStorage(context)

        val latestMeasurement = storage!!.latestMeasurement;

        val isStale = latestMeasurement != null && isMeasurementStale(
            System.currentTimeMillis(),
            latestMeasurement
        )

        val rawMeasurement = intent.getStringExtra("measurement")
        val rawThresholds = intent.getStringExtra("thresholds")

        val hasNewMeasurement = handleMeasurement(rawMeasurement)
        val hasNewThresholds = handleThresholds(rawThresholds)

        if(hasNewMeasurement || hasNewThresholds || isStale) {
            updateWidget(context)
            updateComplication(context)
        }
    }

    private fun handleMeasurement(rawMeasurement: String?): Boolean {
        if(rawMeasurement == null) return false

        val measurement = json.decodeFromString<GlucoseMeasurement>(rawMeasurement)

        if (lastMeasurement != null && measurement == lastMeasurement) return false

        storage?.latestMeasurement = measurement
        lastMeasurement = measurement

        return true
    }

    private fun handleThresholds(rawThresholds: String?): Boolean {
        if(rawThresholds == null) return false

        val thresholds = json.decodeFromString<GlucoseThresholds>(rawThresholds)

        if (lastThresholds != null && thresholds == lastThresholds) return false

        storage?.glucoseThresholds = thresholds
        lastThresholds = thresholds

        return true
    }

    private fun BroadcastReceiver.goAsync(
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend CoroutineScope.() -> Unit
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