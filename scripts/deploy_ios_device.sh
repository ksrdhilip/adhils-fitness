#!/usr/bin/env bash
set -euo pipefail

TASK_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEVICE_UDID="00008150-0001186E3447801C"

echo "=========================================================="
echo " Adhil's Fitness - Physical iOS Device Deployment Pipeline"
echo "=========================================================="

echo "Step 1: Packaging multi-architecture shared.xcframework..."
rm -rf "$TASK_ROOT/shared/build/XCFrameworks/debug/shared.xcframework"
xcodebuild -create-xcframework \
    -framework "$TASK_ROOT/shared/build/bin/iosArm64/debugFramework/shared.framework" \
    -framework "$TASK_ROOT/shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework" \
    -output "$TASK_ROOT/shared/build/XCFrameworks/debug/shared.xcframework"

echo "Step 2: Building and signing native iOS app with personal profile..."
xcodebuild -project "$TASK_ROOT/iosApp/iosApp.xcodeproj" \
    -scheme iosApp \
    -destination "id=$DEVICE_UDID" \
    -configuration Debug \
    -derivedDataPath "$TASK_ROOT/build/ios" \
    -allowProvisioningUpdates \
    -allowProvisioningDeviceRegistration \
    build

echo "Step 3: Installing app onto iPhone 17 Pro Max..."
xcrun devicectl device install app --device "$DEVICE_UDID" \
    "$TASK_ROOT/build/ios/Build/Products/Debug-iphoneos/iosApp.app"

echo "Step 4: Launching Adhil's Fitness on iPhone 17 Pro Max..."
xcrun devicectl device process launch --device "$DEVICE_UDID" com.dhilip.adhilsfitness || true

echo "=========================================================="
echo " Successfully installed and launched Adhil's Fitness!"
echo "=========================================================="
