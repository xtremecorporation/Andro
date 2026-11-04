# Location Status Helper

Native Kotlin Android utility for Android 10–16 (API 29+). It runs a user-started location foreground service using `LocationManager` and lets Android display its own location privacy indicator while location access is active.

## Build

The repository targets Java 17, Gradle 8.13, Android Gradle Plugin 8.13, Kotlin 2.2.21, and compile/target SDK 36. In Android Studio, open the repository and run `:app:assembleDebug`. On a machine with Java 17, Android SDK 36, and Gradle 8.13 installed:

```text
gradle :app:assembleDebug
```

The APK is `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions builds and uploads this same file as `location-status-helper-debug-apk`.

## Setup and permissions

Install the APK, open the app, grant approximate or precise location, turn on the service, and press Start. Android 13+ may ask for notification permission; allow it so the persistent foreground-service notification is visible. The app intentionally does not request `ACCESS_BACKGROUND_LOCATION`: a foreground service started by the user is sufficient for this utility and avoids unnecessary background access. Location must be enabled in Android Settings.

The app does not upload or persist location data. It only receives updates locally and discards them. Stop the service or revoke permission whenever desired.

## Platform limitations and recovery

The Android privacy indicator is controlled by Android and cannot be faked or forced to remain visible without active location access. Android 14+ requires a declared location foreground-service type and location permission before starting the service. Android may block or delay boot recovery, OEM auto-launch, and background starts; the app never claims boot recovery is guaranteed. Force Stop always wins and requires reopening the app. Realme devices may require Auto-launch, background activity, and unrestricted battery settings. Battery optimization exclusions can increase battery use.

If Location is turned off, requests stop and the notification explains that the service is waiting. Re-enable Location and reopen the app if the OEM blocks automatic resumption. If notification permission is denied, Android may still enforce foreground-service rules while hiding the notification, so allow notifications for reliable operation.

## Test checklist

- Fresh install; permission granted and denied; precise and approximate location.
- Location ON/OFF; service start/stop; app minimized; app removed from Recents.
- Device reboot with service previously enabled; verify recovery is best-effort and actionable.
- Battery optimization enabled and disabled; Realme Auto-launch/background management.
- Notification permission allowed and denied.
- Location permission revoked while service is active; confirm no crash and stop/recovery behavior.
- Force Stop; verify Android requires manual relaunch.
- Android 15 and Android 16 on a real device, including the Realme Note 70.
