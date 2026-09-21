# DevOptionsLock 1.2.0

Root-level Android 11+ Developer Options lock designed for rooted kiosk devices.

## Behavior

- Keeps `development_settings_enabled=0` while locked.
- Keeps USB debugging (`adb_enabled`) and wireless debugging (`adb_wifi_enabled`) disabled while locked.
- Admin enters an 8-64 character ASCII alphanumeric password (A-Z/a-z/0-9) in the controller app.
- The app opens Android's Developer Options screen directly; no launcher/taskbar/notification/dialer is required.
- While the Developer Options activity is in the foreground, the module allows the master setting to remain enabled.
- When the user leaves that screen, the module immediately relocks it.
- A 10-minute absolute timeout is a safety net if activity detection fails.
- If the controller APK is actually removed, the module intentionally fails open and leaves Developer Options enabled.

## Intended stack

- KSU / APatch / Magisk: root authorization.
- OwnDroid: Device Owner restrictions such as disallowing debugging features and app installation.
- DevOptionsLock: Developer Options gate.

The module cannot prevent an unrestricted root user from modifying or replacing itself. Its purpose is to make root unavailable to ordinary newly installed apps, so the protected administrator app is the only normal Android-side control path.

## GitHub Actions

`.github/workflows/build.yml` builds the debug APK and root module on every push/PR. Pushing a tag such as `v1.1.0` additionally creates a GitHub Release containing both files.


## Build

Use JDK 17. The project uses Android Gradle Plugin 8.9.2 and Gradle 8.11.1, with only Google Maven, Maven Central, and the Gradle Plugin Portal.

Local build: `gradle :app:assembleDebug`

GitHub Actions: upload the project, rename `GITHUB_ACTIONS_WORKFLOW.yml` to `.github/workflows/build.yml`, then open **Actions** and run **Build DevOptionsLock**. Creating a tag such as `v1.2.3` also creates a GitHub Release with the APK and root module.


## Local build

Windows: `gradlew.bat assembleDebug` (this project wrapper delegates to the Gradle installed on your PATH).

GitHub Actions: upload the project, rename `GITHUB_ACTIONS_WORKFLOW.yml` to `.github/workflows/build.yml`, then run the workflow.
