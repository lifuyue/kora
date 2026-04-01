#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_ID="${APP_ID:-com.lifuyue.kora}"
APK_PATH="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"
PAYLOAD_SOURCE="${PAYLOAD_SOURCE:-core/database/src/test/resources/local_knowledge_benchmark.json}"
PAYLOAD_DEVICE_PATH="${PAYLOAD_DEVICE_PATH:-files/debug/seed/local_knowledge_benchmark.json}"
STATUS_DEVICE_PATH="${STATUS_DEVICE_PATH:-files/debug/seed/seed-status.txt}"
RECEIVER_COMPONENT="${RECEIVER_COMPONENT:-${APP_ID}/.debug.DebugKnowledgeSeedReceiver}"
MAIN_ACTIVITY_COMPONENT="${MAIN_ACTIVITY_COMPONENT:-${APP_ID}/.MainActivity}"
SEED_ACTION="${SEED_ACTION:-com.lifuyue.kora.debug.SEED_LOCAL_KNOWLEDGE}"
BATCH_SIZE="${BATCH_SIZE:-20}"
SEED_TIMEOUT_SECONDS="${SEED_TIMEOUT_SECONDS:-180}"
REPLACE_EXISTING="${REPLACE_EXISTING:-true}"

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

ADB_BIN="$(find_android_tool adb)"

if [[ -z "${ADB_BIN}" ]]; then
  echo "[seed] adb 不可用，请先安装 Android platform-tools。" >&2
  exit 1
fi

cd "${ROOT_DIR}"

echo "[seed] 生成知识库 fixture"
python3 ./scripts/generate_local_knowledge_benchmark.py

if [[ ! -f "${ROOT_DIR}/${PAYLOAD_SOURCE}" ]]; then
  echo "[seed] 未找到 payload 源文件: ${PAYLOAD_SOURCE}" >&2
  exit 1
fi

echo "[seed] 构建 debug APK"
./scripts/gradle-safe.sh assembleDebug

if [[ ! -f "${ROOT_DIR}/${APK_PATH}" ]]; then
  echo "[seed] 未找到 APK: ${APK_PATH}" >&2
  exit 1
fi

SERIAL="${SERIAL:-}"
if [[ -z "${SERIAL}" ]]; then
  connected_devices=()
  while IFS= read -r device; do
    connected_devices+=("${device}")
  done < <("${ADB_BIN}" devices | awk 'NR>1 && $2=="device" { print $1 }')
  if [[ "${#connected_devices[@]}" -ne 1 ]]; then
    echo "[seed] 请通过 SERIAL 指定唯一设备，当前可用设备数: ${#connected_devices[@]}" >&2
    "${ADB_BIN}" devices >&2
    exit 1
  fi
  SERIAL="${connected_devices[0]}"
fi

echo "[seed] 目标设备: ${SERIAL}"

echo "[seed] 安装/更新 debug APK"
"${ADB_BIN}" -s "${SERIAL}" install -r "${APK_PATH}" >/dev/null

if ! "${ADB_BIN}" -s "${SERIAL}" shell "run-as '${APP_ID}' id" >/dev/null 2>&1; then
  echo "[seed] 当前安装包不可 run-as，必须使用 debuggable 的 Kora" >&2
  exit 1
fi

REMOTE_TMP="/data/local/tmp/kora-local-knowledge-benchmark.json"
SEED_TOKEN="$(python3 - <<'PY'
import secrets
print(secrets.token_hex(16))
PY
)"
echo "[seed] 推送 fixture 到设备"
"${ADB_BIN}" -s "${SERIAL}" push "${PAYLOAD_SOURCE}" "${REMOTE_TMP}" >/dev/null

echo "[seed] 复制到应用私有目录"
"${ADB_BIN}" -s "${SERIAL}" shell "run-as '${APP_ID}' sh -c 'mkdir -p files/debug/seed && cp ${REMOTE_TMP} ${PAYLOAD_DEVICE_PATH}'"
"${ADB_BIN}" -s "${SERIAL}" shell "run-as '${APP_ID}' sh -c 'printf %s ${SEED_TOKEN} > files/debug/seed/seed-token.txt'"

echo "[seed] 清理旧状态"
"${ADB_BIN}" -s "${SERIAL}" shell "run-as '${APP_ID}' sh -c 'rm -f ${STATUS_DEVICE_PATH}'"

echo "[seed] 拉起应用到前台"
"${ADB_BIN}" -s "${SERIAL}" shell am start -W -n "${MAIN_ACTIVITY_COMPONENT}" >/dev/null

echo "[seed] 触发 debug-only 导入"
"${ADB_BIN}" -s "${SERIAL}" shell am broadcast \
  -a "${SEED_ACTION}" \
  -n "${RECEIVER_COMPONENT}" \
  --es payload_path "${PAYLOAD_DEVICE_PATH}" \
  --es seed_token "${SEED_TOKEN}" \
  --ez replace_existing "${REPLACE_EXISTING}" \
  --ei batch_size "${BATCH_SIZE}" >/dev/null

echo "[seed] 等待导入完成"
deadline=$((SECONDS + SEED_TIMEOUT_SECONDS))
while (( SECONDS < deadline )); do
  status="$("${ADB_BIN}" -s "${SERIAL}" shell "run-as '${APP_ID}' sh -c 'cat ${STATUS_DEVICE_PATH}'" 2>/dev/null | tr -d '\r' || true)"
  if [[ -n "${status}" ]]; then
    state="$(printf '%s\n' "${status}" | awk -F= '$1=="state" { print $2 }' | tail -n 1)"
    imported="$(printf '%s\n' "${status}" | awk -F= '$1=="imported" { print $2 }' | tail -n 1)"
    ready="$(printf '%s\n' "${status}" | awk -F= '$1=="ready" { print $2 }' | tail -n 1)"
    elapsed="$(printf '%s\n' "${status}" | awk -F= '$1=="elapsedMs" { print $2 }' | tail -n 1)"
    message="$(printf '%s\n' "${status}" | awk -F= '$1=="message" { sub(/^message=/, "", $0); print }' | tail -n 1)"

    case "${state}" in
      success)
        echo "[seed] 成功: imported=${imported:-0} ready=${ready:-0} elapsedMs=${elapsed:-0}"
        exit 0
        ;;
      failure)
        echo "[seed] 失败: ${message:-unknown error}" >&2
        exit 1
        ;;
    esac
  fi
  sleep 2
done

echo "[seed] 导入超时，请检查设备上的状态文件: ${STATUS_DEVICE_PATH}" >&2
exit 1
