#!/usr/bin/env bash
set -euo pipefail

TASK_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]]; then
    export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
elif [[ -z "${JAVA_HOME:-}" ]] && command -v /usr/libexec/java_home >/dev/null 2>&1; then
    if /usr/libexec/java_home -v 21 >/dev/null 2>&1; then
        export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
    fi
fi

if [[ -z "${ANDROID_HOME:-}" ]]; then
    if [[ -d "$HOME/Library/Android/sdk" ]]; then
        export ANDROID_HOME="$HOME/Library/Android/sdk"
    elif [[ -d "$HOME/Android/Sdk" ]]; then
        export ANDROID_HOME="$HOME/Android/Sdk"
    fi
fi

export GRADLE_USER_HOME="$TASK_ROOT/.tools/gradle-user-home"
TASK_SOCKET_DIR="$TASK_ROOT/.tmp"
mkdir -p "$TASK_SOCKET_DIR"
export JAVA_TOOL_OPTIONS="-Djdk.net.unixdomain.tmpdir=$TASK_SOCKET_DIR"

TASK_GRADLE="$TASK_ROOT/.tools/gradle-8.11.1/bin/gradle"
if [[ ! -x "$TASK_GRADLE" ]]; then
    echo "Gradle is not installed. Review and run ./scripts/bootstrap.sh first; this build never downloads or executes an installer automatically." >&2
    exit 1
fi

if [[ $# -eq 0 ]]; then
    TASKS=(":core:test" ":app:assembleDebug" ":app:lintDebug")
else
    TASKS=("$@")
fi

cd "$TASK_ROOT"
"$TASK_GRADLE" "${TASKS[@]}" --console=plain
