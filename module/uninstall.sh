#!/system/bin/sh
# Removing the module is intentionally a fail-open operation for this project.
settings put global development_settings_enabled 1 2>/dev/null
rm -rf /data/adb/devoptionslock 2>/dev/null
log -t DevOptionsLock "uninstalled: developer options unlocked"
