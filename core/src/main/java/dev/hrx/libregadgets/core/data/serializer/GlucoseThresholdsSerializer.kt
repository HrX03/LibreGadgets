package dev.hrx.libregadgets.core.data.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import dev.hrx.libregadgets.core.data.message.Thresholds.GlucoseThresholds
import java.io.InputStream
import java.io.OutputStream

object GlucoseThresholdsSerializer : Serializer<GlucoseThresholds> {
    override val defaultValue: GlucoseThresholds
        get() = GlucoseThresholds.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): GlucoseThresholds =
        try {
            GlucoseThresholds.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }

    override suspend fun writeTo(t: GlucoseThresholds, output: OutputStream) {
        t.writeTo(output)
    }
}