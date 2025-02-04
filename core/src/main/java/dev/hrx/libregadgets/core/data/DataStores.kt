package dev.hrx.libregadgets.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import dev.hrx.libregadgets.core.data.message.Measurement.GlucoseMeasurement
import dev.hrx.libregadgets.core.data.message.Thresholds.GlucoseThresholds
import dev.hrx.libregadgets.core.data.serializer.GlucoseMeasurementSerializer
import dev.hrx.libregadgets.core.data.serializer.GlucoseThresholdsSerializer

val Context.glucoseMeasurementStore: DataStore<GlucoseMeasurement> by dataStore(
    fileName = "measurement.pb",
    serializer = GlucoseMeasurementSerializer,
)

val Context.glucoseThresholdsStore: DataStore<GlucoseThresholds> by dataStore(
    fileName = "thresholds.pb",
    serializer = GlucoseThresholdsSerializer,
)