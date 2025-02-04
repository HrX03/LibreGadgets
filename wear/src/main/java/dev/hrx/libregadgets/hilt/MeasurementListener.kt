package dev.hrx.libregadgets.hilt

import android.content.Context
import dev.hrx.libregadgets.core.interfaces.MeasurementListener
import dev.hrx.libregadgets.gadgets.updateWearComplication
import javax.inject.Inject

class WearMeasurementListener @Inject constructor() : MeasurementListener {
    override suspend fun onMeasurementReceived(context: Context) {
        updateWearComplication(context)
    }
}