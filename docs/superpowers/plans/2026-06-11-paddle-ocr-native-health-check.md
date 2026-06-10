# PaddleOCR Native Health Check Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ensure PaddleOCR offline nodes recover through the native `/ocr` JSON/Base64 API while online OCR nodes keep their own permission health check.

**Architecture:** `RoutingOcrHealthClient` remains the protocol boundary: offline nodes use `PaddleOcrHealthClient`, online nodes use `DashScopeOnlineOcrClient`. `PaddleOcrHealthClient` sends the same minimal JSON/Base64 request as the verified native PaddleOCR API and treats only `errorCode == 0` as healthy.

**Tech Stack:** Java 17, Spring Boot auto-configuration, JUnit 5, AssertJ, Vue/Vite dashboard utility tests.

---

### Task 1: Lock PaddleOCR Native Probe Contract

**Files:**
- Modify: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrHealthClientTest.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrHealthClient.java`

- [x] **Step 1: Write the failing test**

Add a test that starts a local `/ocr` server and asserts the client sends:
`POST /ocr`, `Content-Type: application/json`, the fixed 1x1 PNG Base64 `file`,
`fileType=1`, and `visualize=false`.

- [x] **Step 2: Run test to verify it fails**

Run:
`mvn -pl doclens-spring-boot-starter -am -Dtest=PaddleOcrHealthClientTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected before implementation: FAIL because old code calls `/health` with GET.

- [x] **Step 3: Write minimal implementation**

Change `PaddleOcrHealthClient` to build `http://host:port/ocr`, send the JSON/Base64 probe body, and parse JSON response `errorCode`.

- [x] **Step 4: Run test to verify it passes**

Run:
`mvn -pl doclens-spring-boot-starter -am -Dtest=PaddleOcrHealthClientTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected after implementation: PASS.

### Task 2: Preserve Online OCR Health Route Boundary

**Files:**
- Create: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/RoutingOcrHealthClientTest.java`
- Verify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/RoutingOcrHealthClient.java`

- [x] **Step 1: Write the failing test**

Add tests proving online nodes do not call the offline PaddleOCR client and offline nodes do not call the online permission probe.

- [x] **Step 2: Run test to verify behavior**

Run:
`mvn -pl doclens-spring-boot-starter -am -Dtest=RoutingOcrHealthClientTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: PASS if the existing route boundary is already correct; FAIL if a regression routes online nodes to PaddleOCR.

- [x] **Step 3: Keep implementation minimal**

If the test fails, update only `RoutingOcrHealthClient` routing conditions. If it passes, do not change production code.

- [x] **Step 4: Run broader OCR health tests**

Run:
`mvn -pl doclens-spring-boot-starter -am -Dtest=PaddleOcrHealthClientTest,RoutingOcrHealthClientTest,PaddleOcrNativeClientTest,OcrHealthCheckerLifecycleTest,OcrHealthCheckerTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: PASS.

### Task 3: Runtime And Commit Gate

**Files:**
- Verify: `doclens-server/src/main/resources/static/dashboard/index.html`
- Verify: `doclens-server/src/main/resources/static/dashboard/assets/*.js`

- [x] **Step 1: Build frontend and server artifact**

Run:
`npm --prefix doclens-dashboard run test:utils -- ocrDisplayRules`
`npm --prefix doclens-dashboard run build`
`mvn -pl doclens-server -am package -DskipTests`

Expected: all commands complete successfully; Vite chunk warnings are acceptable.

- [ ] **Step 2: Restart backend and verify live API**

Run the backend on port `10003`, then verify:
`curl http://127.0.0.1:10003/api/v1/health`
`curl http://127.0.0.1:10003/api/v1/ocr-models`

Expected: health is `ok`, PaddleOCR model reports `health_path=/ocr`, and offline PaddleOCR nodes can recover when their native API returns `errorCode == 0`.

- [ ] **Step 3: Apply code-review-spec**

Compare the full diff against `/Users/lvdaxianer/.claude/skills/code-review-spec/SKILL.md`, `spec.md`, and relevant `references/*`.

- [ ] **Step 4: Commit**

Create one atomic Chinese Conventional Commit:
`fix(ocr): 修复 PaddleOCR 原生心跳探测`
