#!/usr/bin/env bash
set -euo pipefail

entrypoint="${1:-docker/entrypoint.sh}"
helm_values="${2:-deploy/helm/doclens-j/values.yaml}"
helm_configmap="${3:-deploy/helm/doclens-j/templates/app-configmap.yaml}"
helm_deployment="${4:-deploy/helm/doclens-j/templates/deployment.yaml}"
packaging_doc="${5:-docs/packaging.md}"
packaging_doc_en="${6:-docs/packaging_en.md}"
env_example="${7:-doclens-server/src/assembly/conf/doclens.env.example}"
dockerfile="${8:-Dockerfile}"

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

require_pattern "${entrypoint}" 'DOCLENS_SERVER_PORT' \
  'Entrypoint must expose an explicit DocLens server port environment variable.'
require_pattern "${entrypoint}" 'SERVER_PORT' \
  'Entrypoint must map the DocLens server port to Spring Boot server port env.'
require_pattern "${entrypoint}" 'POSTGRES_PORT' \
  'Entrypoint must expose a configurable PostgreSQL port.'
require_pattern "${entrypoint}" 'SPRING_CONFIG_ADDITIONAL_LOCATION' \
  'Entrypoint must wire an additional Spring config location for mounted overrides.'
require_pattern "${entrypoint}" 'DOCLENS_CONFIG_DIR' \
  'Entrypoint must expose a configurable external DocLens config directory.'
require_pattern "${entrypoint}" 'pg_isready .* -p "\$\{POSTGRES_PORT\}"' \
  'Entrypoint readiness check must honor the configurable PostgreSQL port.'
require_pattern "${entrypoint}" 'postgres -D "\$\{PGDATA\}" -p "\$\{POSTGRES_PORT\}"' \
  'Entrypoint must start PostgreSQL on the configured port.'

require_pattern "${dockerfile}" '/opt/doclens/config' \
  'Dockerfile must declare the external config directory as a mountable volume.'

require_pattern "${helm_values}" 'serverPort:' \
  'Helm values must expose a configurable application server port.'
require_pattern "${helm_values}" 'extraVolumeMounts:' \
  'Helm values must expose extra volume mounts for runtime config.'
require_pattern "${helm_values}" 'extraVolumes:' \
  'Helm values must expose extra volumes for runtime config.'
require_pattern "${helm_configmap}" 'SERVER_PORT:' \
  'Helm app ConfigMap must publish the application server port env.'
require_pattern "${helm_deployment}" 'containerPort: \{\{ \.Values\.app\.serverPort \}\}' \
  'Helm Deployment must use the configurable application container port.'
require_pattern "${helm_deployment}" '\.Values\.app\.extraVolumeMounts' \
  'Helm Deployment must render extra volume mounts.'
require_pattern "${helm_deployment}" '\.Values\.app\.extraVolumes' \
  'Helm Deployment must render extra volumes.'

require_pattern "${env_example}" '^DOCLENS_SERVER_PORT=' \
  'Env example must document the configurable DocLens server port.'
require_pattern "${env_example}" '^POSTGRES_PORT=' \
  'Env example must document the configurable PostgreSQL port.'
require_pattern "${env_example}" '^DOCLENS_CONFIG_DIR=' \
  'Env example must document the external config directory.'
require_pattern "${env_example}" '^SPRING_CONFIG_ADDITIONAL_LOCATION=' \
  'Env example must document the additional Spring config location hook.'

require_pattern "${packaging_doc}" 'DOCLENS_SERVER_PORT' \
  'Packaging doc must explain the application port override env.'
require_pattern "${packaging_doc}" 'POSTGRES_PORT' \
  'Packaging doc must explain the PostgreSQL port override env.'
require_pattern "${packaging_doc}" 'DOCLENS_CONFIG_DIR' \
  'Packaging doc must explain the external config directory.'
require_pattern "${packaging_doc}" 'SPRING_CONFIG_ADDITIONAL_LOCATION' \
  'Packaging doc must explain the Spring additional config location hook.'
require_pattern "${packaging_doc_en}" 'DOCLENS_SERVER_PORT' \
  'English packaging doc must explain the application port override env.'
require_pattern "${packaging_doc_en}" 'DOCLENS_CONFIG_DIR' \
  'English packaging doc must explain the external config directory.'

if [ "${failures}" -gt 0 ]; then
  exit 1
fi

printf 'Container runtime override contract verified.\n'
