package dev.hrx.libregadgets.communication

import android.content.Context
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import dev.hrx.libregadgets.core.communication.GlucoseWearSyncDataSource
import dev.hrx.libregadgets.core.data.glucoseMeasurementStore
import dev.hrx.libregadgets.core.data.glucoseThresholdsStore
import dev.hrx.libregadgets.core.data.serializer.GlucoseMeasurementSerializer
import dev.hrx.libregadgets.core.data.serializer.GlucoseThresholdsSerializer
import dev.hrx.libregadgets.gadgets.updateWearComplication
import dev.hrx.libregadgets.gadgets.updateWearTile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

class GlucoseWearSyncService : WearableListenerService() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main.immediate + job)

    override fun onDestroy() {
        super.onDestroy()

        job.cancel()
        scope.coroutineContext.cancelChildren()
    }

    override fun onDataChanged(dataBuffer: DataEventBuffer) {
        super.onDataChanged(dataBuffer)
        val context = this

        dataBuffer.forEach { event ->
            event.dataItem.also { item ->
                when (item.uri.path) {
                    GlucoseWearSyncDataSource.NEW_MEASUREMENT_PATH -> handleNewMeasurement(
                        context,
                        item.data
                    )

                    GlucoseWearSyncDataSource.NEW_THRESHOLDS_PATH -> handleNewThresholds(
                        context,
                        item.data
                    )
                }
            }
        }

    }

    private fun handleNewMeasurement(context: Context, data: ByteArray?) {
        scope.launch {
            val inputStream = ByteArrayInputStream(data)
            val measurement = GlucoseMeasurementSerializer.readFrom(inputStream)

            context.glucoseMeasurementStore.updateData { measurement }

            updateWearComplication(context)
            updateWearTile(context)
        }
    }

    private fun handleNewThresholds(context: Context, data: ByteArray?) {
        scope.launch {
            val inputStream = ByteArrayInputStream(data)
            val thresholds = GlucoseThresholdsSerializer.readFrom(inputStream)

            context.glucoseThresholdsStore.updateData { thresholds }

            updateWearComplication(context)
            updateWearTile(context)
        }
    }
}