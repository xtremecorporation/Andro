package com.xtreme.locationstatushelper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

object NotificationHelper {
    const val CHANNEL_ID = "location_service"
    const val NOTIFICATION_ID = 1001
    const val ACTION_STOP = "com.xtreme.locationstatushelper.STOP"
    const val ACTION_RESUME = "com.xtreme.locationstatushelper.RESUME"

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, context.getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW).apply {
            description = context.getString(R.string.channel_description)
        })
    }

    fun build(context: Context, state: String, locationActive: Boolean): Notification {
        val open = PendingIntent.getActivity(context, 1, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val resume = PendingIntent.getActivity(context, 3, Intent(context, MainActivity::class.java).setAction(ACTION_RESUME), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop = PendingIntent.getService(context, 2, Intent(context, LocationService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(state)
            .setContentIntent(open).setOngoing(true).setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stop)
            .apply {
                if (!locationActive) addAction(android.R.drawable.ic_menu_mylocation, "Resume", resume)
            }
            .build()
    }
}
