package dev.hrx.libregadgets.communication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.hrx.libregadgets.storage.SharedStorage

class BootStartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        if(intent?.action != "android.intent.action.BOOT_COMPLETED") return

        val storage = SharedStorage(context)
        if (storage.jwtToken.isNotEmpty())
            context.startForegroundService(Intent(context, GlucosePollService::class.java))
    }
}