package com.xtreme.locationstatushelper

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val preferences by lazy { ServicePreferences(this) }
    private val devicePolicyManager by lazy { getSystemService(DevicePolicyManager::class.java) }
    private val adminComponent by lazy { ComponentName(this, AdminReceiver::class.java) }
    private lateinit var status: TextView
    private lateinit var enabledSwitch: Switch
    private lateinit var launcherSwitch: Switch
    private var updatingUi = false

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (PermissionManager.hasLocation(this)) startServiceNow()
        updateUi()
    }
    private val adminLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { updateUi() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.statusText)
        enabledSwitch = findViewById(R.id.enabledSwitch)
        launcherSwitch = findViewById(R.id.launcherSwitch)
        enabledSwitch.setOnCheckedChangeListener { _, checked ->
            if (updatingUi) return@setOnCheckedChangeListener
            if (checked) startServiceFlow() else stopServiceByUser()
        }
        launcherSwitch.isChecked = true
        launcherSwitch.setOnCheckedChangeListener { _, checked ->
            if (!checked) {
                AlertDialog.Builder(this)
                    .setTitle("Keep launcher icon visible")
                    .setMessage("Hiding the launcher icon is not implemented because Android 15/16 and Realme launchers do not guarantee a reliable recovery path. The icon remains visible so the device owner can always reopen this app.")
                    .setPositiveButton("Keep visible") { _, _ -> launcherSwitch.isChecked = true }
                    .show()
            }
        }
        findViewById<Button>(R.id.startButton).setOnClickListener { startServiceFlow() }
        findViewById<Button>(R.id.stopButton).setOnClickListener { stopServiceByUser() }
        findViewById<Button>(R.id.locationSettingsButton).setOnClickListener { startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
        findViewById<Button>(R.id.permissionSettingsButton).setOnClickListener { openAppSettings() }
        findViewById<Button>(R.id.enableAdminButton).setOnClickListener { enableDeviceAdmin() }
        findViewById<Button>(R.id.disableAdminButton).setOnClickListener { disableDeviceAdmin() }
        findViewById<Button>(R.id.backgroundGuideButton).setOnClickListener { showBackgroundGuide() }
        handleIntent(intent)
        updateUi()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == NotificationHelper.ACTION_RESUME) startServiceFlow()
    }

    private fun startServiceFlow() {
        preferences.userEnabled = true
        val permissions = buildList {
            if (!PermissionManager.hasLocation(this@MainActivity)) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= 33 && !PermissionManager.hasNotifications(this@MainActivity)) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
            updateUi()
            return
        }
        startServiceNow()
    }

    private fun startServiceNow() {
        try {
            ContextCompat.startForegroundService(this, Intent(this, LocationService::class.java))
            preferences.recoveryStatus = if (isLocationEnabled()) "Start requested" else "Waiting for Location to be enabled"
        } catch (_: Exception) {
            preferences.recoveryStatus = "Android blocked foreground service startup"
            RecoveryHelper.postResumeNotification(this, "Android blocked service startup. Open the app to resume.")
        }
        updateUi()
    }

    private fun stopServiceByUser() {
        preferences.userEnabled = false
        stopService(Intent(this, LocationService::class.java))
        preferences.runtimeRunning = false
        preferences.locationActive = false
        preferences.recoveryStatus = "Stopped by user"
        updateUi()
    }

    private fun updateUi() {
        if (!::status.isInitialized) return
        val running = preferences.runtimeRunning
        val admin = devicePolicyManager.isAdminActive(adminComponent)
        updatingUi = true
        enabledSwitch.isChecked = preferences.userEnabled
        updatingUi = false
        status.text = buildString {
            appendLine("Location: ${if (isLocationEnabled()) "ON" else "OFF"}")
            appendLine("Location permission: ${if (PermissionManager.hasLocation(this@MainActivity)) "Granted" else "Denied"}")
            appendLine("Service preference: ${if (preferences.userEnabled) "Enabled" else "Disabled"}")
            appendLine("Service runtime state: ${if (running) "Running" else "Stopped"}")
            appendLine("Location requests: ${if (preferences.locationActive) "Active" else "Inactive"}")
            appendLine("Notification permission: ${if (PermissionManager.hasNotifications(this@MainActivity)) "Granted" else "Denied"}")
            appendLine("Device Admin: ${if (admin) "Enabled" else "Disabled"}")
            append("Recovery status: ${preferences.recoveryStatus}")
        }
    }

    private fun isLocationEnabled(): Boolean = runCatching { getSystemService(LocationManager::class.java).isLocationEnabled }.getOrDefault(false)
    private fun openAppSettings() { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) }

    private fun enableDeviceAdmin() {
        AlertDialog.Builder(this)
            .setTitle("Optional Device Admin")
            .setMessage("This optional Android feature only exposes activation state for this app. It cannot prevent Force Stop, permission revocation, or uninstall, and this app requests no device policies.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Continue") { _, _ ->
                adminLauncher.launch(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Optional status-only administration. No restrictions are requested.")
                })
            }.show()
    }

    private fun disableDeviceAdmin() {
        if (devicePolicyManager.isAdminActive(adminComponent)) devicePolicyManager.removeActiveAdmin(adminComponent)
        updateUi()
    }

    private fun showBackgroundGuide() {
        AlertDialog.Builder(this)
            .setTitle("Background operation guide")
            .setMessage("For Realme/OPPO devices, open Settings → Apps → Location Status Helper and allow notifications and Location. In Battery usage, choose Unrestricted or allow background activity if offered. In Auto-launch/Auto-start, enable this app if the option exists. You can also review it under Settings → Battery → Battery optimization. Menu names vary by Realme UI version; these settings may increase battery use. Android and Force Stop can still stop the app.")
            .setPositiveButton("Open app settings") { _, _ -> openAppSettings() }
            .setNegativeButton("Close", null)
            .show()
    }

    override fun onResume() { super.onResume(); updateUi() }
}
