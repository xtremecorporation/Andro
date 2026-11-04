package com.xtreme.locationstatushelper

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object RecoveryHelper {
    const val RECOVERY_NOTIFICATION_ID = 1002

    fun postResumeNotification(context: Context, reason: String) {
        NotificationHelper.createChannel(context)
        val intent = Intent(context, MainActivity::class.java).setAction(NotificationHelper.ACTION_RESUME)
        val open = PendingIntent.getActivity(
            context, 20, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(reason)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Open the app and tap Enable Location Service to resume. $reason"))
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_mylocation, "Resume", open)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(RECOVERY_NOTIFICATION_ID, notification) }
    }
}
