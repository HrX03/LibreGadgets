package dev.hrx.libregadgets.glance

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.DynamicThemeColorProviders
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.hrx.libregadgets.R
import dev.hrx.libregadgets.api.LibreLinkApi
import dev.hrx.libregadgets.storage.GlucoseMeasurement
import dev.hrx.libregadgets.storage.MeasurementEvaluation
import dev.hrx.libregadgets.storage.MeasurementTrend
import dev.hrx.libregadgets.storage.SharedStorage
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class GlucoseAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlucoseAppWidget()
}

val GLUCOSE_LOW = Color(0xFFC62828)
val GLUCOSE_NORMAL = Color(0xFF2E7D32)
val GLUCOSE_HIGH = Color(0xFFFF8F00)
val GLUCOSE_VERY_HIGH = Color(0xFFE65100)
val GLUCOSE_UNKNOWN = Color(0xFF424242)

val WHITE = ColorProvider(Color(0xFFFFFFFF))

private class GlucoseAppWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme(colors = DynamicThemeColorProviders) {
                WidgetBody(context)
            }
        }
    }

    @Composable
    private fun WidgetBody(context: Context) {
        val storage = SharedStorage(context)
        val data = storage.latestMeasurement
        val icon = arrowProviderForType(data?.trend)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && storage.jwtToken.isNotEmpty()) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "glucoseAppWidgetWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequest.Builder(
                    GlucoseAppWidgetWorker::class.java,
                    1.minutes.toJavaDuration()
                ).setInitialDelay(1.minutes.toJavaDuration()).build()
            )
        }

        Column(
            modifier = GlanceModifier.fillMaxSize()
                .background(
                    imageProvider = ImageProvider(R.drawable.glucose_widget_background),
                    colorFilter = ColorFilter.tint(colorForValue(data?.value)),
                )
                .padding(
                    start = 16.dp,
                    top = 12.dp,
                    bottom = 16.dp,
                    end = 14.dp,
                ),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = data?.value?.toString() ?: "---",
                    style = TextStyle(
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = WHITE,
                    ),
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                if (data != null && icon != null) Image(
                    provider = icon,
                    contentDescription = "Current glucose trend",
                    colorFilter = ColorFilter.tint(WHITE),
                )
            }
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                modifier = GlanceModifier.padding(start = 2.dp),
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = WHITE,
                ),
                text = textForValue(data?.value) ?: "Need setup",
            )
        }
    }

    private fun arrowProviderForType(type: MeasurementTrend?): ImageProvider? {
        return when (type) {
            MeasurementTrend.FallQuick -> ImageProvider(R.drawable.trend_fallquick)
            MeasurementTrend.Fall -> ImageProvider(R.drawable.trend_fall)
            MeasurementTrend.Normal -> ImageProvider(R.drawable.trend_stable)
            MeasurementTrend.Rise -> ImageProvider(R.drawable.trend_rise)
            MeasurementTrend.RiseQuick -> ImageProvider(R.drawable.trend_risequick)
            else -> null
        };
    }

    private fun colorForValue(value: Int?): ColorProvider {
        return ColorProvider(
            when {
                value == null -> GLUCOSE_UNKNOWN
                value > 240 -> GLUCOSE_VERY_HIGH
                value > 200 -> GLUCOSE_HIGH
                value > 70 -> GLUCOSE_NORMAL
                else -> GLUCOSE_LOW
            }
        )
    }

    private fun textForValue(value: Int?): String? {
        return when {
            value == null -> null
            value > 240 -> "Very high"
            value > 200 -> "High"
            value > 70 -> "Normal"
            else -> "Low"
        }
    }
}

class GlucoseAppWidgetWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        GlucoseAppWidget().apply {
            val api = LibreLinkApi(applicationContext)
            val storage = SharedStorage(applicationContext)

            val connectionResponse = api.getConnection() ?: return Result.failure()
            val patient = connectionResponse.data.first()

            storage.latestMeasurement = GlucoseMeasurement(
                value = patient.glucoseMeasurement.value,
                evaluation = when {
                    patient.glucoseMeasurement.isHigh -> MeasurementEvaluation.High
                    patient.glucoseMeasurement.isLow -> MeasurementEvaluation.Low
                    else -> MeasurementEvaluation.Normal
                },
                trend = when(patient.glucoseMeasurement.trendArrow) {
                    1 -> MeasurementTrend.FallQuick
                    2 -> MeasurementTrend.Fall
                    3 -> MeasurementTrend.Normal
                    4 -> MeasurementTrend.Rise
                    5 -> MeasurementTrend.FallQuick
                    else -> MeasurementTrend.Unknown
                },
            )
            // Call update/updateAll in case a Worker for the widget is not currently running.
            updateWidget(applicationContext)
        }
        return Result.success()
    }
}

suspend fun updateWidget(context: Context) {
    val manager = GlanceAppWidgetManager(context)
    val widget = GlucoseAppWidget()
    val glanceIds = manager.getGlanceIds(widget.javaClass)
    glanceIds.forEach { glanceId ->
        widget.update(context, glanceId)
    }
}