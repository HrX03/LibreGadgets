package dev.hrx.libregadgets.gadgets

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.google.protobuf.Timestamp
import dev.hrx.libregadgets.core.R
import dev.hrx.libregadgets.core.data.glucoseMeasurementStore
import dev.hrx.libregadgets.core.data.message.Measurement.GlucoseMeasurement
import dev.hrx.libregadgets.core.data.message.Measurement.MeasurementEvaluation
import dev.hrx.libregadgets.core.data.message.Measurement.MeasurementTrend
import dev.hrx.libregadgets.core.storage.SharedStorage
import kotlinx.coroutines.flow.first

class GlucoseWearComplication : SuspendingComplicationDataSourceService() {
    private lateinit var storage: SharedStorage
    override fun onCreate() {
        super.onCreate()
        storage = SharedStorage(this)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return buildComplicationData(
            type = type,
            measurement = GlucoseMeasurement.newBuilder().apply {
                value = 115
                evaluation = MeasurementEvaluation.Normal
                trend = MeasurementTrend.Stable
                timestamp = Timestamp.getDefaultInstance()
            }.build(),
        )
    }

    override suspend fun onComplicationRequest(
        request: ComplicationRequest
    ): ComplicationData {
        return buildComplicationData(
            request.complicationType, glucoseMeasurementStore.data.first()
        )
    }

    private fun buildComplicationData(
        type: ComplicationType,
        measurement: GlucoseMeasurement?
    ): ComplicationData {
        val icon = when (measurement?.trend) {
            MeasurementTrend.FallQuick -> R.drawable.trend_fallquick
            MeasurementTrend.Fall -> R.drawable.trend_fall
            MeasurementTrend.Stable -> R.drawable.trend_stable
            MeasurementTrend.Rise -> R.drawable.trend_rise
            MeasurementTrend.RiseQuick -> R.drawable.trend_risequick
            else -> null
        }
        val ambientIcon = when (measurement?.trend) {
            MeasurementTrend.FallQuick -> R.drawable.trend_fallquick_small
            MeasurementTrend.Fall -> R.drawable.trend_fall_small
            MeasurementTrend.Stable -> R.drawable.trend_stable_small
            MeasurementTrend.Rise -> R.drawable.trend_rise_small
            MeasurementTrend.RiseQuick -> R.drawable.trend_risequick_small
            else -> null
        }

        val builder = when (type) {
            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
                value = (measurement?.value ?: 0).toFloat().coerceIn(70f..240f),
                min = 70f,
                max = 240f,
                contentDescription = ComplicationText.EMPTY,
            ).apply {
                setText(PlainComplicationText.Builder(measurement?.value?.toString() ?: "--").build())
                if (icon != null) {
                    val context = this@GlucoseWearComplication
                    val iconBuilder =
                        MonochromaticImage.Builder(Icon.createWithResource(context, icon))
                    iconBuilder.setAmbientImage(Icon.createWithResource(context, ambientIcon!!))
                    setMonochromaticImage(iconBuilder.build())
                }
            }.build()

            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(measurement?.value?.toString() ?: "--").build(),
                contentDescription = ComplicationText.EMPTY,
            ).apply {
                if (icon != null) {
                    val context = this@GlucoseWearComplication
                    val iconBuilder =
                        MonochromaticImage.Builder(Icon.createWithResource(context, icon))
                    iconBuilder.setAmbientImage(Icon.createWithResource(context, ambientIcon!!))
                    setMonochromaticImage(iconBuilder.build())
                }
            }.build()

            else -> throw Exception("Complication type not supported: $type")
        }


        return builder
    }
}

fun updateWearComplication(context: Context) {
    val request = ComplicationDataSourceUpdateRequester.create(
        context,
        ComponentName(context, GlucoseWearComplication::class.java)
    )
    request.requestUpdateAll()
}