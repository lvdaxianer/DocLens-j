#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "${ROOT_DIR}"
exec nodemon \
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
