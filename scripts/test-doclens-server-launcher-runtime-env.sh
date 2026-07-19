#!/usr/bin/env bash
set -euo pipefail

repo_root="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
launcher_source="${repo_root}/doclens-server/src/assembly/bin/doclens-server.sh"
work_dir="$(mktemp -d)"
trap 'rm -rf "${work_dir}"' EXIT

app_home="${work_dir}/app"
mkdir -p "${app_home}/bin" "${app_home}/lib" "${app_home}/config" "${work_dir}/fake-bin"
cp "${launcher_source}" "${app_home}/bin/doclens-server.sh"
chmod +x "${app_home}/bin/doclens-server.sh"
touch "${app_home}/lib/doclens-server-0.0-test.jar"

cat >"${app_home}/config/runtime.env" <<'EOF'
SPRING_PROFILES_ACTIVE=prod
POSTGRES_DB=doclens
POSTGRES_USER=doclens
POSTGRES_PASSWORD=replace-with-strong-password
POSTGRES_PORT=5432
DOCLENS_DB_URL=
DOCLENS_DB_USERNAME=
DOCLENS_DB_PASSWORD=
EOF

cat >"${work_dir}/fake-bin/java" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
printf 'SPRING_PROFILES_ACTIVE=%s\n' "${SPRING_PROFILES_ACTIVE:-}" >"${LAUNCHER_CAPTURE_FILE}"
printf 'SPRING_CONFIG_ADDITIONAL_LOCATION=%s\n' "${SPRING_CONFIG_ADDITIONAL_LOCATION:-}" >>"${LAUNCHER_CAPTURE_FILE}"
printf 'DOCLENS_DB_URL=%s\n' "${DOCLENS_DB_URL:-}" >>"${LAUNCHER_CAPTURE_FILE}"
printf 'DOCLENS_DB_USERNAME=%s\n' "${DOCLENS_DB_USERNAME:-}" >>"${LAUNCHER_CAPTURE_FILE}"
printf 'DOCLENS_DB_PASSWORD=%s\n' "${DOCLENS_DB_PASSWORD:-}" >>"${LAUNCHER_CAPTURE_FILE}"
printf 'argv=%s\n' "$*" >>"${LAUNCHER_CAPTURE_FILE}"
EOF
chmod +x "${work_dir}/fake-bin/java"

capture_file="${work_dir}/captured-env.txt"
PATH="${work_dir}/fake-bin:${PATH}" \
LAUNCHER_CAPTURE_FILE="${capture_file}" \
"${app_home}/bin/doclens-server.sh"

grep -Fx 'SPRING_PROFILES_ACTIVE=prod' "${capture_file}" >/dev/null
grep -Fx "SPRING_CONFIG_ADDITIONAL_LOCATION=optional:file:${app_home}/conf/,optional:file:${app_home}/config/" "${capture_file}" >/dev/null
grep -Fx 'DOCLENS_DB_URL=jdbc:postgresql://127.0.0.1:5432/doclens' "${capture_file}" >/dev/null
grep -Fx 'DOCLENS_DB_USERNAME=doclens' "${capture_file}" >/dev/null
grep -Fx 'DOCLENS_DB_PASSWORD=replace-with-strong-password' "${capture_file}" >/dev/null

printf 'doclens-server launcher runtime env verified.\n'
