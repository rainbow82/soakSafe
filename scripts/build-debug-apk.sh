#!/usr/bin/env bash
# Build a sideloadable debug APK. Gradle must run on JDK 17 or 21 (not JDK 25+).
set -euo pipefail
cd "$(dirname "$0")/.."
for v in 21 17; do
  if /usr/libexec/java_home -v "$v" &>/dev/null; then
    export JAVA_HOME
    JAVA_HOME="$(/usr/libexec/java_home -v "$v")"
    echo "Using JAVA_HOME=$JAVA_HOME ($(java -version 2>&1 | head -1))"
    ./gradlew :app:assembleDebug "$@"
    echo ""
    echo "APK: $(pwd)/app/build/outputs/apk/debug/app-debug.apk"
    exit 0
  fi
done
echo "No JDK 21 or 17 found. Install one, e.g.: brew install --cask temurin@21" >&2
echo "Then re-run: ./scripts/build-debug-apk.sh" >&2
exit 1
