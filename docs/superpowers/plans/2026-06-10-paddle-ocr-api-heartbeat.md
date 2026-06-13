# Paddle OCR API Heartbeat Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Use the native PaddleOCR `/ocr` JSON/Base64 API as the offline OCR node heartbeat probe.

**Architecture:** `PaddleOcrHealthClient` should perform a minimal POST request to the node OCR endpoint and validate the native PaddleOCR success payload. Existing `OcrHealthChecker` state transitions stay unchanged so DOWN nodes still pass through RECOVERING before UP.

**Tech Stack:** Java 17, JUnit 5, AssertJ, JDK `HttpServer`, JDK `HttpClient`, Jackson.

---

### Task 1: PaddleOCR Native API Health Probe

**Files:**
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrHealthClient.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrHealthClientTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void healthCheckPostsTinyJsonImageToNativeOcrEndpoint() throws IOException {
    CapturedRequest captured = new CapturedRequest();
    HttpServer server = startServer(exchange -> {
        captured.method = exchange.getRequestMethod();
        captured.path = exchange.getRequestURI().getPath();
        captured.body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        writeResponse(exchange, 200, "{\"errorCode\": 0, \"errorMsg\": \"Success\", \"rec_texts\": [\"ok\"]}");
    });
    try {
        boolean healthy = new PaddleOcrHealthClient(TEST_TIMEOUT_SECONDS).isHealthy(nodeFor(server));

        assertThat(healthy).isTrue();
        assertThat(captured.method).isEqualTo("POST");
        assertThat(captured.path).isEqualTo("/ocr");
        assertRequestBody(captured.body);
    } finally {
        server.stop(0);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl doclens-spring-boot-starter -am -Dtest=PaddleOcrHealthClientTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: FAIL because the current health client sends `GET /health`, not `POST /ocr`.

- [ ] **Step 3: Write minimal implementation**

Update `PaddleOcrHealthClient` to:
- Build `http://{host}:{port}/ocr`.
- POST JSON with a tiny embedded PNG as Base64, `fileType: 1`, and `visualize: false`.
- Return healthy only when HTTP status is 2xx and the native payload reports `errorCode == 0`.
- Log only summarized response context and not the Base64 payload.

- [ ] **Step 4: Run focused tests**

Run: `mvn -pl doclens-spring-boot-starter -am -Dtest=PaddleOcrHealthClientTest,OcrHealthCheckerTest,OcrHealthCheckerLifecycleTest,OcrNodeHeartbeatProbeTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: PASS.

- [ ] **Step 5: Run broader verification**

Run: `mvn -pl doclens-spring-boot-starter -am test`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add docs/superpowers/plans/2026-06-10-paddle-ocr-api-heartbeat.md \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrHealthClient.java \
  doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrHealthClientTest.java
git commit -m "fix(ocr): 使用原生接口检测节点心跳"
```
