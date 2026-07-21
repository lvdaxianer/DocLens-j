#!/usr/bin/env bash
# 使用本地运行时归档构建 DocLens 多架构公测镜像。
#
# 构建输入约定：
# - Ubuntu 基础镜像必须已经存在于本地 Docker 中。
# - JDK21 归档按目标 CPU 架构存放在 docker/runtime。
# - Node22 归档按目标 CPU 架构存放在 docker/runtime。
# - PostgreSQL Debian 依赖闭包按目标架构提前准备。
# - Maven Assembly 发行包在宿主机完成构建。
# - Docker build 阶段不联网下载上述运行时和应用依赖。
#
# 版本约定：
# - RELEASE_VERSION 表示镜像内应用的 SemVer 版本。
# - amd64 最终标签由 RELEASE_VERSION 和 amd64 后缀组成。
# - arm64 最终标签由 RELEASE_VERSION 和 arm64 后缀组成。
# - Assembly 内部 JAR 必须使用同一个 RELEASE_VERSION。
# - 版本不匹配时在 Docker build 之前终止。
# - 架构兼容标签只在最终镜像构建成功后刷新。
#
# 每个平台的构建顺序：
# - 解析不可变应用标签、基础标签和兼容标签。
# - 解析 JDK、Node.js 和 PostgreSQL 本地归档后缀。
# - 检查三个运行时归档都存在。
# - 检查 Maven Assembly 发行包存在。
# - 检查 Assembly 内部后端 JAR 版本。
# - 检查 PostgreSQL Debian 依赖闭包完整。
# - 构建可复用的 Ubuntu runtime 基础镜像。
# - 基于 runtime 基础镜像构建最终应用镜像。
# - 把最终应用镜像刷新到架构兼容标签。
#
# 平台隔离约定：
# - linux/amd64 只能使用 x64 JDK 和 Node.js。
# - linux/amd64 只能使用 x64 PostgreSQL Debian 包。
# - linux/arm64 只能使用 aarch64 JDK。
# - linux/arm64 只能使用 arm64 Node.js 和 PostgreSQL 包。
# - 未支持的平台直接返回错误，不进行降级构建。
#
# 运维约定：
# - 默认仓库名为 doclens。
# - 默认同时构建 amd64 和 arm64。
# - PLATFORMS 可以收窄本次需要构建的平台。
# - IMAGE_TAG_* 可用于显式发布标签控制。
# - COMPATIBILITY_TAG_* 可用于调整本地兼容别名。
# - BASE_IMAGE_TAG_* 可用于复用已有 runtime 基础镜像。
# - 任一阶段失败都会停止当前脚本，避免刷新错误标签。
set -Eeuo pipefail

IMAGE_REPOSITORY="${IMAGE_REPOSITORY:-doclens}"
# 公测发布版本同时用于 Assembly 内容校验和不可变镜像标签。
RELEASE_VERSION="${RELEASE_VERSION:-0.1.0-beta.1}"
# 每个平台生成一个带发布版本的不可变应用镜像。
IMAGE_TAG_AMD64="${IMAGE_TAG_AMD64:-${RELEASE_VERSION}-amd64}"
IMAGE_TAG_ARM64="${IMAGE_TAG_ARM64:-${RELEASE_VERSION}-arm64}"
# 架构标签保留为兼容别名，不作为默认部署版本依据。
COMPATIBILITY_TAG_AMD64="${COMPATIBILITY_TAG_AMD64:-amd64}"
COMPATIBILITY_TAG_ARM64="${COMPATIBILITY_TAG_ARM64:-arm64}"
BASE_IMAGE_TAG_AMD64="${BASE_IMAGE_TAG_AMD64:-base-amd64}"
BASE_IMAGE_TAG_ARM64="${BASE_IMAGE_TAG_ARM64:-base-arm64}"
PLATFORMS="${PLATFORMS:-linux/amd64 linux/arm64}"
RUNTIME_DIR="${RUNTIME_DIR:-docker/runtime}"
POSTGRES_MAJOR="${POSTGRES_MAJOR:-16}"
LOCAL_SERVER_DIST_ARCHIVE="${LOCAL_SERVER_DIST_ARCHIVE:-${RUNTIME_DIR%/}/../build/doclens-server-dist.tar.gz}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "${SCRIPT_DIR}/postgres-deb-bundle.sh"

# 根据目标平台返回镜像标签和 artifact 后缀。
# 参数 platform：仅支持 linux/amd64 或 linux/arm64。
# 返回：应用标签、基础标签、兼容标签、JDK、Node 和 PG 后缀。
# 作者：lvdaxianer@yeah.net
# 日期：2026-07-21
platform_suffix() {
  case "$1" in
    linux/amd64) printf '%s %s %s x64 x64 x64\n' "${IMAGE_TAG_AMD64}" "${BASE_IMAGE_TAG_AMD64}" "${COMPATIBILITY_TAG_AMD64}" ;;
    linux/arm64) printf '%s %s %s aarch64 arm64 arm64\n' "${IMAGE_TAG_ARM64}" "${BASE_IMAGE_TAG_ARM64}" "${COMPATIBILITY_TAG_ARM64}" ;;
    *)
      printf 'Unsupported platform: %s\n' "$1" >&2
      exit 1
      ;;
  esac
}

# 校验 Assembly 中的后端版本与待构建镜像版本一致。
# 参数：无，读取 LOCAL_SERVER_DIST_ARCHIVE 和 RELEASE_VERSION。
# 返回：版本一致时返回，版本不一致时终止构建。
# 作者：lvdaxianer@yeah.net
# 日期：2026-07-21
validate_server_distribution_version() {
  # 保存发行包条目和预期的后端 jar 路径。
  local distribution_entries
  local expected_server_jar

  distribution_entries="$(tar -tzf "${LOCAL_SERVER_DIST_ARCHIVE}")"
  expected_server_jar="doclens-server-${RELEASE_VERSION}/lib/doclens-server-${RELEASE_VERSION}.jar"

  # 版本匹配时继续构建，否则拒绝生成错误版本标签。
  if grep -Fxq -- "${expected_server_jar}" <<<"${distribution_entries}"; then
    return
  else
    printf 'Server distribution does not match release version %s: %s\n' \
      "${RELEASE_VERSION}" "${LOCAL_SERVER_DIST_ARCHIVE}" >&2
    exit 1
  fi
}

# 检查单个本地构建输入是否存在。
# 参数 artifact：本地构建输入路径。
# 参数 platform：目标 Docker 平台。
# 返回：文件存在时返回，缺失时终止构建。
# 作者：lvdaxianer@yeah.net
# 日期：2026-07-21
require_artifact_file() {
  # 保存输入路径和目标平台，便于错误信息定位。
  local artifact="$1"
  local platform="$2"

  # 文件存在时继续，否则终止当前平台构建。
  if [ -f "${artifact}" ]; then
    return
  else
    printf 'Missing %s artifact: %s\n' "${platform}" "${artifact}" >&2
    exit 1
  fi
}

# 检查 Maven Assembly 发行包是否存在。
# 参数：无，固定读取 LOCAL_SERVER_DIST_ARCHIVE。
# 返回：文件存在时返回，缺失时终止并输出准备命令。
# 作者：lvdaxianer@yeah.net
# 日期：2026-07-21
require_server_distribution() {
  # 发行包存在时继续，否则提示标准准备脚本。
  if [ -f "${LOCAL_SERVER_DIST_ARCHIVE}" ]; then
    return
  else
    printf 'Missing local server distribution artifact: %s\n' "${LOCAL_SERVER_DIST_ARCHIVE}" >&2
    printf 'Run scripts/prepare-docker-build-context.sh before building images.\n' >&2
    exit 1
  fi
}

# 校验 PostgreSQL 本地 Debian 依赖闭包。
# 参数 platform：目标 Docker 平台。
# 参数 archive：PostgreSQL Debian 归档路径。
# 返回：闭包完整时返回，不完整时终止构建。
# 作者：lvdaxianer@yeah.net
# 日期：2026-07-21
validate_postgres_runtime_archive() {
  # 保存平台和归档，交给共享 PostgreSQL 校验库。
  local platform="$1"
  local archive="$2"

  # 依赖闭包完整时继续，否则输出统一修复指引。
  if validate_postgres_archive "${platform}" "${archive}"; then
    return
  else
    report_incomplete_postgres_archive "${archive}"
    exit 1
  fi
}

# 检查指定平台的全部本地 runtime artifacts。
# 参数 platform：目标 Docker 平台。
# 返回：全部输入有效时返回，任一输入无效时终止构建。
# 作者：lvdaxianer@yeah.net
# 日期：2026-07-21
require_runtime_artifacts() {
  # 解析平台和三个本地运行时归档路径。
  local platform="$1"
  set -- $(platform_suffix "${platform}")
  local jdk_archive="${RUNTIME_DIR}/jdk-21_linux-${4}_bin.tar.gz"
  local node_archive="${RUNTIME_DIR}/node-v22-linux-${5}.tar.gz"
  local postgres_archive="${RUNTIME_DIR}/postgresql-${POSTGRES_MAJOR}-ubuntu22.04-${6}-debs.tar.gz"

  require_artifact_file "${jdk_archive}" "${platform}"
  require_artifact_file "${node_archive}" "${platform}"
  require_artifact_file "${postgres_archive}" "${platform}"
  require_server_distribution
  validate_server_distribution_version
  validate_postgres_runtime_archive "${platform}" "${postgres_archive}"
}

# 输出 PostgreSQL 归档不完整的修复指引。
# 参数 archive：不完整的 PostgreSQL 归档路径。
# 返回：无返回值，只向标准错误输出修复方向。
# 作者：lvdaxianer@yeah.net
# 日期：2026-07-21
report_incomplete_postgres_archive() {
  printf 'PostgreSQL runtime archive is incomplete: %s\n' "$1" >&2
  printf 'Run scripts/prepare-container-runtimes.sh with a full PostgreSQL Debian dependency closure.\n' >&2
}

# 构建指定平台的可复用 runtime 基础镜像。
# 参数 platform：目标 Docker 平台。
# 参数 base_tag：基础镜像标签。
# 返回：Docker build 成功时返回，失败时终止。
# 作者：lvdaxianer@yeah.net
# 日期：2026-07-21
build_runtime_base_image() {
  # 解析平台后缀，并构建只包含本地运行时的基础层。
  local platform="$1"
  local base_tag="$2"
  set -- $(platform_suffix "${platform}")
  docker build \
    --pull=false \
    --platform "${platform}" \
    -f Dockerfile.runtime-base \
    --build-arg "LOCAL_JDK_ARCHIVE=${RUNTIME_DIR}/jdk-21_linux-${4}_bin.tar.gz" \
    --build-arg "LOCAL_NODE_ARCHIVE=${RUNTIME_DIR}/node-v22-linux-${5}.tar.gz" \
    --build-arg "LOCAL_POSTGRES_DEB_ARCHIVE=${RUNTIME_DIR}/postgresql-${POSTGRES_MAJOR}-ubuntu22.04-${6}-debs.tar.gz" \
    -t "${IMAGE_REPOSITORY}:${base_tag}" \
    .
}

# 构建指定平台的最终 DocLens 应用镜像。
# 参数 platform：目标 Docker 平台。
# 参数 image_tag：不可变应用镜像标签。
# 参数 base_tag：已构建的 runtime 基础镜像标签。
# 返回：Docker build 成功时返回，失败时终止。
# 作者：lvdaxianer@yeah.net
# 日期：2026-07-21
build_application_image() {
  # 保存三个构建参数，并复制匹配版本的 Assembly。
  local platform="$1"
  local image_tag="$2"
  local base_tag="$3"
  docker build \
    --pull=false \
    --platform "${platform}" \
    --build-arg "DOCLENS_RUNTIME_BASE_IMAGE=${IMAGE_REPOSITORY}:${base_tag}" \
    --build-arg "LOCAL_SERVER_DIST_ARCHIVE=${LOCAL_SERVER_DIST_ARCHIVE}" \
    -t "${IMAGE_REPOSITORY}:${image_tag}" \
    .
}

# 刷新最终镜像的架构兼容别名。
# 参数 image_tag：已成功构建的不可变标签。
# 参数 compatibility_tag：需要刷新的兼容标签。
# 返回：Docker tag 成功时返回，失败时终止。
# 作者：lvdaxianer@yeah.net
# 日期：2026-07-21
refresh_compatibility_alias() {
  # 只有不可变镜像构建成功后才刷新架构兼容别名。
  docker tag "${IMAGE_REPOSITORY}:$1" "${IMAGE_REPOSITORY}:$2"
}

# 构建指定平台镜像并刷新版本标签和兼容标签。
# 参数 platform：目标 Docker 平台。
# 返回：构建和标签成功时返回，任一步失败时终止。
# 作者：lvdaxianer@yeah.net
# 日期：2026-07-21
build_platform_image() {
  # 解析平台标签并按固定顺序执行校验、构建和别名刷新。
  local platform="$1"
  set -- $(platform_suffix "${platform}")
  require_runtime_artifacts "${platform}"
  build_runtime_base_image "${platform}" "$2"
  build_application_image "${platform}" "$1" "$2"
  refresh_compatibility_alias "$1" "$3"
}

for platform in ${PLATFORMS}; do
  build_platform_image "${platform}"
done
