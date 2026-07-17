#!/usr/bin/env bash

POSTGRES_REQUIRED_DEB_PACKAGES=(
  "postgresql-${POSTGRES_MAJOR}"
  "postgresql-client-${POSTGRES_MAJOR}"
  postgresql-common
  postgresql-client-common
  libpq5
  libicu70
  libldap-2.5-0
  libllvm15
  libxml2
  libxslt1.1
  libreadline8
  openssl
  ssl-cert
  tzdata
)

# 提取 Debian 包文件名中的 package name。
postgres_deb_package_name() {
  local deb_basename
  deb_basename="$(basename "$1")"
  printf '%s\n' "${deb_basename%%_*}"
}

# 从目录中的 .deb 文件收集 package name。
postgres_collect_names_from_dir() {
  local deb_dir="$1"
  local package_names_file="$2"

  while IFS= read -r -d '' deb_file; do
    postgres_deb_package_name "${deb_file}"
  done < <(find "${deb_dir}" -maxdepth 1 -type f -name '*.deb' -print0) \
    | LC_ALL=C sort >"${package_names_file}"
}

# 从 tar.gz 归档中的 .deb 文件收集 package name。
postgres_collect_names_from_archive() {
  local archive="$1"
  local package_names_file="$2"

  while IFS= read -r archive_entry; do
    case "${archive_entry}" in
      *.deb) postgres_deb_package_name "${archive_entry}" ;;
    esac
  done < <(tar -tzf "${archive}") | LC_ALL=C sort >"${package_names_file}"
}

# 拒绝同一包名出现多个 .deb，避免 dpkg 安装不确定。
postgres_reject_duplicate_names() {
  local package_names_file="$1"
  local source_name="$2"
  local duplicate_names_file

  duplicate_names_file="$(mktemp)"
  LC_ALL=C uniq -d "${package_names_file}" >"${duplicate_names_file}"
  if [ -s "${duplicate_names_file}" ]; then
    printf 'Found duplicate PostgreSQL Debian package names in %s:\n' "${source_name}" >&2
    sed 's/^/  /' "${duplicate_names_file}" >&2
    printf 'Keep exactly one .deb per package name before preparing runtime artifacts.\n' >&2
    rm -f "${duplicate_names_file}"
    return 1
  fi
  rm -f "${duplicate_names_file}"
}

# 统计缺失的固定依赖包名。
postgres_count_missing_required_names() {
  local package_names_file="$1"
  local message_prefix="$2"
  local missing_count=0
  local package_name

  for package_name in "${POSTGRES_REQUIRED_DEB_PACKAGES[@]}"; do
    if ! grep -Fxq "${package_name}" "${package_names_file}"; then
      printf 'Missing %sPostgreSQL Debian package dependency: %s\n' "${message_prefix}" "${package_name}" >&2
      missing_count=$((missing_count + 1))
    fi
  done
  printf '%s\n' "${missing_count}"
}

# 检查 locales/locales-all 二选一依赖。
postgres_count_missing_locale_name() {
  local package_names_file="$1"
  local message_prefix="$2"

  if grep -Fxq locales "${package_names_file}" || grep -Fxq locales-all "${package_names_file}"; then
    printf '0\n'
    return
  fi
  printf 'Missing %sPostgreSQL Debian package dependency: locales or locales-all\n' "${message_prefix}" >&2
  printf '1\n'
}

# 校验包名集合是否为可安装的 PostgreSQL 依赖闭包。
postgres_validate_package_names() {
  local package_names_file="$1"
  local message_prefix="$2"
  local missing_count
  local locale_missing_count

  missing_count="$(postgres_count_missing_required_names "${package_names_file}" "${message_prefix}")"
  locale_missing_count="$(postgres_count_missing_locale_name "${package_names_file}" "${message_prefix}")"
  [ $((missing_count + locale_missing_count)) -eq 0 ]
}

# 校验本地目录形式的 PostgreSQL Debian 包闭包。
validate_postgres_deb_bundle() {
  local deb_dir="$1"
  local source_name="$2"
  local package_names_file

  package_names_file="$(mktemp)"
  postgres_collect_names_from_dir "${deb_dir}" "${package_names_file}"
  if ! postgres_reject_duplicate_names "${package_names_file}" "${source_name}"; then
    rm -f "${package_names_file}"
    return 1
  fi
  if ! postgres_validate_package_names "${package_names_file}" ""; then
    rm -f "${package_names_file}"
    return 1
  fi
  rm -f "${package_names_file}"
}

# 校验 tar.gz 形式的 PostgreSQL Debian 包闭包。
validate_postgres_archive() {
  local platform="$1"
  local archive="$2"
  local package_names_file

  package_names_file="$(mktemp)"
  postgres_collect_names_from_archive "${archive}" "${package_names_file}"
  if ! postgres_reject_duplicate_names "${package_names_file}" "${archive}"; then
    rm -f "${package_names_file}"
    return 1
  fi
  if ! postgres_validate_package_names "${package_names_file}" "${platform} "; then
    rm -f "${package_names_file}"
    return 1
  fi
  rm -f "${package_names_file}"
}
