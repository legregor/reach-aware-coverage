#!/bin/bash

set -euo pipefail

load_env_file() {
  # Single source of truth: .env in the repository root (see .env.example)
  if [[ -f ".env.local" ]]; then
    echo "[WARN] .env.local is no longer read; move its values to .env and delete it" >&2
  fi

  if [[ -f ".env" ]]; then
    set -a
    # shellcheck disable=SC1091
    source ".env"
    set +a
    echo "[INFO] Loaded environment variables from .env"
  else
    echo "[ERROR] No .env file found; copy .env.example to .env and adjust the paths" >&2
    exit 1
  fi
}

main() {
  load_env_file

  # Prefer JDK 17 for builds: set JAVA_HOME if available on macOS and ensure PATH contains its bin
  if [[ -z "${JAVA_HOME:-}" ]]; then
    if command -v /usr/libexec/java_home >/dev/null 2>&1; then
      JAVA_HOME_17=$(/usr/libexec/java_home -v17 2>/dev/null || true)
      if [[ -n "$JAVA_HOME_17" ]]; then
        export JAVA_HOME="$JAVA_HOME_17"
        export PATH="$JAVA_HOME/bin:$PATH"
        echo "[INFO] Set JAVA_HOME to $JAVA_HOME (JDK 17) for build steps"
      fi
    fi
  fi
  if [[ -n "${JAVA_HOME:-}" ]]; then
    echo "[INFO] Using JAVA_HOME=$JAVA_HOME"
  fi

  if [[ -z "${MICROSERVICES_ROOT:-}" ]]; then
    echo "[ERROR] MICROSERVICES_ROOT variable is not set in .env file" >&2
    exit 1
  fi

  if [[ ! -d "$MICROSERVICES_ROOT" ]]; then
    echo "[ERROR] Microservices root directory does not exist: $MICROSERVICES_ROOT" >&2
    exit 1
  fi

  echo "[INFO] Microservices root: $MICROSERVICES_ROOT"
  echo "[INFO] Build scaffold initialized"

  echo "[STEP] Building hypertrace image"
  make hypertrace-image-build
  echo "[INFO] hypertrace image built successfully"
  
  echo "[STEP] Running build_images.sh from microservices root"
  (cd "$MICROSERVICES_ROOT" && ./build_images.sh)
  echo "[INFO] build_images.sh completed successfully"

  echo "[STEP] Building coverage application"
  make coverage-build
  echo "[INFO] coverage application built successfully"

  echo "[STEP] Installing frontend dependencies"
  cd frontend && pnpm install && cd ..
  echo "[INFO] frontend dependencies installed successfully"

  echo "[PROGRESS] Build completed successfully"
}

main
