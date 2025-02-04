package dev.hrx.libregadgets.core.interfaces

import android.content.Context

interface MeasurementListener {
    suspend fun onMeasurementReceived(context: Context)
}