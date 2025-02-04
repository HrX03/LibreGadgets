package dev.hrx.libregadgets.core.data.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import dev.hrx.libregadgets.core.data.message.Measurement.GlucoseMeasurement
import java.io.InputStream
import java.io.OutputStream

object GlucoseMeasurementSerializer : Serializer<GlucoseMeasurement> {
    override val defaultValue: GlucoseMeasurement
        get() = GlucoseMeasurement.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): GlucoseMeasurement =
        try {
            GlucoseMeasurement.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }

    override suspend fun writeTo(t: GlucoseMeasurement, output: OutputStream) {
        t.writeTo(output)
    }
}