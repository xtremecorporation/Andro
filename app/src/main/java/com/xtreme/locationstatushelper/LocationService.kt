package com.xtreme.locationstatushelper

import android.app.Service
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.os.Looper

class LocationService : Service() {
    private lateinit var locationManager: LocationManager
    private lateinit var stateMonitor: LocationStateMonitor
    private lateinit var preferences: ServicePreferences
    private val registeredProviders = mutableSetOf<String>()
    private var startedInForeground = false

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) { /* Local-only; intentionally not persisted. */ }
        override fun onProviderEnabled(provider: String) = refreshRequests("Provider enabled")
        override fun onProviderDisabled(provider: String) = refreshRequests("Provider disabled")
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LocationManager::class.java)
        preferences = ServicePreferences(this)
        NotificationHelper.createChannel(this)
        try {
            startForeground(NotificationHelper.NOTIFICATION_ID, NotificationHelper.build(this, "Starting location service", false))
            startedInForeground = true
            preferences.runtimeRunning = true
        } catch (error: Exception) {
            preferences.runtimeRunning = false
            preferences.recoveryStatus = "Android blocked foreground service startup: ${error.javaClass.simpleName}"
            RecoveryHelper.postResumeNotification(this, "Android blocked foreground service startup.")
            stopSelf()
            return
        }
        stateMonitor = LocationStateMonitor(this) { refreshRequests("System Location state changed") }
        stateMonitor.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!startedInForeground) return START_NOT_STICKY
        if (intent?.action == NotificationHelper.ACTION_STOP) {
            preferences.userEnabled = false
            preferences.recoveryStatus = "Stopped by user"
            stopSelf()
            return START_NOT_STICKY
        }
        if (!preferences.userEnabled) {
            stopSelf()
            return START_NOT_STICKY
        }
        refreshRequests("Service started")
        return START_STICKY
    }

    private fun refreshRequests(trigger: String) {
        if (!::preferences.isInitialized || !preferences.userEnabled) return
        stopRequests()
        if (!PermissionManager.hasLocation(this)) {
            setWaitingState("Location permission is required")
            preferences.recoveryStatus = "Location permission is not granted"
            return
        }
        if (!isLocationEnabled()) {
            setWaitingState("Location is off; waiting for it to be enabled")
            preferences.recoveryStatus = "Waiting for Location to be enabled"
            return
        }
        val providers = buildList {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) add(LocationManager.NETWORK_PROVIDER)
            if (PermissionManager.hasFineLocation(this@LocationService) && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) add(LocationManager.GPS_PROVIDER)
        }
        if (providers.isEmpty()) {
            setWaitingState("No Location provider is available")
            preferences.recoveryStatus = "No GPS or Network provider available"
            return
        }
        try {
            for (provider in providers) {
                locationManager.requestLocationUpdates(provider, UPDATE_INTERVAL_MS, MIN_DISTANCE_METERS, listener, Looper.getMainLooper())
                registeredProviders += provider
            }
            preferences.locationActive = registeredProviders.isNotEmpty()
            preferences.recoveryStatus = "Active; last checked after $trigger"
            updateNotification("Location access is active", true)
        } catch (_: SecurityException) {
            stopRequests()
            setWaitingState("Location permission was revoked")
            preferences.recoveryStatus = "Permission was revoked"
        } catch (_: IllegalArgumentException) {
            stopRequests()
            setWaitingState("Location provider is unavailable")
            preferences.recoveryStatus = "Provider unavailable"
        }
    }

    private fun setWaitingState(message: String) {
        preferences.locationActive = false
        updateNotification(message, false)
    }

    private fun stopRequests() {
        if (!::locationManager.isInitialized) return
        runCatching { locationManager.removeUpdates(listener) }
        registeredProviders.clear()
        if (::preferences.isInitialized) preferences.locationActive = false
    }

    private fun isLocationEnabled(): Boolean = runCatching { locationManager.isLocationEnabled }.getOrDefault(false)

    private fun updateNotification(message: String, active: Boolean) {
        getSystemService(android.app.NotificationManager::class.java).notify(NotificationHelper.NOTIFICATION_ID, NotificationHelper.build(this, message, active))
    }

    override fun onDestroy() {
        stopRequests()
        if (::stateMonitor.isInitialized) stateMonitor.stop()
        if (::preferences.isInitialized) preferences.runtimeRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val UPDATE_INTERVAL_MS = 60_000L
        private const val MIN_DISTANCE_METERS = 100f
    }
}
