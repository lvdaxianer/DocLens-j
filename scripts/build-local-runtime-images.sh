#!/usr/bin/env bash
set -Eeuo pipefail

IMAGE_REPOSITORY="${IMAGE_REPOSITORY:-doclens}"
IMAGE_TAG_AMD64="${IMAGE_TAG_AMD64:-amd64}"
IMAGE_TAG_ARM64="${IMAGE_TAG_ARM64:-arm64}"
BASE_IMAGE_TAG_AMD64="${BASE_IMAGE_TAG_AMD64:-base-amd64}"
BASE_IMAGE_TAG_ARM64="${BASE_IMAGE_TAG_ARM64:-base-arm64}"
PLATFORMS="${PLATFORMS:-linux/amd64 linux/arm64}"
RUNTIME_DIR="${RUNTIME_DIR:-docker/runtime}"
POSTGRES_MAJOR="${POSTGRES_MAJOR:-16}"
LOCAL_SERVER_DIST_ARCHIVE="${LOCAL_SERVER_DIST_ARCHIVE:-${RUNTIME_DIR%/}/../build/doclens-server-dist.tar.gz}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "${SCRIPT_DIR}/postgres-deb-bundle.sh"

# 根据目标平台返回本地 artifact 后缀。
platform_suffix() {
  case "$1" in
    linux/amd64) printf '%s %s x64 x64 x64\n' "${IMAGE_TAG_AMD64}" "${BASE_IMAGE_TAG_AMD64}" ;;
    linux/arm64) printf '%s %s aarch64 arm64 arm64\n' "${IMAGE_TAG_ARM64}" "${BASE_IMAGE_TAG_ARM64}" ;;
    *)
      printf 'Unsupported platform: %s\n' "$1" >&2
      exit 1
      ;;
  esac
}

# 检查指定平台的本地 runtime artifacts 是否齐备。
require_runtime_artifacts() {
  local platform="$1"
  set -- $(platform_suffix "${platform}")

  jdk_archive="${RUNTIME_DIR}/jdk-21_linux-${3}_bin.tar.gz"
  node_archive="${RUNTIME_DIR}/node-v22-linux-${4}.tar.gz"
  postgres_archive="${RUNTIME_DIR}/postgresql-${POSTGRES_MAJOR}-ubuntu22.04-${5}-debs.tar.gz"

  for artifact in "${jdk_archive}" "${node_archive}" "${postgres_archive}"; do
    if [ ! -f "${artifact}" ]; then
      printf 'Missing %s artifact: %s\n' "${platform}" "${artifact}" >&2
      exit 1
    fi
  done

  if [ ! -f "${LOCAL_SERVER_DIST_ARCHIVE}" ]; then
    printf 'Missing local server distribution artifact: %s\n' "${LOCAL_SERVER_DIST_ARCHIVE}" >&2
    printf 'Run scripts/prepare-docker-build-context.sh before building images.\n' >&2
    exit 1
  fi

  if ! validate_postgres_archive "${platform}" "${postgres_archive}"; then
    report_incomplete_postgres_archive "${postgres_archive}"
    exit 1
  fi
}

# 输出 PostgreSQL 归档不完整的修复指引。
report_incomplete_postgres_archive() {
  printf 'PostgreSQL runtime archive is incomplete: %s\n' "$1" >&2
  printf 'Run scripts/prepare-container-runtimes.sh with a full PostgreSQL Debian dependency closure.\n' >&2
}

# 构建指定平台镜像并使用平台后缀打标签。
build_platform_image() {
  local platform="$1"
  set -- $(platform_suffix "${platform}")

  require_runtime_artifacts "${platform}"
  docker build \
    --pull=false \
    --platform "${platform}" \
    -f Dockerfile.runtime-base \
    --build-arg "LOCAL_JDK_ARCHIVE=${RUNTIME_DIR}/jdk-21_linux-${3}_bin.tar.gz" \
    --build-arg "LOCAL_NODE_ARCHIVE=${RUNTIME_DIR}/node-v22-linux-${4}.tar.gz" \
    --build-arg "LOCAL_POSTGRES_DEB_ARCHIVE=${RUNTIME_DIR}/postgresql-${POSTGRES_MAJOR}-ubuntu22.04-${5}-debs.tar.gz" \
    -t "${IMAGE_REPOSITORY}:${2}" \
    .

  docker build \
    --pull=false \
    --platform "${platform}" \
    --build-arg "DOCLENS_RUNTIME_BASE_IMAGE=${IMAGE_REPOSITORY}:${2}" \
    --build-arg "LOCAL_SERVER_DIST_ARCHIVE=${LOCAL_SERVER_DIST_ARCHIVE}" \
    -t "${IMAGE_REPOSITORY}:${1}" \
    .
}

for platform in ${PLATFORMS}; do
  build_platform_image "${platform}"
done
