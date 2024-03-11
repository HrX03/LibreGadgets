package dev.hrx.libregadgets.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Process
import android.widget.Toast
import androidx.work.ListenableWorker
import dev.hrx.libregadgets.api.LibreLinkApi
import dev.hrx.libregadgets.glance.updateWidget
import dev.hrx.libregadgets.storage.GlucoseMeasurement
import dev.hrx.libregadgets.storage.MeasurementEvaluation
import dev.hrx.libregadgets.storage.MeasurementTrend
import dev.hrx.libregadgets.storage.SharedStorage

class ApiPollService : Service() {
    private var serviceLooper: Looper? = null
    private var serviceHandler: ServiceHandler? = null

    // Handler that receives messages from the thread
    private inner class ServiceHandler(looper: Looper) : Handler(looper) {

        override fun handleMessage(msg: Message) {
            ServiceRunnable(this).run()
        }
    }

    private inner class ServiceRunnable(val handler: Handler) : Runnable {
        override fun run() {
            val api = LibreLinkApi(applicationContext)
            val storage = SharedStorage(applicationContext)

            val connectionResponse = api.getConnection() ?: return ListenableWorker.Result.failure()
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
            handler.postDelayed(this, 60000)
        }

    }

    override fun onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()

            // Get the HandlerThread's Looper and use it for our Handler
            serviceLooper = looper
            serviceHandler = ServiceHandler(looper)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show()

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        serviceHandler?.obtainMessage()?.also { msg ->
            msg.arg1 = startId
            serviceHandler?.sendMessage(msg)
        }

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show()
    }
}