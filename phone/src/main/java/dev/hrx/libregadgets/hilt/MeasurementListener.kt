package dev.hrx.libregadgets.hilt

import android.content.Context
import dev.hrx.libregadgets.core.interfaces.MeasurementListener
import dev.hrx.libregadgets.gadgets.updateComplication
import dev.hrx.libregadgets.gadgets.updateWidget
import javax.inject.Inject

class PhoneMeasurementListener @Inject constructor() : MeasurementListener {
    override suspend fun onMeasurementReceived(context: Context) {
        updateWidget(context)
        updateComplication(context)
    }
}