#!/usr/bin/env bash
set -euo pipefail

MODE="${1:-normal}"
AVD_NAME="${AVD_NAME:-Pixel_8_API_35}"
APP_ID="${APP_ID:-com.lifuyue.kora}"
ACTIVITY="${ACTIVITY:-com.lifuyue.kora/.MainActivity}"
APK_PATH="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"
DEBUG_EXTRA_KEY="com.lifuyue.kora.extra.OPEN_DEBUG_SHELL"

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
  if [[ -n "${ANDROID_SDK_ROOT:-}" && -x "${ANDROID_SDK_ROOT}/emulator/${tool}" ]]; then
    printf '%s\n' "${ANDROID_SDK_ROOT}/emulator/${tool}"
    return 0
  fi
  if [[ -n "${ANDROID_HOME:-}" && -x "${ANDROID_HOME}/platform-tools/${tool}" ]]; then
    printf '%s\n' "${ANDROID_HOME}/platform-tools/${tool}"
    return 0
  fi
  if [[ -n "${ANDROID_HOME:-}" && -x "${ANDROID_HOME}/emulator/${tool}" ]]; then
    printf '%s\n' "${ANDROID_HOME}/emulator/${tool}"
    return 0
  fi
  return 1
}

ADB_BIN="$(find_android_tool adb)"
EMULATOR_BIN="$(find_android_tool emulator)"

if [[ -z "${ADB_BIN}" || -z "${EMULATOR_BIN}" ]]; then
  echo "[android-run] adb 或 emulator 不可用，请先配置 Android SDK。" >&2
  exit 1
fi

running_emulator_for_avd() {
  while IFS= read -r serial; do
    [[ -z "${serial}" ]] && continue
    local avd_name
    avd_name="$("${ADB_BIN}" -s "${serial}" emu avd name 2>/dev/null | tr -d '\r' | head -n 1)"
    if [[ "${avd_name}" == "${AVD_NAME}" ]]; then
      printf '%s\n' "${serial}"
      return 0
    fi
  done < <("${ADB_BIN}" devices | awk 'NR>1 && $2=="device" && $1 ~ /^emulator-/ { print $1 }')
  return 1
}

wait_for_boot() {
  local serial="$1"
  "${ADB_BIN}" -s "${serial}" wait-for-device >/dev/null
  until [[ "$("${ADB_BIN}" -s "${serial}" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; do
    sleep 2
  done
}

ensure_emulator() {
  local serial
  if serial="$(running_emulator_for_avd)"; then
    echo "[android-run] 复用已启动模拟器: ${serial} (${AVD_NAME})" >&2
    wait_for_boot "${serial}"
    printf '%s\n' "${serial}"
    return 0
  fi

  echo "[android-run] 启动模拟器: ${AVD_NAME}" >&2
  nohup "${EMULATOR_BIN}" -avd "${AVD_NAME}" > /tmp/kora-emulator.log 2>&1 &

  local attempts=0
  until serial="$(running_emulator_for_avd)"; do
    attempts=$((attempts + 1))
    if (( attempts > 90 )); then
      echo "[android-run] 等待模拟器出现超时，请检查 /tmp/kora-emulator.log" >&2
      exit 1
    fi
    sleep 2
  done

  wait_for_boot "${serial}"
  printf '%s\n' "${serial}"
}

launch_app() {
  local serial="$1"
  "${ADB_BIN}" -s "${serial}" shell am force-stop "${APP_ID}" >/dev/null 2>&1 || true

  case "${MODE}" in
    normal)
      "${ADB_BIN}" -s "${serial}" shell am start -n "${ACTIVITY}" >/dev/null
      ;;
    debug-ui)
      "${ADB_BIN}" -s "${serial}" shell am start -n "${ACTIVITY}" --ez "${DEBUG_EXTRA_KEY}" true >/dev/null
      ;;
    *)
      echo "[android-run] 不支持的模式: ${MODE}，可选 normal/debug-ui" >&2
      exit 1
      ;;
  esac
}

SERIAL="$(ensure_emulator)"

echo "[android-run] 构建 debug 包"
./scripts/gradle-safe.sh assembleDebug

if [[ ! -f "${APK_PATH}" ]]; then
  echo "[android-run] 未找到 APK: ${APK_PATH}" >&2
  exit 1
fi

echo "[android-run] 安装 APK 到 ${SERIAL}"
"${ADB_BIN}" -s "${SERIAL}" install -r "${APK_PATH}" >/dev/null

echo "[android-run] 启动应用 (${MODE})"
launch_app "${SERIAL}"

echo "[android-run] 已完成: serial=${SERIAL} mode=${MODE}"
