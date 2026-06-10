# Dev Runtime Helper Scripts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 DocLens 仓库提供可重复执行的前后端开发启动、停止与状态查看脚本，并让后端代码变更后自动重启。

**Architecture:** 在仓库根目录新增 `scripts/` 下的 Shell 脚本，统一把 PID、日志和运行标记写入已忽略的 `var/` 目录。前端继续使用 Vite 开发服务，后端使用 `nodemon` 监听多模块源码变化，触发 Maven 打包并重启可执行 Jar。

**Tech Stack:** Bash, nodemon, Maven, Spring Boot Jar, Vite

---

### Task 1: 先写失败测试定义脚本契约

**Files:**
- Create: `scripts/test-dev-scripts.sh`
- Test: `scripts/test-dev-scripts.sh`

- [ ] **Step 1: Write the failing test**

```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

assert_file() {
  local file_path="$1"
  if [[ ! -f "${ROOT_DIR}/${file_path}" ]]; then
    echo "missing file: ${file_path}" >&2
    exit 1
  fi
}

assert_help() {
  local file_path="$1"
  local expected="$2"
  local output
  output="$("${ROOT_DIR}/${file_path}" --help)"
  if [[ "${output}" != *"${expected}"* ]]; then
    echo "unexpected help output for ${file_path}" >&2
    exit 1
  fi
}

assert_file "scripts/dev-up.sh"
assert_file "scripts/dev-down.sh"
assert_file "scripts/dev-status.sh"
assert_help "scripts/dev-up.sh" "启动前后端开发服务"
assert_help "scripts/dev-down.sh" "停止前后端开发服务"
assert_help "scripts/dev-status.sh" "查看前后端开发服务状态"
```

- [ ] **Step 2: Run test to verify it fails**

Run: `bash scripts/test-dev-scripts.sh`  
Expected: FAIL with `missing file`

- [ ] **Step 3: Write minimal implementation**

```bash
mkdir -p scripts
# 先补三个带 --help 的最小脚本骨架，后续任务再填充完整逻辑
```

- [ ] **Step 4: Run test to verify it passes**

Run: `bash scripts/test-dev-scripts.sh`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add scripts/test-dev-scripts.sh scripts/dev-up.sh scripts/dev-down.sh scripts/dev-status.sh
git commit -F /tmp/dev-runtime-task1.commit
```

### Task 2: 实现前后端启动、停止与状态逻辑

**Files:**
- Modify: `scripts/dev-up.sh`
- Modify: `scripts/dev-down.sh`
- Modify: `scripts/dev-status.sh`
- Modify: `scripts/test-dev-scripts.sh`

- [ ] **Step 1: Write the failing test**

```bash
if ! "${ROOT_DIR}/scripts/dev-status.sh" | grep -q "frontend"; then
  echo "status output missing frontend section" >&2
  exit 1
fi
```

- [ ] **Step 2: Run test to verify it fails**

Run: `bash scripts/test-dev-scripts.sh`  
Expected: FAIL with `status output missing frontend section`

- [ ] **Step 3: Write minimal implementation**

```bash
# dev-up.sh
# 1. 创建 var/dev 目录
# 2. 检查 node、npm、mvn、nodemon
# 3. 后台启动前端到 10002
# 4. 后台启动后端 watcher，到 10003，并在源码变更后重打包重启

# dev-down.sh
# 1. 读取 pid 文件
# 2. 优雅停止前后端
# 3. 清理 pid 文件

# dev-status.sh
# 1. 读取 pid 文件
# 2. 展示 pid、端口、日志位置和存活状态
```

- [ ] **Step 4: Run test to verify it passes**

Run: `bash scripts/test-dev-scripts.sh`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add scripts/dev-up.sh scripts/dev-down.sh scripts/dev-status.sh scripts/test-dev-scripts.sh
git commit -F /tmp/dev-runtime-task2.commit
```

### Task 3: 更新文档并做真实运行验证

**Files:**
- Modify: `README.md`
- Modify: `doclens-dashboard/README.md`

- [ ] **Step 1: Write the failing test**

```bash
if ! rg -n "./scripts/dev-up.sh|./scripts/dev-down.sh|./scripts/dev-status.sh" README.md doclens-dashboard/README.md >/dev/null; then
  echo "documentation missing helper scripts" >&2
  exit 1
fi
```

- [ ] **Step 2: Run test to verify it fails**

Run: `bash scripts/test-dev-scripts.sh`  
Expected: FAIL with `documentation missing helper scripts`

- [ ] **Step 3: Write minimal implementation**

```bash
# README.md 增加推荐开发命令与端口说明
# doclens-dashboard/README.md 同步更新代理端口说明为 10003
```

- [ ] **Step 4: Run test to verify it passes**

Run: `bash scripts/test-dev-scripts.sh`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add README.md doclens-dashboard/README.md scripts/test-dev-scripts.sh
git commit -F /tmp/dev-runtime-task3.commit
```
