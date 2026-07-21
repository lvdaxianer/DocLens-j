#!/usr/bin/env bash
# 校验首个公测版本在构建、打包和部署入口中的发布身份保持一致。
#
# 发布契约范围：
# - Maven 根项目决定后端模块和 Assembly 的产品版本。
# - Maven 子模块的 parent 引用必须与根项目同步。
# - Dashboard package.json 暴露前端包版本。
# - Dashboard package-lock.json 同时保存根版本和工作区版本。
# - Docker 构建脚本负责生成架构独立的不可变标签。
# - Docker 构建脚本还保留架构兼容别名。
# - Assembly 内部 JAR 版本必须与镜像发布版本一致。
# - x86 Compose 默认固定到不可变 amd64 标签。
# - x86 Compose 允许运维通过 DOCLENS_IMAGE_TAG 回滚。
# - Helm chart 版本表示交付模板自身版本。
# - Helm appVersion 表示部署的 DocLens 应用版本。
# - Helm 默认镜像必须是当前不可变 amd64 公测镜像。
#
# 标签约定：
# - amd64 不可变标签格式为 <version>-amd64。
# - arm64 不可变标签格式为 <version>-arm64。
# - amd64 兼容标签只用于已有本地构建流程。
# - arm64 兼容标签只用于已有本地构建流程。
# - Compose 和 Helm 不得默认使用兼容标签。
# - 正式部署与回滚必须选择不可变标签。
#
# 一致性约定：
# - 后端、前端和部署元数据使用同一个 SemVer 公测版本。
# - 当前公测版本固定为 0.1.0-beta.1。
# - 后续 beta 版本需要同步所有发布面后再构建。
# - 不能只修改 Docker 标签而保留旧 Assembly。
# - 不能只修改 Helm appVersion 而保留 latest 镜像。
# - 不能在当前有效文档中继续展示 SNAPSHOT 版本。
#
# 失败语义：
# - 任一发布面漂移都会累计一个明确失败项。
# - JSON 文件使用 Node.js 结构化解析，避免字段顺序影响。
# - YAML 和 shell 使用精确发布行或固定契约文本检查。
# - 所有失败项输出后脚本统一返回非零状态。
# - 成功时只输出稳定的发布契约通过标志。
# - 本脚本不构建镜像，也不修改任何发布文件。
# - 本脚本可在提交前和后续发布流水线中重复执行。
set -euo pipefail

# 公测版本是本脚本唯一允许的不可变发布版本。
EXPECTED_VERSION="0.1.0-beta.1"
# 镜像仓库名与现有本地交付约定一致。
EXPECTED_IMAGE_REPOSITORY="doclens"
# x86 和 arm64 分别使用可追踪的架构后缀。
EXPECTED_AMD64_TAG="${EXPECTED_VERSION}-amd64"
EXPECTED_ARM64_TAG="${EXPECTED_VERSION}-arm64"
# 记录所有契约失败项，便于一次展示完整的版本漂移。
failures=0

# 要求文件包含固定文本。
# 参数 file：待检查文件。
# 参数 literal：必须出现的固定文本。
# 参数 message：失败时输出的契约说明。
# 返回：命中时保持失败数，否则累计一个失败项。
# 作者：lvdaxianer@yeah.net
# 日期：2026-07-21
require_literal() {
  # 接收目标文件、固定文本和失败说明。
  local file="$1"
  local literal="$2"
  local message="$3"

  # 固定字符串不存在时累计失败，否则保持当前失败数。
  if ! grep -Fq -- "${literal}" "${file}"; then
    printf 'FAIL: %s\n' "${message}" >&2
    failures=$((failures + 1))
  else
    failures="${failures}"
  fi
}

# 要求配置文件存在一整行精确值。
# 参数 file：待检查配置文件。
# 参数 expected_line：包含缩进的完整预期行。
# 参数 message：失败时输出的契约说明。
# 返回：完整行存在时保持失败数，否则累计一个失败项。
# 作者：lvdaxianer@yeah.net
# 日期：2026-07-21
require_exact_line() {
  # 接收目标文件、完整行和值漂移说明。
  local file="$1"
  local expected_line="$2"
  local message="$3"

  # 完整行不存在时累计失败，否则保持当前失败数。
  if ! grep -Fxq -- "${expected_line}" "${file}"; then
    printf 'FAIL: %s\n' "${message}" >&2
    failures=$((failures + 1))
  else
    failures="${failures}"
  fi
}

# 要求 JSON 文件的根版本与公测版本完全一致。
# 参数 file：待结构化读取的 JSON 文件。
# 返回：版本一致时保持失败数，否则累计一个失败项。
# 作者：lvdaxianer@yeah.net
# 日期：2026-07-21
require_json_version() {
  # 接收 package JSON 文件路径。
  local file="$1"

  # Node.js 结构化读取 JSON，避免依赖文本格式和字段顺序。
  if ! node -e 'const fs=require("node:fs"); const value=JSON.parse(fs.readFileSync(process.argv[1], "utf8")); process.exit(value.version === process.argv[2] ? 0 : 1)' \
      "${file}" "${EXPECTED_VERSION}"; then
    printf 'FAIL: %s must use version %s.\n' "${file}" "${EXPECTED_VERSION}" >&2
    failures=$((failures + 1))
  else
    failures="${failures}"
  fi
}

# 校验 package-lock 顶层和工作区根包版本。
# 参数：无，固定检查 Dashboard lockfile。
# 返回：两个版本一致时保持失败数，否则累计一个失败项。
# 作者：lvdaxianer@yeah.net
# 日期：2026-07-21
require_lockfile_versions() {
  # 固定读取 Dashboard lockfile，避免调用方传入不相关文件。
  local file="doclens-dashboard/package-lock.json"

  # 结构化检查两个版本字段，防止只更新其中一个。
  if ! node -e 'const fs=require("node:fs"); const value=JSON.parse(fs.readFileSync(process.argv[1], "utf8")); process.exit(value.version === process.argv[2] && value.packages?.[""]?.version === process.argv[2] ? 0 : 1)' \
      "${file}" "${EXPECTED_VERSION}"; then
    printf 'FAIL: %s root versions must use %s.\n' "${file}" "${EXPECTED_VERSION}" >&2
    failures=$((failures + 1))
  else
    failures="${failures}"
  fi
}

# Maven 父项目和子模块父引用都必须声明同一个公测版本。
for pom_file in pom.xml doclens-api/pom.xml doclens-core/pom.xml \
  doclens-spring-boot-starter/pom.xml doclens-server/pom.xml; do
  require_literal "${pom_file}" "<version>${EXPECTED_VERSION}</version>" \
    "${pom_file} must use the public beta version."
done

# Dashboard 元数据及 lockfile 根项目版本必须同步。
require_json_version "doclens-dashboard/package.json"
require_lockfile_versions

# 构建脚本默认生成不可变架构标签，并刷新旧架构标签作为兼容别名。
require_literal "scripts/build-local-runtime-images.sh" \
  "RELEASE_VERSION=\"\${RELEASE_VERSION:-${EXPECTED_VERSION}}\"" \
  'Build script must define the public beta release version.'
require_literal "scripts/build-local-runtime-images.sh" \
  'IMAGE_TAG_AMD64="${IMAGE_TAG_AMD64:-${RELEASE_VERSION}-amd64}"' \
  'Build script must derive the amd64 immutable tag from the release version.'
require_literal "scripts/build-local-runtime-images.sh" \
  'IMAGE_TAG_ARM64="${IMAGE_TAG_ARM64:-${RELEASE_VERSION}-arm64}"' \
  'Build script must derive the arm64 immutable tag from the release version.'
require_literal "scripts/build-local-runtime-images.sh" \
  'COMPATIBILITY_TAG_AMD64="${COMPATIBILITY_TAG_AMD64:-amd64}"' \
  'Build script must retain the amd64 compatibility alias.'
require_literal "scripts/build-local-runtime-images.sh" \
  'COMPATIBILITY_TAG_ARM64="${COMPATIBILITY_TAG_ARM64:-arm64}"' \
  'Build script must retain the arm64 compatibility alias.'
require_literal "scripts/build-local-runtime-images.sh" 'docker tag' \
  'Build script must refresh compatibility aliases after a successful build.'
require_literal "scripts/build-local-runtime-images.sh" 'validate_server_distribution_version' \
  'Build script must reject a server distribution whose version differs from the image version.'

# Compose 默认固定 x86 公测镜像，同时保留显式版本覆盖入口。
require_literal "docker/x86/docker-compose.yml" \
  "image: ${EXPECTED_IMAGE_REPOSITORY}:\${DOCLENS_IMAGE_TAG:-${EXPECTED_AMD64_TAG}}" \
  'x86 Compose must default to the immutable amd64 beta image.'

# Helm 的应用版本和默认镜像必须与公测版本一致。
require_exact_line "deploy/helm/doclens-j/Chart.yaml" "version: ${EXPECTED_VERSION}" \
  'Helm chart version must identify the public beta.'
require_exact_line "deploy/helm/doclens-j/Chart.yaml" "appVersion: \"${EXPECTED_VERSION}\"" \
  'Helm appVersion must identify the public beta.'
require_exact_line "deploy/helm/doclens-j/values.yaml" "  repository: ${EXPECTED_IMAGE_REPOSITORY}" \
  'Helm must use the DocLens image repository.'
require_exact_line "deploy/helm/doclens-j/values.yaml" "  tag: ${EXPECTED_AMD64_TAG}" \
  'Helm must default to the immutable amd64 beta image.'

# 交付文档必须展示两个不可变架构标签和 Compose 覆盖方式。
for packaging_doc in docs/packaging.md docs/packaging_en.md docker/x86/README.md; do
  require_literal "${packaging_doc}" "${EXPECTED_IMAGE_REPOSITORY}:${EXPECTED_AMD64_TAG}" \
    "${packaging_doc} must document the immutable amd64 beta image."
done
require_literal "docs/packaging.md" "${EXPECTED_IMAGE_REPOSITORY}:${EXPECTED_ARM64_TAG}" \
  'Chinese packaging doc must document the immutable arm64 beta image.'
require_literal "docs/packaging_en.md" "${EXPECTED_IMAGE_REPOSITORY}:${EXPECTED_ARM64_TAG}" \
  'English packaging doc must document the immutable arm64 beta image.'
require_literal "docker/x86/README.md" 'DOCLENS_IMAGE_TAG=' \
  'x86 README must explain how to override the pinned image tag.'

# 有效构建和 SDK 文档不能继续暴露旧 SNAPSHOT 版本。
if rg -n '0\.1\.0-SNAPSHOT' pom.xml doclens-*/pom.xml scripts/run-backend-dev.sh \
    docs/sdk.md docs/sdk-http-delivery.md docs/packaging.md docs/packaging_en.md >/dev/null; then
  printf 'FAIL: active build or documentation files still reference 0.1.0-SNAPSHOT.\n' >&2
  failures=$((failures + 1))
else
  failures="${failures}"
fi

# 任一版本契约失败时返回非零状态，否则输出唯一成功标志。
if [ "${failures}" -gt 0 ]; then
  exit 1
else
  printf 'Public beta release version contract verified.\n'
fi
