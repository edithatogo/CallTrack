#!/usr/bin/env bash
# Only the explicitly named disposable Android 17 AVD is permitted.
set -euo pipefail
cd "$(dirname "$0")/.."
serial="${1:?Usage: scripts/qualify_android17_local.sh emulator-5554}"
[[ "$serial" =~ ^emulator-[0-9]+$ ]] || { echo 'Refusing a physical device' >&2; exit 2; }
: "${ANDROID_HOME:?Set ANDROID_HOME to your installed Android SDK}"
: "${JAVA_HOME:?Set JAVA_HOME to the qualified JDK 25}"
adb="$ANDROID_HOME/platform-tools/adb"
test -x "$adb"
test -d "$ANDROID_HOME/platforms/android-37"
actual_avd="$("$adb" -s "$serial" emu avd name | sed -n '1p' | tr -d '\r')"
actual_sdk="$("$adb" -s "$serial" shell getprop ro.build.version.sdk | tr -d '\r')"
if [[ "$actual_avd" != 'calltrack-android17-local' || "$actual_sdk" != 37 ]]; then
  echo "Refusing AVD '$actual_avd' (API $actual_sdk); expected calltrack-android17-local API 37" >&2
  exit 2
fi
test "$("$adb" -s "$serial" shell getprop ro.kernel.qemu | tr -d '\r')" = 1
mkdir -p .local/slice-evidence
if [[ ! -e .local/android-sdk && ! -L .local/android-sdk ]]; then
  ln -s "$ANDROID_HOME" .local/android-sdk
fi
test "$(cd .local/android-sdk && pwd -P)" = "$(cd "$ANDROID_HOME" && pwd -P)"
test "$(.venv/bin/python --version)" = "Python $(cat .python-version)"
.venv/bin/python -m unittest discover -s tests -q
.venv/bin/python scripts/verify_conductor.py
.venv/bin/python scripts/run_slice_build.py --jdk "$JAVA_HOME" --sdk "$ANDROID_HOME"
for suite in settings imports recovery accessibility; do
  .venv/bin/python scripts/qualify_parity_ui.py --serial "$serial" --suite "$suite"
done
