# Location Status Helper

Native Kotlin Android utility for API 29+ that keeps Android location genuinely active through a user-started location foreground service. It does not fake Android's privacy indicator, upload location, hide its behavior, intercept dialer/USSD codes, or prevent Force Stop/uninstall.

## What was fixed

- A service-bound `LocationStateMonitor` observes provider changes and checks both GPS and Network provider state. When Location is disabled, requests are released but the user's preference remains enabled; when it returns, the existing foreground service refreshes requests without reopening the Activity.
- Runtime state, active-request state, user preference, and recovery status are persisted separately. The UI no longer says “may be running”.
- Recovery notifications provide a Resume action when permission, Location, OEM policy, or boot restrictions prevent automatic recovery.
- Boot recovery checks user preference, Location permission, and Location state before attempting a foreground service start. Android/OEM restrictions remain visible instead of being claimed as guaranteed.
- The service uses one listener, clears old requests before registering, requests low-power updates at a 60-second/100-meter threshold, handles permission/provider exceptions, and always includes Stop.
- Optional Device Admin activation/deactivation is implemented with no policies. It cannot prevent Force Stop, permission revocation, or uninstall.
- The launcher icon intentionally remains visible. Android 15/16 and OEM launchers do not provide a reliable, universally recoverable hidden-icon flow for this utility.
- `#1234#`-style secret dial codes are intentionally not implemented: ordinary apps cannot reliably intercept carrier USSD/MMI codes on Android 15/16.

## Permissions

The app requests only coarse/fine location, location foreground-service permissions, notifications on Android 13+, and boot completed. It deliberately does not request `ACCESS_BACKGROUND_LOCATION` because the user-started foreground service is the implemented access model. No `WAKE_LOCK` is needed.

On first use, grant Location and notification permission, enable Location in Android Settings, then press **Enable Location Service**. If notification permission is denied, Android may hide or restrict the foreground-service notification; allow notifications for reliable operation.

## Device Admin

Device Admin is optional and status-only. In the app, tap **Enable Optional Device Admin**, read the Android confirmation screen, and activate it if you have a legitimate reason to expose the status. Tap **Disable Device Admin** to deactivate it. This feature does not grant persistence or control over the device owner.

## Realme/OPPO background settings

Open the app's **Background Operation Guide** or Settings → Apps → Location Status Helper. Allow Location and notifications. In Battery usage choose Unrestricted or allow background activity if offered. In Auto-launch/Auto-start enable the app if that menu exists. Review Battery optimization as needed. Names vary by Realme UI version and these changes can increase battery use. Android and Force Stop can still stop the app.

## Platform limitations

Android 14+ requires a declared location foreground-service type and location permission. Android 15/16 can block background foreground-service starts, boot recovery, or OEM auto-start. If the service process is killed by an OEM, no ordinary app can guarantee an immediate restart; use the visible launcher, Android Settings → Apps entry, or the Resume notification. If Location is switched off while the service remains alive, requests stop and resume when the provider-change callback is delivered. The native indicator appears only while Android has active location access.

The boot receiver is not direct-boot aware: it intentionally waits for the normal unlocked `BOOT_COMPLETED` event because credential-protected preferences cannot safely be read before user unlock. This is a documented limitation, not a hidden persistence mechanism.

## Build locally

Target SDK and compile SDK are 36, Java is 17, AGP is 8.13, Kotlin is 2.2.21, and Gradle is 8.13. With Java 17, Android SDK 36, and Gradle available:

```text
gradle :app:lintDebug
gradle :app:assembleDebug
```

The APK is `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions

`.github/workflows/build-apk.yml` runs on pushes to `main` and manual dispatch, installs JDK 17, current Android command-line tools, platform-tools, Android 36, and build-tools 36.0.0, runs lint, builds the debug APK, and uploads `location-status-helper-debug-apk`. Open the repository's Actions tab, select the successful **Build APK** run, and download the artifact.

## Acceptance checklist

- Fresh install; coarse/precise permission grant and denial.
- Service enable/stop, notification Stop, app minimized, removed from Recents.
- Location OFF/ON while service is alive; verify requests stop/resume and status changes.
- Permission revoke; verify graceful inactive state and no crash.
- Notification permission denied; verify truthful UI and recovery guidance.
- Reboot/power cycle with service enabled, Location disabled, permission revoked, and battery restrictions enabled.
- Force Stop; verify Android requires manual reopening.
- Device Admin activation/deactivation.
- Launcher remains visible and app remains in Settings → Apps.
- Android 15 and Android 16, especially Realme Note 70 background management.
