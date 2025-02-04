package dev.hrx.libregadgets.core.communication

import android.content.Context
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.hrx.libregadgets.core.data.serializer.GlucoseMeasurementSerializer
import dev.hrx.libregadgets.core.data.message.Measurement.GlucoseMeasurement
import dev.hrx.libregadgets.core.data.message.Thresholds.GlucoseThresholds
import dev.hrx.libregadgets.core.data.serializer.GlucoseThresholdsSerializer
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlucoseWearSyncDataSource @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        const val NEW_MEASUREMENT_PATH = "/update/measurement"
        const val NEW_THRESHOLDS_PATH = "/update/thresholds"
    }

    private val dataClient = Wearable.getDataClient(context)

    suspend fun notifyNewMeasurement(measurement: GlucoseMeasurement) {
        val stream = ByteArrayOutputStream()
        GlucoseMeasurementSerializer.writeTo(measurement, stream)
        val request = PutDataRequest.create(NEW_MEASUREMENT_PATH).run {
            this.data = stream.toByteArray()
            setUrgent()
        }
//        val request = PutDataMapRequest.create(NEW_MEASUREMENT_PATH).run {
//            dataMap.putInt(VALUE_KEY, measurement.value)
//            dataMap.putString(EVALUATION_KEY, measurement.evaluation.name)
//            dataMap.putBoolean(STARTED_KEY, true)
//            dataMap.putLong(STARTED_AT_KEY, start.toEpochMilli())
//
//            setUrgent()
//            asPutDataRequest()
//        }

        dataClient.putDataItem(request)
    }

    suspend fun notifyNewThresholds(thresholds: GlucoseThresholds) {
        val stream = ByteArrayOutputStream()
        GlucoseThresholdsSerializer.writeTo(thresholds, stream)
        val request = PutDataRequest.create(NEW_THRESHOLDS_PATH).run {
            this.data = stream.toByteArray()
            setUrgent()
        }
//        val request = PutDataMapRequest.create(NEW_MEASUREMENT_PATH).run {
//            dataMap.putInt(VALUE_KEY, measurement.value)
//            dataMap.putString(EVALUATION_KEY, measurement.evaluation.name)
//            dataMap.putBoolean(STARTED_KEY, true)
//            dataMap.putLong(STARTED_AT_KEY, start.toEpochMilli())
//
//            setUrgent()
//            asPutDataRequest()
//        }

        dataClient.putDataItem(request)
    }
}