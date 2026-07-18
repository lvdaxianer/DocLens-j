#!/usr/bin/env bash
set -euo pipefail

compose_file="${1:-docker-compose.all-in-one.yml}"
env_example="${2:-docker/examples/all-in-one/.env.example}"
config_example="${3:-docker/examples/all-in-one/application.yml}"
packaging_doc="${4:-docs/packaging.md}"
packaging_doc_en="${5:-docs/packaging_en.md}"

failures=0

# 校验示例文件是否已经落库。
require_file() {
  local file="$1"
  local message="$2"

  if [ ! -f "${file}" ]; then
    printf 'FAIL: %s\n' "${message}" >&2
    failures=$((failures + 1))
  fi
}

# 校验正则模式是否出现在目标文件中。
require_pattern() {
  local file="$1"
  local pattern="$2"
  local message="$3"

  if [ ! -f "${file}" ] || ! grep -Eq -- "${pattern}" "${file}"; then
    printf 'FAIL: %s\n' "${message}" >&2
    failures=$((failures + 1))
  fi
}

# 校验字面字符串是否出现在目标文件中，适合路径这类固定文本。
require_literal() {
  local file="$1"
  local text="$2"
  local message="$3"

  if [ ! -f "${file}" ] || ! grep -Fq -- "${text}" "${file}"; then
    printf 'FAIL: %s\n' "${message}" >&2
    failures=$((failures + 1))
  fi
}

require_file "${compose_file}" \
  'Dedicated all-in-one Docker Compose example must exist.'
require_pattern "${compose_file}" 'doclens:(amd64|arm64)|\$\{DOCLENS_IMAGE_REPOSITORY' \
  'Compose example must point at the all-in-one DocLens image.'
require_pattern "${compose_file}" 'DOCLENS_CONFIG_DIR' \
  'Compose example must inject DOCLENS_CONFIG_DIR.'
require_pattern "${compose_file}" '/opt/doclens/config' \
  'Compose example must mount the external Spring config directory.'

require_file "${env_example}" \
  'All-in-one env example must exist.'
require_pattern "${env_example}" '^DOCLENS_IMAGE_REPOSITORY=' \
  'Env example must document the image repository.'
require_pattern "${env_example}" '^DOCLENS_IMAGE_TAG=' \
  'Env example must document the image tag.'
require_pattern "${env_example}" '^DOCLENS_SERVER_PORT=' \
  'Env example must document the application port.'
require_pattern "${env_example}" '^POSTGRES_PORT=' \
  'Env example must document the PostgreSQL port.'

require_file "${config_example}" \
  'Mounted Spring application.yml example must exist.'
require_pattern "${config_example}" '^doclens:' \
  'Mounted application.yml example must include doclens runtime settings.'
require_pattern "${config_example}" 'default-routing-mode:' \
  'Mounted application.yml example must include OCR routing settings.'
require_pattern "${config_example}" 'global-protection:' \
  'Mounted application.yml example must include traffic protection settings.'

require_literal "${packaging_doc}" 'docker-compose.all-in-one.yml' \
  'Chinese packaging guide must mention the dedicated all-in-one Compose example.'
require_literal "${packaging_doc}" 'docker/examples/all-in-one/.env.example' \
  'Chinese packaging guide must mention the env example file.'
require_literal "${packaging_doc}" 'docker/examples/all-in-one/application.yml' \
  'Chinese packaging guide must mention the mounted application.yml example.'
require_literal "${packaging_doc_en}" 'docker-compose.all-in-one.yml' \
  'English packaging guide must mention the dedicated all-in-one Compose example.'
require_literal "${packaging_doc_en}" 'docker/examples/all-in-one/.env.example' \
  'English packaging guide must mention the env example file.'

if [ "${failures}" -gt 0 ]; then
  exit 1
fi

printf 'All-in-one Compose runtime example verified.\n'
