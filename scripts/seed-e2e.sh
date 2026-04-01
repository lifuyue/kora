#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_ID="${APP_ID:-com.lifuyue.kora}"
PAYLOAD_DEVICE_PATH="${PAYLOAD_DEVICE_PATH:-files/debug/seed/local_knowledge_benchmark.json}"
STATUS_DEVICE_PATH="${STATUS_DEVICE_PATH:-files/debug/seed/seed-status.txt}"
RECEIVER_COMPONENT="${RECEIVER_COMPONENT:-${APP_ID}/.debug.DebugKnowledgeSeedReceiver}"
MAIN_ACTIVITY_COMPONENT="${MAIN_ACTIVITY_COMPONENT:-${APP_ID}/.MainActivity}"
SEED_ACTION="${SEED_ACTION:-com.lifuyue.kora.debug.SEED_LOCAL_KNOWLEDGE}"
SEED_E2E_DIR="${SEED_E2E_DIR:-${ROOT_DIR}/build/seed-e2e}"
SEED_E2E_RECORD_PATH="${SEED_E2E_RECORD_PATH:-${SEED_E2E_DIR}/last-run.json}"

find_android_tool() {
  local tool="$1"
  if command -v "$tool" >/dev/null 2>&1; then
    command -v "$tool"
    return 0
  fi
  if [[ -n "${ANDROID_SDK_ROOT:-}" && -x "${ANDROID_SDK_ROOT}/platform-tools/${tool}" ]]; then
    printf '%s\n' "${ANDROID_SDK_ROOT}/platform-tools/${tool}"
    return 0
  fi
  if [[ -n "${ANDROID_HOME:-}" && -x "${ANDROID_HOME}/platform-tools/${tool}" ]]; then
    printf '%s\n' "${ANDROID_HOME}/platform-tools/${tool}"
    return 0
  fi
  return 1
}

json_escape() {
  python3 -c 'import json, sys; print(json.dumps(sys.argv[1]))' "$1"
}

fail_with_reason() {
  local reason="$1"
  local detail="${2:-}"
  mkdir -p "${SEED_E2E_DIR}"
  cat > "${SEED_E2E_RECORD_PATH}" <<EOF
{
  "result": "failure",
  "reason": $(json_escape "${reason}"),
  "detail": $(json_escape "${detail}")
}
EOF
  echo "[seed-e2e] ${reason}${detail:+: ${detail}}" >&2
  exit 1
}

ADB_BIN="$(find_android_tool adb || true)"
if [[ -z "${ADB_BIN}" ]]; then
  fail_with_reason "adb unavailable" "请先安装 Android platform-tools"
fi

cd "${ROOT_DIR}"
mkdir -p "${SEED_E2E_DIR}"

SERIAL="${SERIAL:-}"
if [[ -z "${SERIAL}" ]]; then
  connected_devices=()
  while IFS= read -r device; do
    connected_devices+=("${device}")
  done < <("${ADB_BIN}" devices | awk 'NR>1 && $2=="device" { print $1 }')
  if [[ "${#connected_devices[@]}" -eq 0 ]]; then
    fail_with_reason "no device" "当前没有可用设备"
  fi
  if [[ "${#connected_devices[@]}" -ne 1 ]]; then
    fail_with_reason "multiple devices" "请通过 SERIAL 指定唯一设备"
  fi
  SERIAL="${connected_devices[0]}"
fi

android_version="$("${ADB_BIN}" -s "${SERIAL}" shell getprop ro.build.version.release 2>/dev/null | tr -d '\r' || true)"
sdk_int="$("${ADB_BIN}" -s "${SERIAL}" shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r' || true)"
package_path="$("${ADB_BIN}" -s "${SERIAL}" shell pm path "${APP_ID}" 2>/dev/null | tr -d '\r' || true)"
if [[ -z "${package_path}" ]]; then
  fail_with_reason "package missing" "${APP_ID} 未安装到设备 ${SERIAL}"
fi

run_as_ok="false"
if "${ADB_BIN}" -s "${SERIAL}" shell "run-as '${APP_ID}' id" >/dev/null 2>&1; then
  run_as_ok="true"
else
  fail_with_reason "run-as unavailable" "${APP_ID} 不是可调试安装包或 run-as 不可用"
fi

cat > "${SEED_E2E_RECORD_PATH}" <<EOF
{
  "result": "running",
  "serial": $(json_escape "${SERIAL}"),
  "appId": $(json_escape "${APP_ID}"),
  "androidVersion": $(json_escape "${android_version}"),
  "sdkInt": $(json_escape "${sdk_int}"),
  "packagePath": $(json_escape "${package_path}"),
  "runAsAvailable": ${run_as_ok},
  "payloadDevicePath": $(json_escape "${PAYLOAD_DEVICE_PATH}"),
  "statusDevicePath": $(json_escape "${STATUS_DEVICE_PATH}"),
  "mainActivityComponent": $(json_escape "${MAIN_ACTIVITY_COMPONENT}"),
  "receiverComponent": $(json_escape "${RECEIVER_COMPONENT}"),
  "seedAction": $(json_escape "${SEED_ACTION}")
}
EOF

echo "[seed-e2e] 设备: ${SERIAL} Android ${android_version} (SDK ${sdk_int})"
echo "[seed-e2e] 包路径: ${package_path}"
echo "[seed-e2e] 记录文件: ${SEED_E2E_RECORD_PATH}"

if ! SERIAL="${SERIAL}" APP_ID="${APP_ID}" PAYLOAD_DEVICE_PATH="${PAYLOAD_DEVICE_PATH}" STATUS_DEVICE_PATH="${STATUS_DEVICE_PATH}" MAIN_ACTIVITY_COMPONENT="${MAIN_ACTIVITY_COMPONENT}" RECEIVER_COMPONENT="${RECEIVER_COMPONENT}" SEED_ACTION="${SEED_ACTION}" ./scripts/seed-installed-app.sh; then
  status_content="$("${ADB_BIN}" -s "${SERIAL}" shell "run-as '${APP_ID}' sh -c 'cat ${STATUS_DEVICE_PATH}'" 2>/dev/null | tr -d '\r' || true)"
  if [[ -z "${status_content}" ]]; then
    fail_with_reason "seed execution failed" "状态文件缺失或导入脚本提前失败"
  fi
  fail_with_reason "seed execution failed" "${status_content}"
fi

status_content="$("${ADB_BIN}" -s "${SERIAL}" shell "run-as '${APP_ID}' sh -c 'cat ${STATUS_DEVICE_PATH}'" 2>/dev/null | tr -d '\r' || true)"
if [[ -z "${status_content}" ]]; then
  fail_with_reason "status missing" "导入完成后未找到状态文件"
fi

state="$(printf '%s\n' "${status_content}" | awk -F= '$1=="state" { print $2 }' | tail -n 1)"
imported="$(printf '%s\n' "${status_content}" | awk -F= '$1=="imported" { print $2 }' | tail -n 1)"
ready="$(printf '%s\n' "${status_content}" | awk -F= '$1=="ready" { print $2 }' | tail -n 1)"
elapsed="$(printf '%s\n' "${status_content}" | awk -F= '$1=="elapsedMs" { print $2 }' | tail -n 1)"
message="$(printf '%s\n' "${status_content}" | awk -F= '$1=="message" { sub(/^message=/, "", $0); print }' | tail -n 1)"

if [[ "${state}" != "success" ]]; then
  fail_with_reason "seed state failure" "${status_content}"
fi

if [[ -z "${imported}" || "${imported}" == "0" ]]; then
  fail_with_reason "empty import" "${status_content}"
fi

if [[ "${ready}" != "${imported}" ]]; then
  fail_with_reason "ready mismatch" "${status_content}"
fi

cat > "${SEED_E2E_RECORD_PATH}" <<EOF
{
  "result": "success",
  "serial": $(json_escape "${SERIAL}"),
  "appId": $(json_escape "${APP_ID}"),
  "androidVersion": $(json_escape "${android_version}"),
  "sdkInt": $(json_escape "${sdk_int}"),
  "packagePath": $(json_escape "${package_path}"),
  "runAsAvailable": true,
  "payloadDevicePath": $(json_escape "${PAYLOAD_DEVICE_PATH}"),
  "statusDevicePath": $(json_escape "${STATUS_DEVICE_PATH}"),
  "mainActivityComponent": $(json_escape "${MAIN_ACTIVITY_COMPONENT}"),
  "receiverComponent": $(json_escape "${RECEIVER_COMPONENT}"),
  "seedAction": $(json_escape "${SEED_ACTION}"),
  "status": $(json_escape "${state}"),
  "imported": ${imported},
  "ready": ${ready},
  "elapsedMs": ${elapsed:-0},
  "message": $(json_escape "${message}")
}
EOF

printf '[seed-e2e] success serial=%s appId=%s imported=%s ready=%s elapsedMs=%s\n' \
  "${SERIAL}" "${APP_ID}" "${imported}" "${ready}" "${elapsed:-0}"
printf '[seed-e2e] record=%s\n' "${SEED_E2E_RECORD_PATH}"
