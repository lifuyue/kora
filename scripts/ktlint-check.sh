#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ ! -x "${ROOT_DIR}/gradlew" ]]; then
  echo "gradlew not found or not executable at ${ROOT_DIR}/gradlew" >&2
  exit 1
fi

exec "${ROOT_DIR}/gradlew" ktlintCheck
