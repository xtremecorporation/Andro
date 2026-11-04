package com.xtreme.locationstatushelper

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val preferences = ServicePreferences(context)
        if (!preferences.userEnabled) {
            preferences.recoveryStatus = "Not started after boot: disabled by user"
            return
        }
        if (!PermissionManager.hasLocation(context)) {
            preferences.recoveryStatus = "Recovery needs Location permission"
            RecoveryHelper.postResumeNotification(context, "Location permission is required after reboot.")
            return
        }
        val locationManager = context.getSystemService(android.location.LocationManager::class.java)
        if (!runCatching { locationManager.isLocationEnabled }.getOrDefault(false)) {
            preferences.recoveryStatus = "Recovery waiting for Location"
            RecoveryHelper.postResumeNotification(context, "Turn on Location to resume the service.")
            return
        }
        try {
            ContextCompat.startForegroundService(context, Intent(context, LocationService::class.java))
            preferences.recoveryStatus = "Boot recovery requested; Android/OEM policy may still delay it"
        } catch (_: Exception) {
            preferences.recoveryStatus = "Android/OEM policy blocked boot recovery"
            RecoveryHelper.postResumeNotification(context, "Android or Realme blocked automatic recovery after reboot.")
        }
    }
}
