#!/usr/bin/env bash
set -euo pipefail

dockerfile="${1:-Dockerfile}"
entrypoint="${2:-docker/entrypoint.sh}"
runtime_script="${3:-scripts/prepare-container-runtimes.sh}"
packaging_doc="${4:-docs/packaging.md}"
build_script="${5:-scripts/build-local-runtime-images.sh}"
postgres_lib="${6:-scripts/postgres-deb-bundle.sh}"
build_context_script="${7:-scripts/prepare-docker-build-context.sh}"
runtime_base_dockerfile="${8:-Dockerfile.runtime-base}"

failures=0

require_pattern() {
  local file="$1"
  local pattern="$2"
  local message="$3"

  if ! grep -Eq -- "${pattern}" "${file}"; then
    printf 'FAIL: %s\n' "${message}" >&2
    failures=$((failures + 1))
  fi
}

reject_pattern() {
  local file="$1"
  local pattern="$2"
  local message="$3"

  if grep -Eq -- "${pattern}" "${file}"; then
    printf 'FAIL: %s\n' "${message}" >&2
    failures=$((failures + 1))
  fi
}

require_pattern "${dockerfile}" '^ARG DOCLENS_RUNTIME_BASE_IMAGE=' \
  'Dockerfile must accept the reusable DocLens runtime base image.'
require_pattern "${dockerfile}" '^FROM \$\{DOCLENS_RUNTIME_BASE_IMAGE\} AS runtime$' \
  'Final runtime stage must use the reusable DocLens runtime base image.'
reject_pattern "${dockerfile}" '^ARG POSTGRES_IMAGE=' \
  'Dockerfile must not use PostgreSQL image as the final runtime base.'
reject_pattern "${dockerfile}" '^FROM \$\{POSTGRES_IMAGE\} AS runtime$' \
  'Final runtime stage must not be based on the PostgreSQL image.'
reject_pattern "${dockerfile}" '(curl|wget).*https?://' \
  'Dockerfile must not download runtime artifacts during build.'
reject_pattern "${dockerfile}" 'RUN npm ci' \
  'Dockerfile must not resolve npm dependencies during build.'
reject_pattern "${dockerfile}" 'RUN mvn ' \
  'Dockerfile must not resolve Maven dependencies during build.'
reject_pattern "${dockerfile}" '^# syntax=' \
  'Dockerfile must not require an external Dockerfile frontend image.'
require_pattern "${dockerfile}" 'ARG LOCAL_SERVER_DIST_ARCHIVE=' \
  'Dockerfile must accept a local Maven Assembly server distribution archive.'
require_pattern "${dockerfile}" 'COPY \$\{LOCAL_SERVER_DIST_ARCHIVE\}' \
  'Dockerfile must copy the local server distribution archive from build context.'

require_pattern "${runtime_base_dockerfile}" '^ARG UBUNTU_IMAGE=ubuntu:' \
  'Runtime base Dockerfile must define a local Ubuntu base image arg.'
require_pattern "${runtime_base_dockerfile}" '^FROM \$\{UBUNTU_IMAGE\} AS runtime-base$' \
  'Runtime base Dockerfile must use the Ubuntu image arg.'
require_pattern "${runtime_base_dockerfile}" 'ARG LOCAL_JDK_ARCHIVE=' \
  'Runtime base Dockerfile must accept a local JDK21 archive.'
require_pattern "${runtime_base_dockerfile}" 'ARG LOCAL_NODE_ARCHIVE=' \
  'Runtime base Dockerfile must accept a local Node22 archive.'
require_pattern "${runtime_base_dockerfile}" 'ARG LOCAL_POSTGRES_DEB_ARCHIVE=' \
  'Runtime base Dockerfile must accept a local PostgreSQL Debian package archive.'
require_pattern "${runtime_base_dockerfile}" 'COPY \$\{LOCAL_JDK_ARCHIVE\}' \
  'Runtime base Dockerfile must copy the local JDK archive from build context.'
require_pattern "${runtime_base_dockerfile}" 'COPY \$\{LOCAL_NODE_ARCHIVE\}' \
  'Runtime base Dockerfile must copy the local Node archive from build context.'
require_pattern "${runtime_base_dockerfile}" 'COPY \$\{LOCAL_POSTGRES_DEB_ARCHIVE\}' \
  'Runtime base Dockerfile must copy the local PostgreSQL package archive from build context.'
require_pattern "${runtime_base_dockerfile}" 'dpkg -i .*/postgres' \
  'Runtime base Dockerfile must install PostgreSQL from local Debian packages.'
reject_pattern "${runtime_base_dockerfile}" '(curl|wget).*https?://' \
  'Runtime base Dockerfile must not download runtime artifacts during build.'

require_pattern "${entrypoint}" 'initdb' \
  'Entrypoint must initialize PostgreSQL without the official image entrypoint.'
require_pattern "${entrypoint}" 'pg_ctl' \
  'Entrypoint must start PostgreSQL without the official image entrypoint.'
require_pattern "${entrypoint}" 'chown postgres:postgres "\$\{password_file\}"' \
  'Entrypoint must make the initdb password file readable by the postgres user.'
require_pattern "${entrypoint}" "<<'SQL'" \
  'Entrypoint must feed database-existence SQL through stdin so psql variables are expanded.'
reject_pattern "${entrypoint}" "--command \"SELECT 1 FROM pg_database WHERE datname = :'database_name'\"" \
  'Entrypoint must not rely on psql variable expansion inside a --command string.'
reject_pattern "${entrypoint}" '/usr/local/bin/docker-entrypoint\.sh' \
  'Entrypoint must not depend on the official PostgreSQL image entrypoint.'

require_pattern "${runtime_script}" 'JDK_SOURCE=' \
  'Runtime preparation script must accept a local JDK source archive.'
require_pattern "${runtime_script}" 'DOCKER_PLATFORM=' \
  'Runtime preparation script must pin the Docker platform for architecture consistency.'
require_pattern "${runtime_script}" 'NODE_IMAGE=' \
  'Runtime preparation script must create Node22 archive from a local Node image.'
require_pattern "${runtime_script}" 'NODE_SOURCE=' \
  'Runtime preparation script must accept a downloaded local Node archive.'
require_pattern "${runtime_script}" 'POSTGRES_DOWNLOAD_IMAGE=' \
  'Runtime preparation script must prepare PostgreSQL packages from a local Ubuntu image.'
require_pattern "${runtime_script}" 'POSTGRES_DEB_SOURCE_DIR=' \
  'Runtime preparation script must accept a local PostgreSQL Debian package directory.'
require_pattern "${runtime_script}" 'LOCAL_POSTGRES_DEB_ARCHIVE=' \
  'Runtime preparation script must produce the PostgreSQL Debian package archive.'
require_pattern "${runtime_script}" 'validate_postgres_deb_bundle' \
  'Runtime preparation script must validate local PostgreSQL Debian package completeness.'
require_pattern "${runtime_script}" '--reinstall' \
  'Runtime preparation script must explicitly download PostgreSQL dependencies that may already be installed in the downloader image.'
require_pattern "${runtime_script}" 'libldap-2\.5-0' \
  'Runtime preparation script must explicitly include the PostgreSQL LDAP dependency in the local Debian bundle.'
require_pattern "${runtime_script}" 'openssl' \
  'Runtime preparation script must explicitly include the PostgreSQL ssl-cert runtime dependency in the local Debian bundle.'
require_pattern "${runtime_base_dockerfile}" 'DEBIAN_FRONTEND=noninteractive' \
  'Runtime base Dockerfile must install local Debian packages non-interactively.'
require_pattern "${packaging_doc}" 'prepare-container-runtimes\.sh' \
  'Packaging doc must explain how to prepare local runtime artifacts.'
require_pattern "${packaging_doc}" 'LOCAL_NODE_ARCHIVE' \
  'Packaging doc must document the local Node archive build arg.'
require_pattern "${packaging_doc}" 'LOCAL_POSTGRES_DEB_ARCHIVE' \
  'Packaging doc must document the local PostgreSQL package archive build arg.'
require_pattern "${packaging_doc}" '完整依赖闭包' \
  'Packaging doc must explain that PostgreSQL local Debian packages need the full dependency closure.'
require_pattern "${build_script}" 'linux/amd64' \
  'Build script must support a linux/amd64 image.'
require_pattern "${build_script}" 'linux/arm64' \
  'Build script must support a linux/arm64 image.'
require_pattern "${build_script}" 'IMAGE_REPOSITORY="\$\{IMAGE_REPOSITORY:-doclens\}"' \
  'Build script must default to the requested doclens image repository.'
require_pattern "${build_script}" 'IMAGE_TAG_AMD64="\$\{IMAGE_TAG_AMD64:-amd64\}"' \
  'Build script must default the x86 image tag to amd64.'
require_pattern "${build_script}" 'IMAGE_TAG_ARM64="\$\{IMAGE_TAG_ARM64:-arm64\}"' \
  'Build script must default the arm image tag to arm64.'
require_pattern "${build_script}" 'BASE_IMAGE_TAG_AMD64="\$\{BASE_IMAGE_TAG_AMD64:-base-amd64\}"' \
  'Build script must default the x86 runtime base image tag to base-amd64.'
require_pattern "${build_script}" 'BASE_IMAGE_TAG_ARM64="\$\{BASE_IMAGE_TAG_ARM64:-base-arm64\}"' \
  'Build script must default the arm runtime base image tag to base-arm64.'
require_pattern "${build_script}" 'Dockerfile.runtime-base' \
  'Build script must build the reusable runtime base Dockerfile.'
require_pattern "${build_script}" 'DOCLENS_RUNTIME_BASE_IMAGE' \
  'Build script must pass the reusable runtime base image to the final Docker build.'
require_pattern "${build_script}" 'validate_postgres_archive' \
  'Build script must validate PostgreSQL package archives before docker build.'
require_pattern "${build_script}" 'LOCAL_SERVER_DIST_ARCHIVE' \
  'Build script must pass the local server distribution archive to docker build.'
require_pattern "${postgres_lib}" 'postgresql-common' \
  'PostgreSQL bundle library must require PostgreSQL common Debian dependencies.'
require_pattern "${postgres_lib}" 'duplicate PostgreSQL Debian package' \
  'PostgreSQL bundle library must reject duplicate PostgreSQL Debian package names.'
require_pattern "${build_context_script}" 'npm ci' \
  'Build context preparation script must prepare dashboard dependencies on the host.'
require_pattern "${build_context_script}" 'npm run build' \
  'Build context preparation script must build dashboard assets on the host.'
require_pattern "${build_context_script}" 'mvn -pl doclens-server -am -Pdist' \
  'Build context preparation script must create the Maven Assembly distribution on the host.'
require_pattern "${build_context_script}" 'doclens-server-dist\.tar\.gz' \
  'Build context preparation script must produce the local server distribution archive.'

if [ "${failures}" -gt 0 ]; then
  exit 1
fi

printf 'Dockerfile local runtime contract verified.\n'
