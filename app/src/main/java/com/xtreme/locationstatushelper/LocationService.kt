package com.xtreme.locationstatushelper

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat

class LocationService : Service() {
    private lateinit var locationManager: LocationManager
    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }
    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) { /* Access is intentionally local and not persisted. */ }
        override fun onProviderEnabled(provider: String) = refreshRequests()
        override fun onProviderDisabled(provider: String) = refreshRequests()
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LocationManager::class.java)
        NotificationHelper.createChannel(this)
        startForeground(NotificationHelper.NOTIFICATION_ID, NotificationHelper.build(this, isLocationEnabled()))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == NotificationHelper.ACTION_STOP) {
            prefs.edit().putBoolean(KEY_ENABLED, false).apply()
            stopSelf()
            return START_NOT_STICKY
        }
        if (!prefs.getBoolean(KEY_ENABLED, false)) { stopSelf(); return START_NOT_STICKY }
        refreshRequests()
        return START_STICKY
    }

    private fun refreshRequests() {
        stopRequests()
        if (!PermissionManager.hasLocation(this) || !isLocationEnabled()) {
            updateNotification()
            return
        }
        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        } ?: return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        try {
            locationManager.requestLocationUpdates(provider, UPDATE_INTERVAL_MS, 100f, listener, Looper.getMainLooper())
            updateNotification()
        } catch (_: SecurityException) { updateNotification() }
    }

    private fun stopRequests() { if (::locationManager.isInitialized) locationManager.removeUpdates(listener) }
    private fun isLocationEnabled() = locationManager.isLocationEnabled
    private fun updateNotification() { getSystemService(android.app.NotificationManager::class.java).notify(NotificationHelper.NOTIFICATION_ID, NotificationHelper.build(this, isLocationEnabled())) }
    override fun onDestroy() { stopRequests(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object { const val PREFS = "settings"; const val KEY_ENABLED = "service_enabled"; private const val UPDATE_INTERVAL_MS = 60_000L }
}
