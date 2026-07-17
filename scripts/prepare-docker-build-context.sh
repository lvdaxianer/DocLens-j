#!/usr/bin/env bash
set -Eeuo pipefail

BUILD_DIR="${BUILD_DIR:-docker/build}"
DASHBOARD_DIR="${DASHBOARD_DIR:-doclens-dashboard}"
SERVER_DIST_ARCHIVE="${SERVER_DIST_ARCHIVE:-${BUILD_DIR}/doclens-server-dist.tar.gz}"
MAVEN_ARGS="${MAVEN_ARGS:--Dmaven.test.skip=true}"

mkdir -p "${BUILD_DIR}"

# 在宿主机安装前端依赖并构建静态资源，避免 Docker build 现场下载 npm 依赖。
(
  cd "${DASHBOARD_DIR}"
  npm ci
  npm run build
)

# 在宿主机生成后端 Assembly 发行包，避免 Docker build 现场下载 Maven 依赖。
mvn -pl doclens-server -am -Pdist ${MAVEN_ARGS} package

latest_dist="$(find doclens-server/target -maxdepth 1 -type f -name 'doclens-server-*-dist.tar.gz' | sort | tail -n 1)"
if [ -z "${latest_dist}" ]; then
  printf 'Missing Maven Assembly distribution under doclens-server/target.\n' >&2
  exit 1
fi

cp "${latest_dist}" "${SERVER_DIST_ARCHIVE}"

printf 'Prepared Docker build artifact:\n'
printf '  %s\n' "${SERVER_DIST_ARCHIVE}"
