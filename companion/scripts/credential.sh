#!/usr/bin/env bash
set -euo pipefail

ACTION=""
NAME=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        -Action|-a|--action) ACTION="$2"; shift 2 ;;
        -Name|-n|--name) NAME="$2"; shift 2 ;;
        Set|Read|Delete) ACTION="$1"; shift ;;
        openai|claude) NAME="$1"; shift ;;
        *) echo "Unknown argument: $1" >&2; exit 1 ;;
    esac
done

if [[ "$ACTION" != "Set" && "$ACTION" != "Read" && "$ACTION" != "Delete" ]]; then
    echo "Usage: $0 <Set|Read|Delete> <openai|claude>" >&2
    exit 1
fi
if [[ "$NAME" != "openai" && "$NAME" != "claude" ]]; then
    echo "Name must be openai or claude" >&2
    exit 1
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CRED_DIR="$ROOT_DIR/.state"
mkdir -p "$CRED_DIR"
chmod 700 "$CRED_DIR"
CRED_FILE="$CRED_DIR/$NAME.credential"
SERVICE="adhils-fitness-companion-$NAME"
ACCOUNT="${USER:-$(id -un)}"

if [[ "$ACTION" == "Set" ]]; then
    read -r -s -p "Enter $NAME API key: " SECRET_VALUE
    echo ""
    if [[ ${#SECRET_VALUE} -le 10 ]]; then
        echo "API key is too short." >&2
        exit 1
    fi
    if [[ "$(uname -s)" == "Darwin" ]] && command -v /usr/bin/security >/dev/null 2>&1; then
        /usr/bin/security add-generic-password -U -a "$ACCOUNT" -s "$SERVICE" -w "$SECRET_VALUE" >/dev/null
        printf "keychain" > "$CRED_FILE"
        chmod 600 "$CRED_FILE"
        echo "Credential saved in macOS Keychain for user $ACCOUNT. Restart the companion if it is running."
    else
        umask 077
        printf "%s" "$SECRET_VALUE" > "$CRED_FILE"
        chmod 600 "$CRED_FILE"
        echo "Credential saved with 0600 owner-only permissions. Restart the companion if it is running."
    fi
elif [[ "$ACTION" == "Read" ]]; then
    if [[ ! -f "$CRED_FILE" ]]; then
        echo "Credential not configured" >&2
        exit 1
    fi
    CONTENT="$(cat "$CRED_FILE")"
    if [[ "$CONTENT" == "keychain" ]] && command -v /usr/bin/security >/dev/null 2>&1; then
        /usr/bin/security find-generic-password -a "$ACCOUNT" -s "$SERVICE" -w | tr -d '\r\n'
    else
        printf "%s" "$CONTENT"
    fi
elif [[ "$ACTION" == "Delete" ]]; then
    if [[ "$(uname -s)" == "Darwin" ]] && command -v /usr/bin/security >/dev/null 2>&1; then
        /usr/bin/security delete-generic-password -a "$ACCOUNT" -s "$SERVICE" >/dev/null 2>&1 || true
    fi
    rm -f "$CRED_FILE"
fi
