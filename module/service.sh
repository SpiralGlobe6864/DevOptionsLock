#!/system/bin/sh

MODDIR=${0%/*}
STATE_DIR=/data/adb/devoptionslock
STATE_FILE=$STATE_DIR/state
APP_PKG=com.kuroneko.devoptionslock
INTERVAL=0.20
BOOT_WAIT=120
UNLOCK_GRACE=6

mkdir -p "$STATE_DIR"
chmod 700 "$STATE_DIR"

log() {
    log -t DevOptionsLock "$1"
}

get_state() {
    [ -f "$STATE_FILE" ] && cat "$STATE_FILE" 2>/dev/null || echo "locked|0"
}

lock_all() {
    echo "locked|0" > "$STATE_FILE"
    chmod 600 "$STATE_FILE"
    settings put global development_settings_enabled 0 2>/dev/null
    settings put global adb_enabled 0 2>/dev/null
    settings put global adb_wifi_enabled 0 2>/dev/null
}

unlock_dev_options() {
    settings put global development_settings_enabled 1 2>/dev/null
}

get_top_activity() {
    TOP=$(dumpsys activity activities 2>/dev/null | grep -m 1 -E 'mResumedActivity:|mFocusedActivity:|topResumedActivity=' || true)
    if [ -z "$TOP" ]; then
        TOP=$(dumpsys window windows 2>/dev/null | grep -m 1 -E 'mCurrentFocus=|mFocusedApp=' || true)
    fi
    printf '%s' "$TOP" | tr '[:upper:]' '[:lower:]'
}

is_dev_options_page() {
    TOP=$(get_top_activity)
    case "$TOP" in
        *developmentsettings*|*development_options*|*developeroptions*|*developmentsettingsdashboard*|*application_development_settings*|*devsettings*)
            return 0
            ;;
        *)
            return 1
            ;;
    esac
}

is_controller_foreground() {
    TOP=$(get_top_activity)
    case "$TOP" in
        *"$APP_PKG"*) return 0 ;;
        *) return 1 ;;
    esac
}

fail_open() {
    settings put global development_settings_enabled 1 2>/dev/null
    log "fail-open: controller app/module state unavailable"
}

waited=0
while ! pm path "$APP_PKG" >/dev/null 2>&1; do
    if [ "$waited" -ge "$BOOT_WAIT" ]; then
        fail_open
        exit 0
    fi
    sleep 1
    waited=$((waited + 1))
done

log "service started"

while true; do
    if ! pm path "$APP_PKG" >/dev/null 2>&1; then
        fail_open
        exit 0
    fi

    RAW=$(get_state)
    MODE=${RAW%%|*}
    EXP=${RAW#*|}
    NOW=$(date +%s)

    if [ "$MODE" = "unlocked" ] && [ "$EXP" -gt "$NOW" ] 2>/dev/null; then
        # Only keep the controller-app grace window immediately after a fresh unlock.
        # Once the user has actually entered/leaves Developer Options, returning to
        # the controller app is treated as leaving the protected page and relocks.
        if is_dev_options_page || { [ $((EXP - NOW)) -ge $((600 - UNLOCK_GRACE)) ] && is_controller_foreground; }; then
            unlock_dev_options
        else
            lock_all
        fi
    else
        lock_all
    fi

    sleep "$INTERVAL"
done
