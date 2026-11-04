package com.xtreme.locationstatushelper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class LocationStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PROVIDER_CHANGED) {
            val prefs = context.getSharedPreferences(LocationService.PREFS, Context.MODE_PRIVATE)
            if (prefs.getBoolean(LocationService.KEY_ENABLED, false)) {
                try { context.startService(Intent(context, LocationService::class.java)) } catch (_: Exception) { }
            }
        }
    }
}
