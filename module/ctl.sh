#!/system/bin/sh

STATE_DIR=/data/adb/devoptionslock
STATE_FILE=$STATE_DIR/state

mkdir -p "$STATE_DIR"
chmod 700 "$STATE_DIR"

auto_lock() {
    echo "locked|0" > "$STATE_FILE"
    chmod 600 "$STATE_FILE"
    settings put global development_settings_enabled 0 2>/dev/null
    settings put global adb_enabled 0 2>/dev/null
    settings put global adb_wifi_enabled 0 2>/dev/null
}

case "$1" in
  init)
    auto_lock
    ;;
  lock)
    auto_lock
    ;;
  unlock)
    SEC=${2:-600}
    case "$SEC" in
      ''|*[!0-9]*) SEC=600 ;;
    esac
    NOW=$(date +%s)
    EXP=$((NOW + SEC))
    echo "unlocked|$EXP" > "$STATE_FILE"
    chmod 600 "$STATE_FILE"
    settings put global development_settings_enabled 1 2>/dev/null
    ;;
  status)
    cat "$STATE_FILE" 2>/dev/null || echo "locked|0"
    ;;
  *)
    echo "Usage: ctl.sh {init|lock|unlock [seconds]|status}"
    exit 1
    ;;
esac
