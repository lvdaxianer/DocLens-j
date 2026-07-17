#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export DOCLENS_INTEGRATIONS_OPEN_WEBUI_INTERNAL_TOKEN="${DOCLENS_INTEGRATIONS_OPEN_WEBUI_INTERNAL_TOKEN:-7d3079585812617132d4b29611eb5dbb524475b1462a93413c49fb08102899ea}"
export DOCLENS_DB_URL="${DOCLENS_DB_URL:-jdbc:postgresql://localhost:${DOCLENS_POSTGRES_PORT:-5432}/${DOCLENS_POSTGRES_DB:-doclens}}"
export DOCLENS_DB_USERNAME="${DOCLENS_DB_USERNAME:-${DOCLENS_POSTGRES_USER:-doclens}}"
export DOCLENS_DB_PASSWORD="${DOCLENS_DB_PASSWORD:-${DOCLENS_POSTGRES_PASSWORD:-doclens}}"
export DOCLENS_DB_DRIVER="${DOCLENS_DB_DRIVER:-org.postgresql.Driver}"
# export MINIMAX_API_KEY=replace-with-real-secret
# export DASHSCOPE_API_KEY=replace-with-real-secret

cd "${ROOT_DIR}"
exec nodemon \
  --no-stdin \
  --watch "${ROOT_DIR}/doclens-server/src/main" \
  --watch "${ROOT_DIR}/doclens-core/src/main" \
  --watch "${ROOT_DIR}/doclens-api/src/main" \
  --watch "${ROOT_DIR}/doclens-spring-boot-starter/src/main" \
  --watch "${ROOT_DIR}/pom.xml" \
  --watch "${ROOT_DIR}/doclens-server/pom.xml" \
  --ext java,yml,xml \
  --delay 1.5 \
  --signal SIGTERM \
  --exec "sh -c 'cd ${ROOT_DIR} && mvn -pl doclens-server -am package -DskipTests && java -jar ${ROOT_DIR}/doclens-server/target/doclens-server-0.1.0-SNAPSHOT.jar'"
