package dev.hrx.libregadgets.hilt

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import dev.hrx.libregadgets.MainActivity
import dev.hrx.libregadgets.R
import dev.hrx.libregadgets.core.R as coreR
import dev.hrx.libregadgets.core.interfaces.NotificationHelper
import javax.inject.Inject

class WearNotificationHelper @Inject constructor() : NotificationHelper {
    override fun createNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService<NotificationManager>()

        // create the notification channel
        val channel = NotificationChannel(
            NotificationHelper.NOTIFICATION_CHANNEL_ID,
            context.getString(coreR.string.foreground_service_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager?.createNotificationChannel(channel)
    }

    override fun buildNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, NotificationHelper.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(coreR.string.foreground_service_notification_title))
            .setContentText(context.getString(coreR.string.foreground_service_notification_description))
            .setSmallIcon(R.drawable.splash_icon)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(
                Intent(
                    context, MainActivity::class.java
                ).let { notificationIntent ->
                    PendingIntent.getActivity(
                        context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
                    )
                }).build()
    }

}