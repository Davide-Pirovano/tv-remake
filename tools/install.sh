#!/usr/bin/env bash
# Installs an update without enabling accessibility or disabling the stock launcher.
set -euo pipefail
if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
  echo "Uso: $0 SERIAL_ADB [APK]" >&2
  exit 2
fi
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADB="${ADB:-adb}"
SERIAL="$1"
APK="${2:-$ROOT/app/build/outputs/apk/release/app-release.apk}"
APP=it.saba.guesthome
[ -f "$APK" ] || { echo "APK non trovato: compila prima il progetto." >&2; exit 1; }
"$ADB" -s "$SERIAL" get-state
"$ADB" -s "$SERIAL" install -r "$APK"
"$ADB" -s "$SERIAL" shell pm grant "$APP" android.permission.CHANGE_CONFIGURATION
"$ADB" -s "$SERIAL" shell cmd appops set "$APP" WRITE_SETTINGS allow
"$ADB" -s "$SERIAL" shell cmd package set-home-activity --user 0 "$APP/com.gathin.tvhome.MainActivity"
"$ADB" -s "$SERIAL" shell am start -W -n "$APP/com.gathin.tvhome.MainActivity"
echo "Aggiornamento installato. PIN e soggiorno esistenti sono conservati."
echo "Prima installazione: segui il consenso Accessibilità e l'abilitazione della home originale nel README."
