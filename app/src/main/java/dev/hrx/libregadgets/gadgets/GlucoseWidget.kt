package dev.hrx.libregadgets.gadgets

import android.content.Context
import android.os.Build
import android.util.Log
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
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
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
import dev.hrx.libregadgets.R
import dev.hrx.libregadgets.storage.GlucoseThresholds
import dev.hrx.libregadgets.storage.MeasurementTrend
import dev.hrx.libregadgets.storage.SharedStorage
import dev.hrx.libregadgets.utils.getClickIntent
import dev.hrx.libregadgets.utils.isMeasurementStale


class GlucoseAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlucoseAppWidget()
}

val GLUCOSE_LOW = Color(0xFFC62828)
val GLUCOSE_NORMAL = Color(0xFF2E7D32)
val GLUCOSE_VERY_HIGH = Color(0xFFE65100)
val GLUCOSE_OUTSIDE_THRESHOLD = Color(0xFFFF8F00)
val GLUCOSE_UNKNOWN = Color(0xFF424242)

val WHITE = ColorProvider(Color(0xFFFFFFFF))

private class GlucoseAppWidget : GlanceAppWidget() {
    private lateinit var storage: SharedStorage

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        storage = SharedStorage(context)
        provideContent {
            GlanceTheme(colors = DynamicThemeColorProviders) {
                WidgetBody(context)
            }
        }
    }

    @Composable
    private fun WidgetBody(context: Context) {
        Log.d("GlucoseWidget", "drawing")
        val currentData = storage.latestMeasurement
        val thresholds = storage.glucoseThresholds ?: GlucoseThresholds(180, 70)
        val icon = arrowProviderForType(currentData?.trend)
        val isStale =
            currentData != null && isMeasurementStale(System.currentTimeMillis(), currentData)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(
                    start = 16.dp,
                    top = 12.dp,
                    bottom = 16.dp,
                    end = 14.dp,
                )
                .appWidgetBackground()
                .background(if(!isStale) colorForValue(currentData?.value, thresholds) else ColorProvider(GLUCOSE_UNKNOWN))
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        cornerRadius(android.R.dimen.system_app_widget_background_radius)
                    }
                }
                .clickable(actionStartActivity(getClickIntent(context)))
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = currentData?.value?.toString() ?: "---",
                    style = TextStyle(
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = WHITE,
                    ),
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                if (currentData != null && icon != null && !isStale) Image(
                    provider = ImageProvider(icon),
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
                text = if (isStale) "Stale" else (textForValue(currentData?.value, thresholds)
                    ?: "Need setup"),
            )
        }
    }

    private fun arrowProviderForType(type: MeasurementTrend?): Int? {
        return when (type) {
            MeasurementTrend.FallQuick -> R.drawable.trend_fallquick
            MeasurementTrend.Fall -> R.drawable.trend_fall
            MeasurementTrend.Normal -> R.drawable.trend_stable
            MeasurementTrend.Rise -> R.drawable.trend_rise
            MeasurementTrend.RiseQuick -> R.drawable.trend_risequick
            else -> null
        }
    }

    private fun colorForValue(value: Int?, thresholds: GlucoseThresholds): ColorProvider {
        return ColorProvider(
            when {
                value == null -> GLUCOSE_UNKNOWN
                value > 240 -> GLUCOSE_VERY_HIGH
                value > thresholds.high -> GLUCOSE_OUTSIDE_THRESHOLD
                value < 70 -> GLUCOSE_LOW
                value < thresholds.low -> GLUCOSE_OUTSIDE_THRESHOLD
                else -> GLUCOSE_NORMAL
            }
        )
    }

    private fun textForValue(value: Int?, thresholds: GlucoseThresholds): String? {
        return when {
            value == null -> null
            value > 240 -> "High"
            value > thresholds.high -> "Going high"
            value < 70 -> "Low"
            value < thresholds.low -> "Going low"
            else -> "Normal"
        }
    }
}

suspend fun updateWidget(context: Context) {
    val manager = GlanceAppWidgetManager(context)
    val widget = GlucoseAppWidget()
    val glanceIds = manager.getGlanceIds(GlucoseAppWidget::class.java)
    glanceIds.forEach { glanceId ->
        widget.update(context, glanceId)
    }
}