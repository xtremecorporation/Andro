package com.xtreme.locationstatushelper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences(LocationService.PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(LocationService.KEY_ENABLED, false) || !PermissionManager.hasLocation(context)) return
        // A boot receiver may be restricted by OEM policy or newer Android rules; failure is non-fatal.
        try { ContextCompat.startForegroundService(context, Intent(context, LocationService::class.java)) } catch (_: Exception) { }
    }
}
