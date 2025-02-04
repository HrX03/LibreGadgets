package dev.hrx.libregadgets.gadgets

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon as AndroidIcon
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate
import dev.hrx.libregadgets.core.R as coreR
import dev.hrx.libregadgets.R
import dev.hrx.libregadgets.core.storage.MeasurementTrend
import dev.hrx.libregadgets.core.storage.SharedStorage
import dev.hrx.libregadgets.core.utils.formatGlucoseValue
import dev.hrx.libregadgets.core.utils.getClickIntent

class GlucoseComplication : SmartspacerComplicationProvider() {
    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            label = "Live glucose Complication",
            description = "A smartspacer complication to display your current blood glucose with a trend, polled from LibreLinkUp",
            icon = AndroidIcon.createWithResource(
                provideContext(),
                R.drawable.ic_launcher_foreground
            ),
            refreshPeriodMinutes = 1
        )
    }

    override fun getSmartspaceActions(smartspacerId: String): List<SmartspaceAction> {
        val storage = SharedStorage(provideContext())
        val measurement = storage.latestMeasurement
        val icon = if (measurement != null) arrowProviderForType(measurement.trend) else null

        return listOf(
            ComplicationTemplate.Basic(
                id = "glucose_complication",
                content = Text(formatGlucoseValue(measurement?.value)),
                icon = if (icon != null) Icon(
                    AndroidIcon.createWithResource(
                        provideContext(),
                        icon
                    )
                ) else null,
                onClick = TapAction(
                    intent = Intent().setComponent(
                        getClickIntent(provideContext())
                    )
                )
            ).create()
        )
    }

    private fun arrowProviderForType(type: MeasurementTrend?): Int? {
        return when (type) {
            MeasurementTrend.FallQuick -> coreR.drawable.trend_fallquick_small
            MeasurementTrend.Fall -> coreR.drawable.trend_fall_small
            MeasurementTrend.Normal -> coreR.drawable.trend_stable_small
            MeasurementTrend.Rise -> coreR.drawable.trend_rise_small
            MeasurementTrend.RiseQuick -> coreR.drawable.trend_risequick_small
            else -> null
        }
    }
}

fun updateComplication(context: Context) {
    SmartspacerComplicationProvider.notifyChange(context, GlucoseComplication::class.java)
}