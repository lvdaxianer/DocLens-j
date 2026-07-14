#!/usr/bin/env sh
set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
JAR_FILE=$(find "$APP_HOME/lib" -maxdepth 1 -name 'doclens-server-*.jar' | head -n 1)

if [ -z "$JAR_FILE" ]; then
  echo "doclens-server jar was not found under $APP_HOME/lib" >&2
  exit 1
fi

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
export SPRING_CONFIG_ADDITIONAL_LOCATION="${SPRING_CONFIG_ADDITIONAL_LOCATION:-optional:file:$APP_HOME/conf/}"

exec java ${JAVA_OPTS:-} -jar "$JAR_FILE" "$@"
