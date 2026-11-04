package com.xtreme.locationstatushelper

import android.Manifest
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val prefs by lazy { getSharedPreferences(LocationService.PREFS, MODE_PRIVATE) }
    private lateinit var status: TextView
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { updateUi() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.statusText)
        val enabled = findViewById<Switch>(R.id.enabledSwitch)
        enabled.isChecked = prefs.getBoolean(LocationService.KEY_ENABLED, false)
        enabled.setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean(LocationService.KEY_ENABLED, checked).apply(); if (!checked) stopService() else startFlow() }
        findViewById<Button>(R.id.startButton).setOnClickListener { enabled.isChecked = true; startFlow() }
        findViewById<Button>(R.id.stopButton).setOnClickListener { enabled.isChecked = false; stopService() }
        findViewById<Button>(R.id.locationSettingsButton).setOnClickListener { startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
        findViewById<Button>(R.id.permissionSettingsButton).setOnClickListener { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) }
        updateUi()
    }

    private fun startFlow() {
        if (!PermissionManager.hasLocation(this)) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)); return
        }
        try { ContextCompat.startForegroundService(this, Intent(this, LocationService::class.java)) } catch (_: Exception) { }
        updateUi()
    }
    private fun stopService() { stopService(Intent(this, LocationService::class.java)); updateUi() }
    private fun updateUi() {
        if (!::status.isInitialized) return
        val locationOn = getSystemService(LocationManager::class.java).isLocationEnabled
        val running = prefs.getBoolean(LocationService.KEY_ENABLED, false)
        status.text = "Location: ${if (locationOn) "ON" else "OFF"}\nLocation permission: ${if (PermissionManager.hasLocation(this)) "Granted" else "Not granted"}\nBackground location permission: Not requested (not needed for this foreground-only utility)\nService status: ${if (running) "Enabled / may be running" else "Stopped"}\nNotifications: ${if (PermissionManager.hasNotifications(this)) "Allowed or not required" else "Denied; the service may be stopped by Android"}"
    }

    override fun onResume() { super.onResume(); updateUi() }
}
