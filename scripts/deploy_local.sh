#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "=== 1. Building Debug APK ==="
"$SCRIPT_DIR/build.sh" :app:assembleDebug

echo "=== 2. Copying APK to local dist ==="
mkdir -p "$ROOT_DIR/dist"
cp "$ROOT_DIR/app/build/outputs/apk/debug/app-arm64-v8a-debug.apk" "$ROOT_DIR/dist/ADhils-Fitness-arm64.apk"
cp "$ROOT_DIR/app/build/outputs/apk/debug/app-x86_64-debug.apk" "$ROOT_DIR/dist/ADhils-Fitness-x86_64.apk"
cp "$ROOT_DIR/dist/ADhils-Fitness-arm64.apk" "$ROOT_DIR/dist/app.apk"

ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
if [[ -x "$ADB" ]]; then
    DEVICES=$("$ADB" devices 2>/dev/null | grep "device$" | awk '{print $1}' || true)
    if [[ -n "$DEVICES" ]]; then
        for DEV in $DEVICES; do
            echo "=== 3. Deploying to connected device: $DEV ==="
            "$ADB" -s "$DEV" install -r "$ROOT_DIR/dist/ADhils-Fitness-arm64.apk"
            "$ADB" -s "$DEV" shell am start -n com.adhils.fitness/.MainActivity
            echo "Installed and launched on $DEV!"
        done
    else
        echo "No direct ADB device connected. APK is ready in dist/."
    fi
fi

echo "=== 4. Local Wi-Fi Tablet Download Link ==="
echo "http://192.168.1.156:8888/"
echo "Direct APK: http://192.168.1.156:8888/ADhils-Fitness-arm64.apk"
