#!/bin/bash

# Script to run coverage analysis with Jaeger tracing
# Usage: ./run-coverage.sh [jar arguments...]

echo "[PROGRESS] Initializing coverage tool"

# The static analysis reads the sources only, so it needs neither traces nor Docker
STATIC_ONLY=false
for arg in "$@"; do
  if [[ "$arg" == "--static" ]]; then
    STATIC_ONLY=true
    break
  fi
done

if [[ "$STATIC_ONLY" == true ]]; then
  echo "[INFO] Static analysis only; Jaeger is not needed"
else
  echo "[STEP] Checking Docker status"
  if ! docker info >/dev/null 2>&1; then
    echo "[ERROR] Docker is not running. Please start Docker and run the analysis again." >&2
    exit 1
  fi

  echo "[STEP] Checking Jaeger status"
  if docker compose ps jaeger | grep -q "Up"; then
      echo "[INFO] Jaeger is already running"
  else
      echo "[STEP] Starting Jaeger with Docker Compose"
      if ! docker compose up -d jaeger; then
        echo "[ERROR] Starting Jaeger with Docker Compose failed; see the output above" >&2
        exit 1
      fi
      echo "[INFO] Jaeger started successfully"
  fi
fi

echo "[STEP] Building coverage application"
# In a subshell, so a failed build cannot leave the rest of the script in coverage/
if ! (cd coverage && mvn clean install -DskipTests); then
  echo "[ERROR] Building the coverage tool failed; see the Maven output above" >&2
  exit 1
fi
echo "[INFO] Coverage application built successfully"

echo "[STEP] Running coverage analysis"
echo "[INFO] Arguments: $@"
# Prefer JAVA_HOME if set, otherwise pick a JDK 17 installation on macOS, then fallback to system `java`.
JAVA_CMD=""
if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
  JAVA_CMD="$JAVA_HOME/bin/java"
else
  if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    JAVA_HOME_17=$(/usr/libexec/java_home -v17 2>/dev/null || true)
    if [[ -n "$JAVA_HOME_17" && -x "$JAVA_HOME_17/bin/java" ]]; then
      JAVA_CMD="$JAVA_HOME_17/bin/java"
    fi
  fi
  if [[ -z "$JAVA_CMD" ]]; then
    JAVA_CMD=$(command -v java || true)
  fi
fi

if [[ -z "$JAVA_CMD" ]]; then
  echo "[ERROR] java not found; please install JDK 17 and set JAVA_HOME or put java on PATH" >&2
  exit 1
fi

# Check java major version and require 17
JAVA_VERSION=$($JAVA_CMD -version 2>&1 | awk -F '"' '/version/ {print $2}')
JAVA_MAJOR=$(echo "$JAVA_VERSION" | awk -F. '{ if ($1=="1") print $2; else print $1 }')
if [[ "$JAVA_MAJOR" -ne 17 ]]; then
  echo "[ERROR] Detected Java $JAVA_VERSION (major $JAVA_MAJOR). JDK 17 is required." >&2
  echo "[HINT] Set JAVA_HOME to a JDK 17 installation, e.g. export JAVA_HOME=\"\$(/usr/libexec/java_home -v17)\"" >&2
  exit 1
fi

echo "[INFO] Using java: $JAVA_CMD (version $JAVA_VERSION)"

"$JAVA_CMD" \
  --add-opens java.base/java.nio=ALL-UNNAMED \
  --add-opens java.base/jdk.internal.misc=ALL-UNNAMED \
  -jar coverage/target/coverage-1.0.0.jar "$@"
status=$?
if [[ $status -ne 0 ]]; then
  echo "[ERROR] Coverage analysis failed (exit code $status); see the messages above" >&2
  exit $status
fi
echo "[PROGRESS] Coverage analysis completed"