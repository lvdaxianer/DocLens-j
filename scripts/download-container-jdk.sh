#!/usr/bin/env sh
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"

echo "scripts/download-container-jdk.sh is kept for compatibility."
echo "Use scripts/prepare-container-runtimes.sh to prepare JDK, Node, and PostgreSQL artifacts."
exec "${SCRIPT_DIR}/prepare-container-runtimes.sh" "$@"
