package com.xtreme.locationstatushelper

import android.content.Context

/** Small, synchronous state store. Values are tiny flags/status strings, so commit is intentional. */
class ServicePreferences(context: Context) {
    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var userEnabled: Boolean
        get() = prefs.getBoolean(KEY_USER_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_USER_ENABLED, value).commit()

    var runtimeRunning: Boolean
        get() = prefs.getBoolean(KEY_RUNTIME_RUNNING, false)
        set(value) = prefs.edit().putBoolean(KEY_RUNTIME_RUNNING, value).commit()

    var locationActive: Boolean
        get() = prefs.getBoolean(KEY_LOCATION_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_LOCATION_ACTIVE, value).commit()

    var recoveryStatus: String
        get() = prefs.getString(KEY_RECOVERY_STATUS, "Not attempted") ?: "Not attempted"
        set(value) = prefs.edit().putString(KEY_RECOVERY_STATUS, value).commit()

    companion object {
        const val FILE = "settings"
        const val KEY_USER_ENABLED = "service_enabled"
        const val KEY_RUNTIME_RUNNING = "runtime_running"
        const val KEY_LOCATION_ACTIVE = "location_active"
        const val KEY_RECOVERY_STATUS = "recovery_status"
    }
}
