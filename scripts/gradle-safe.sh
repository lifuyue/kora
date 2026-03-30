#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
QUIET_MODE=1
LOG_DIR="${TMPDIR:-/tmp}/kora-gradle-safe"
mkdir -p "${LOG_DIR}"

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
  scripts/gradle-safe.sh --verbose <gradle arguments...>
  scripts/gradle-safe.sh <gradle arguments...>

Examples:
  scripts/gradle-safe.sh testDebugUnitTest
  scripts/gradle-safe.sh --verbose assembleDebug
  scripts/gradle-safe.sh :app:testDebugUnitTest --tests 'com.example.Test'
  scripts/gradle-safe.sh cleanup

Behavior:
  - Runs ./gradlew with the provided arguments.
  - Defaults to quiet summary output.
  - Use --verbose to stream the full Gradle log.
  - Always cleans up GradleDaemon and KotlinCompileDaemon processes on exit.
EOF
}

print_failure_summary() {
  local log_file="$1"

  echo "[gradle-safe] BUILD FAILED"

  local task_lines
  task_lines="$(rg '^> Task .* FAILED$' "${log_file}" || true)"
  if [[ -n "${task_lines}" ]]; then
    echo "[gradle-safe] failed tasks:"
    printf '%s\n' "${task_lines}"
  fi

  local test_lines
  test_lines="$(rg '^[[:alnum:]_.$-]+(Test|Tests) > .* FAILED$' "${log_file}" || true)"
  if [[ -n "${test_lines}" ]]; then
    echo "[gradle-safe] failed tests:"
    printf '%s\n' "${test_lines}"
  fi

  local compile_lines
  compile_lines="$(rg '^e: file://|^e: ' "${log_file}" || true)"
  if [[ -n "${compile_lines}" ]]; then
    echo "[gradle-safe] compiler errors:"
    printf '%s\n' "${compile_lines}"
  fi

  local problem_block
  problem_block="$(
    awk '
      /^\* What went wrong:/ { printing=1; next }
      /^\* Try:/ { printing=0 }
      printing { print }
    ' "${log_file}" | sed '/^[[:space:]]*$/d'
  )"
  if [[ -n "${problem_block}" ]]; then
    echo "[gradle-safe] what went wrong:"
    printf '%s\n' "${problem_block}"
  fi

  echo "[gradle-safe] full log: ${log_file}"
}

print_success_summary() {
  local log_file="$1"
  local success_line
  success_line="$(rg '^BUILD SUCCESSFUL.*$' "${log_file}" | tail -n 1 || true)"
  if [[ -n "${success_line}" ]]; then
    echo "[gradle-safe] ${success_line}"
  else
    echo "[gradle-safe] BUILD SUCCESSFUL"
  fi
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

if [[ "${1}" == "--verbose" ]]; then
  QUIET_MODE=0
  shift
fi

if [[ $# -eq 0 ]]; then
  usage
  exit 1
fi

trap cleanup_gradle_java EXIT INT TERM

cd "${ROOT_DIR}"

if [[ "${QUIET_MODE}" -eq 0 ]]; then
  "${ROOT_DIR}/gradlew" "$@"
  exit 0
fi

log_file="${LOG_DIR}/$(date +%Y%m%d-%H%M%S)-$$.log"
set +e
"${ROOT_DIR}/gradlew" --console=plain "$@" >"${log_file}" 2>&1
gradle_exit_code=$?
set -e

if [[ ${gradle_exit_code} -eq 0 ]]; then
  print_success_summary "${log_file}"
  exit 0
fi

print_failure_summary "${log_file}"
exit "${gradle_exit_code}"
