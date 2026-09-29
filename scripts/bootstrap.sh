#!/usr/bin/env bash
set -euo pipefail

SKIP_MODEL=0
if [[ "${1:-}" == "--skip-model" ]]; then
    SKIP_MODEL=1
fi

TASK_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TASK_TOOLS="$TASK_ROOT/.tools"
mkdir -p "$TASK_TOOLS"

TASK_GRADLE_ZIP="$TASK_TOOLS/gradle-8.11.1-bin.zip"
TASK_GRADLE_HOME="$TASK_TOOLS/gradle-8.11.1"
EXPECTED_SHA="f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6"

sha256_file() {
    if command -v shasum >/dev/null 2>&1; then
        shasum -a 256 "$1" | awk '{print tolower($1)}'
    else
        sha256sum "$1" | awk '{print tolower($1)}'
    fi
}

if [[ ! -d "$TASK_GRADLE_HOME" ]]; then
    if [[ ! -f "$TASK_GRADLE_ZIP" ]]; then
        curl -fsSL "https://services.gradle.org/distributions/gradle-8.11.1-bin.zip" -o "$TASK_GRADLE_ZIP"
    fi
    REMOTE_SHA="$(curl -fsSL "https://services.gradle.org/distributions/gradle-8.11.1-bin.zip.sha256" | tr -d '[:space:]' | tr '[:upper:]' '[:lower:]')"
    ACTUAL_SHA="$(sha256_file "$TASK_GRADLE_ZIP")"
    if [[ "$ACTUAL_SHA" != "$EXPECTED_SHA" || "$ACTUAL_SHA" != "$REMOTE_SHA" ]]; then
        echo "Gradle checksum mismatch: got $ACTUAL_SHA" >&2
        exit 1
    fi
    unzip -q -o "$TASK_GRADLE_ZIP" -d "$TASK_TOOLS"
fi

if [[ "$SKIP_MODEL" -eq 0 ]]; then
    TASK_ASSETS="$TASK_ROOT/app/src/main/assets"
    mkdir -p "$TASK_ASSETS"
    TASK_MODEL="$TASK_ASSETS/pose_landmarker_lite.task"
    if [[ ! -f "$TASK_MODEL" ]]; then
        curl -fsSL "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/1/pose_landmarker_lite.task" -o "$TASK_MODEL"
    fi
    echo "Pose model SHA-256: $(sha256_file "$TASK_MODEL") ($TASK_MODEL)"
fi

echo "Build tools ready. Run ./scripts/build.sh."
