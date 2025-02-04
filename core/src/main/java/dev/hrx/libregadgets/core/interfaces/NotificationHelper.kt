package dev.hrx.libregadgets.core.interfaces

import android.app.Notification
import android.content.Context

interface NotificationHelper {
    fun createNotificationChannel(context: Context)

    fun buildNotification(context: Context): Notification

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "general_notification_channel"
    }
}