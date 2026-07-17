#!/usr/bin/env bash
set -Eeuo pipefail

RUNTIME_DIR="${RUNTIME_DIR:-docker/runtime}"
DOCKER_PLATFORM="${DOCKER_PLATFORM:-linux/amd64}"
NODE_IMAGE="${NODE_IMAGE:-node:22-bookworm-slim}"
NODE_VERSION="${NODE_VERSION:-22.23.1}"
POSTGRES_DOWNLOAD_IMAGE="${POSTGRES_DOWNLOAD_IMAGE:-ubuntu:22.04}"
POSTGRES_MAJOR="${POSTGRES_MAJOR:-16}"
POSTGRES_DEB_SOURCE_DIR="${POSTGRES_DEB_SOURCE_DIR:-${HOME}/Downloads}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "${SCRIPT_DIR}/postgres-deb-bundle.sh"

case "${DOCKER_PLATFORM}" in
  linux/amd64)
    JDK_ARCH="${JDK_ARCH:-x64}"
    NODE_ARCH="${NODE_ARCH:-x64}"
    POSTGRES_ARCH="${POSTGRES_ARCH:-x64}"
    ;;
  linux/arm64)
    JDK_ARCH="${JDK_ARCH:-aarch64}"
    NODE_ARCH="${NODE_ARCH:-arm64}"
    POSTGRES_ARCH="${POSTGRES_ARCH:-arm64}"
    ;;
  *)
    printf 'Unsupported DOCKER_PLATFORM: %s\n' "${DOCKER_PLATFORM}" >&2
    exit 1
    ;;
esac

JDK_SOURCE="${JDK_SOURCE:-${HOME}/Downloads/jdk-21_linux-${JDK_ARCH}_bin.tar.gz}"
NODE_SOURCE="${NODE_SOURCE:-${HOME}/Downloads/node-v${NODE_VERSION}-linux-${NODE_ARCH}.tar.xz}"
LOCAL_JDK_ARCHIVE="${LOCAL_JDK_ARCHIVE:-${RUNTIME_DIR}/jdk-21_linux-${JDK_ARCH}_bin.tar.gz}"
LOCAL_NODE_ARCHIVE="${LOCAL_NODE_ARCHIVE:-${RUNTIME_DIR}/node-v22-linux-${NODE_ARCH}.tar.gz}"
LOCAL_POSTGRES_DEB_ARCHIVE="${LOCAL_POSTGRES_DEB_ARCHIVE:-${RUNTIME_DIR}/postgresql-${POSTGRES_MAJOR}-ubuntu22.04-${POSTGRES_ARCH}-debs.tar.gz}"

POSTGRES_DEB_DOWNLOAD_SCRIPT="$(cat <<'CONTAINER_SCRIPT'
set -Eeuo pipefail

export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y --no-install-recommends ca-certificates curl gnupg
install -d /usr/share/postgresql-common/pgdg
curl -fsSL https://www.postgresql.org/media/keys/ACCC4CF8.asc \
  | gpg --dearmor -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.gpg
printf 'deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.gpg] http://apt.postgresql.org/pub/repos/apt jammy-pgdg main\n' \
  >/etc/apt/sources.list.d/pgdg.list
apt-get update
apt-get install -y --download-only --no-install-recommends \
  "postgresql-${POSTGRES_MAJOR}" \
  "postgresql-client-${POSTGRES_MAJOR}"
apt-get install -y --download-only --reinstall \
  libldap-2.5-0 \
  libreadline8 \
  openssl
mkdir -p /tmp/postgres-debs
cp /var/cache/apt/archives/*.deb /tmp/postgres-debs/
tar -czf "/runtime/${POSTGRES_ARCHIVE_NAME}" -C /tmp/postgres-debs .
CONTAINER_SCRIPT
)"

mkdir -p "${RUNTIME_DIR}"

# 复制本地 JDK 归档到 Docker build 上下文。
copy_jdk_archive() {
  if [ ! -f "${JDK_SOURCE}" ]; then
    printf 'Missing JDK archive: %s\n' "${JDK_SOURCE}" >&2
    exit 1
  fi

  cp "${JDK_SOURCE}" "${LOCAL_JDK_ARCHIVE}"
}

# 从本地 Node 镜像导出 /usr/local，形成 Node22 运行时归档。
archive_node_from_image() {
  temp_dir="$(mktemp -d)"
  container_id="$(docker create --platform "${DOCKER_PLATFORM}" "${NODE_IMAGE}")"

  docker cp "${container_id}:/usr/local" "${temp_dir}/node-v22-linux-${NODE_ARCH}"
  docker rm "${container_id}" >/dev/null
  tar -czf "${LOCAL_NODE_ARCHIVE}" -C "${temp_dir}" "node-v22-linux-${NODE_ARCH}"
  rm -rf "${temp_dir}"
}

# 将用户已下载的 Node22 归档重打成 gzip，避免 Ubuntu runtime 依赖 xz。
copy_node_archive() {
  if [ ! -f "${NODE_SOURCE}" ]; then
    return 1
  fi

  temp_dir="$(mktemp -d)"
  tar -xf "${NODE_SOURCE}" -C "${temp_dir}"
  node_root="$(find "${temp_dir}" -mindepth 1 -maxdepth 1 -type d | head -n 1)"
  if [ -z "${node_root}" ]; then
    printf 'Node archive has no top-level directory: %s\n' "${NODE_SOURCE}" >&2
    exit 1
  fi
  COPYFILE_DISABLE=1 tar -czf "${LOCAL_NODE_ARCHIVE}" -C "${temp_dir}" "$(basename "${node_root}")"
  rm -rf "${temp_dir}"
}

# 使用本地 Ubuntu 镜像预下载 PostgreSQL Debian 包归档。
archive_postgres_debs() {
  runtime_abs="$(cd "${RUNTIME_DIR}" && pwd)"
  archive_name="$(basename "${LOCAL_POSTGRES_DEB_ARCHIVE}")"

  docker run --rm \
    --platform "${DOCKER_PLATFORM}" \
    -e "POSTGRES_MAJOR=${POSTGRES_MAJOR}" \
    -e "POSTGRES_ARCHIVE_NAME=${archive_name}" \
    -v "${runtime_abs}:/runtime" \
    "${POSTGRES_DOWNLOAD_IMAGE}" \
    bash -c "${POSTGRES_DEB_DOWNLOAD_SCRIPT}"
}

# 打包用户已下载的 PostgreSQL Debian 包。
archive_postgres_debs_from_local_dir() {
  if ! find "${POSTGRES_DEB_SOURCE_DIR}" -maxdepth 1 -type f -name '*.deb' | grep -q .; then
    return 1
  fi

  temp_dir="$(mktemp -d)"
  find "${POSTGRES_DEB_SOURCE_DIR}" -maxdepth 1 -type f -name '*.deb' -exec cp {} "${temp_dir}/" \;
  if ! validate_postgres_deb_bundle "${temp_dir}" "${POSTGRES_DEB_SOURCE_DIR}"; then
    printf 'POSTGRES_DEB_SOURCE_DIR must contain the full PostgreSQL %s Debian dependency closure for %s.\n' \
      "${POSTGRES_MAJOR}" "${DOCKER_PLATFORM}" >&2
    printf 'Create it from an Ubuntu 22.04 environment with apt download/install --download-only, then rerun this script.\n' >&2
    exit 1
  fi
  tar -czf "${LOCAL_POSTGRES_DEB_ARCHIVE}" -C "${temp_dir}" .
  rm -rf "${temp_dir}"
}

copy_jdk_archive
copy_node_archive || archive_node_from_image
archive_postgres_debs_from_local_dir || archive_postgres_debs

printf 'Prepared runtime artifacts:\n'
printf '  %s\n' "${LOCAL_JDK_ARCHIVE}"
printf '  %s\n' "${LOCAL_NODE_ARCHIVE}"
printf '  %s\n' "${LOCAL_POSTGRES_DEB_ARCHIVE}"
