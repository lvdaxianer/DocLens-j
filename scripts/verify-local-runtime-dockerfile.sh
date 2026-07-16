#!/usr/bin/env bash
set -euo pipefail

dockerfile="${1:-Dockerfile}"
entrypoint="${2:-docker/entrypoint.sh}"
runtime_script="${3:-scripts/prepare-container-runtimes.sh}"
packaging_doc="${4:-docs/packaging.md}"

failures=0

require_pattern() {
  local file="$1"
  local pattern="$2"
  local message="$3"

  if ! grep -Eq "${pattern}" "${file}"; then
    printf 'FAIL: %s\n' "${message}" >&2
    failures=$((failures + 1))
  fi
}

reject_pattern() {
  local file="$1"
  local pattern="$2"
  local message="$3"

  if grep -Eq "${pattern}" "${file}"; then
    printf 'FAIL: %s\n' "${message}" >&2
    failures=$((failures + 1))
  fi
}

require_pattern "${dockerfile}" '^ARG UBUNTU_IMAGE=ubuntu:' \
  'Dockerfile must define a local Ubuntu runtime base image arg.'
require_pattern "${dockerfile}" '^FROM \$\{UBUNTU_IMAGE\} AS runtime$' \
  'Final runtime stage must use the Ubuntu image arg.'
require_pattern "${dockerfile}" 'ARG LOCAL_JDK_ARCHIVE=' \
  'Dockerfile must accept a local JDK21 archive.'
require_pattern "${dockerfile}" 'ARG LOCAL_NODE_ARCHIVE=' \
  'Dockerfile must accept a local Node22 archive.'
require_pattern "${dockerfile}" 'ARG LOCAL_POSTGRES_DEB_ARCHIVE=' \
  'Dockerfile must accept a local PostgreSQL Debian package archive.'
require_pattern "${dockerfile}" 'COPY \$\{LOCAL_JDK_ARCHIVE\}' \
  'Dockerfile must copy the local JDK archive from build context.'
require_pattern "${dockerfile}" 'COPY \$\{LOCAL_NODE_ARCHIVE\}' \
  'Dockerfile must copy the local Node archive from build context.'
require_pattern "${dockerfile}" 'COPY \$\{LOCAL_POSTGRES_DEB_ARCHIVE\}' \
  'Dockerfile must copy the local PostgreSQL package archive from build context.'
require_pattern "${dockerfile}" 'dpkg -i .*/postgres' \
  'Dockerfile must install PostgreSQL from local Debian packages.'
reject_pattern "${dockerfile}" '^ARG POSTGRES_IMAGE=' \
  'Dockerfile must not use PostgreSQL image as the final runtime base.'
reject_pattern "${dockerfile}" '^FROM \$\{POSTGRES_IMAGE\} AS runtime$' \
  'Final runtime stage must not be based on the PostgreSQL image.'
reject_pattern "${dockerfile}" '(curl|wget).*https?://' \
  'Dockerfile must not download runtime artifacts during build.'

require_pattern "${entrypoint}" 'initdb' \
  'Entrypoint must initialize PostgreSQL without the official image entrypoint.'
require_pattern "${entrypoint}" 'pg_ctl' \
  'Entrypoint must start PostgreSQL without the official image entrypoint.'
reject_pattern "${entrypoint}" '/usr/local/bin/docker-entrypoint\.sh' \
  'Entrypoint must not depend on the official PostgreSQL image entrypoint.'

require_pattern "${runtime_script}" 'JDK_SOURCE=' \
  'Runtime preparation script must accept a local JDK source archive.'
require_pattern "${runtime_script}" 'DOCKER_PLATFORM=' \
  'Runtime preparation script must pin the Docker platform for architecture consistency.'
require_pattern "${runtime_script}" 'NODE_IMAGE=' \
  'Runtime preparation script must create Node22 archive from a local Node image.'
require_pattern "${runtime_script}" 'POSTGRES_DOWNLOAD_IMAGE=' \
  'Runtime preparation script must prepare PostgreSQL packages from a local Ubuntu image.'
require_pattern "${runtime_script}" 'LOCAL_POSTGRES_DEB_ARCHIVE=' \
  'Runtime preparation script must produce the PostgreSQL Debian package archive.'
require_pattern "${packaging_doc}" 'prepare-container-runtimes\.sh' \
  'Packaging doc must explain how to prepare local runtime artifacts.'
require_pattern "${packaging_doc}" 'LOCAL_NODE_ARCHIVE' \
  'Packaging doc must document the local Node archive build arg.'
require_pattern "${packaging_doc}" 'LOCAL_POSTGRES_DEB_ARCHIVE' \
  'Packaging doc must document the local PostgreSQL package archive build arg.'

if [ "${failures}" -gt 0 ]; then
  exit 1
fi

printf 'Dockerfile local runtime contract verified.\n'
