#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

REQUIRED_FILES=(
  "README.md"
  "README-en.md"
  "README_EN.md"
  "docs/api.md"
  "docs/sdk.md"
  "docs/configuration.md"
  "docs/development.md"
  "docs/technical-delivery.md"
  "docs/packaging.md"
  "docs/packaging_en.md"
  "docs/integrations/open-webui-ocr-contract.md"
)

REQUIRED_CONTENT=(
  "README.md|DocLens Java"
  "README.md|docs/api.md"
  "README.md|docs/sdk.md"
  "README.md|docs/configuration.md"
  "README.md|docs/development.md"
  "README-en.md|DocLens Java"
  "README-en.md|docs/api.md"
  "README-en.md|docs/sdk.md"
  "README-en.md|docs/configuration.md"
  "README-en.md|docs/development.md"
  "README_EN.md|README-en.md"
  "docs/api.md|HTTP API Reference"
  "docs/api.md|/api/v1/batches"
  "docs/api.md|/api/v1/ocr-nodes"
  "docs/api.md|/api/v1/llm-markdown-config"
  "docs/api.md|/api/v1/integrations/open-webui/ocr"
  "docs/sdk.md|SDK Usage"
  "docs/configuration.md|Configuration"
  "docs/development.md|Local Development"
  "docs/technical-delivery.md|技术交付文档"
  "docs/technical-delivery.md|三高保障体系"
  "docs/technical-delivery.md|高并发"
  "docs/technical-delivery.md|高可用"
  "docs/technical-delivery.md|高性能"
  "docs/technical-delivery.md|启动恢复"
  "docs/technical-delivery.md|技术取舍"
  "docs/technical-delivery.md|flowchart TD"
  "docs/technical-delivery.md|100 个文档"
)

# Ensure each planned documentation file exists before content checks run.
assert_file() {
  local file_path="$1"
  if [[ ! -f "${ROOT_DIR}/${file_path}" ]]; then
    # Missing planned documentation should stop the verification immediately.
    echo "missing file: ${file_path}" >&2
    exit 1
  fi
}

# Ensure the expected heading, endpoint, or cross-link exists in a file.
assert_contains() {
  local file_path="$1"
  local expected="$2"
  if ! rg -n --fixed-strings "${expected}" "${ROOT_DIR}/${file_path}" >/dev/null; then
    # Missing required content means the documentation split is incomplete.
    echo "${file_path} missing expected content: ${expected}" >&2
    exit 1
  fi
}

for required_file in "${REQUIRED_FILES[@]}"; do
  assert_file "${required_file}"
done

for content_rule in "${REQUIRED_CONTENT[@]}"; do
  file_path="${content_rule%%|*}"
  expected="${content_rule#*|}"
  assert_contains "${file_path}" "${expected}"
done
