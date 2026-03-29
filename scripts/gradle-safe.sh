#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cleanup_gradle_java() {
  local pids=()

  if command -v jps >/dev/null 2>&1; then
    while IFS= read -r pid; do
      [[ -n "${pid}" ]] && pids+=("${pid}")
    done < <(
      jps -lv \
        | awk '
            /org\.gradle\.launcher\.daemon\.bootstrap\.GradleDaemon/ { print $1 }
            /org\.jetbrains\.kotlin\.daemon\.KotlinCompileDaemon/ { print $1 }
          '
    )
  else
    while IFS= read -r pid; do
      [[ -n "${pid}" ]] && pids+=("${pid}")
    done < <(
      pgrep -f 'org\.gradle\.launcher\.daemon\.bootstrap\.GradleDaemon|org\.jetbrains\.kotlin\.daemon\.KotlinCompileDaemon' || true
    )
  fi

  if [[ ${#pids[@]} -eq 0 ]]; then
    echo "[gradle-safe] no Gradle/Kotlin daemons to clean up"
    return 0
  fi

  echo "[gradle-safe] stopping ${#pids[@]} Gradle/Kotlin daemon process(es): ${pids[*]}"
  kill "${pids[@]}" 2>/dev/null || true
  sleep 1

  local remaining=()
  for pid in "${pids[@]}"; do
    if kill -0 "${pid}" 2>/dev/null; then
      remaining+=("${pid}")
    fi
  done

  if [[ ${#remaining[@]} -gt 0 ]]; then
    echo "[gradle-safe] force killing remaining process(es): ${remaining[*]}"
    kill -9 "${remaining[@]}" 2>/dev/null || true
  fi
}

usage() {
  cat <<'EOF'
Usage:
  scripts/gradle-safe.sh cleanup
  scripts/gradle-safe.sh <gradle arguments...>

Examples:
  scripts/gradle-safe.sh testDebugUnitTest
  scripts/gradle-safe.sh :app:testDebugUnitTest --tests 'com.example.Test'
  scripts/gradle-safe.sh cleanup

Behavior:
  - Runs ./gradlew with the provided arguments.
  - Always cleans up GradleDaemon and KotlinCompileDaemon processes on exit.
EOF
}

if [[ $# -eq 0 ]]; then
  usage
  exit 1
fi

if [[ ! -x "${ROOT_DIR}/gradlew" ]]; then
  echo "gradlew not found or not executable at ${ROOT_DIR}/gradlew" >&2
  exit 1
fi

if [[ "${1}" == "cleanup" ]]; then
  cleanup_gradle_java
  exit 0
fi

trap cleanup_gradle_java EXIT INT TERM

cd "${ROOT_DIR}"
"${ROOT_DIR}/gradlew" "$@"
