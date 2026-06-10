# Online OCR Execution Permission Health Check Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ensure online OCR nodes are considered healthy only when they can actually execute the configured provider model, not merely when their channel, model, and API key fields are populated.

**Architecture:** Keep the existing periodic OCR health-check pipeline, but move online-node health from static configuration validation to a real execution-permission probe. Reuse the existing DashScope integration path so the same credential and provider-model semantics are exercised during health checks and routing.

**Tech Stack:** Java 21, Spring Boot 3.5, Maven, Jackson, Java `HttpClient`, JUnit 5, AssertJ.

---

## Confirmed Requirements

- 在线 OCR 节点不能只以“配置完整”视为健康。
- 在线 OCR 节点健康检查必须判断当前渠道、模型和密钥是否具备实际执行权限。
- 无执行权限时节点必须在健康检查后被摘除，不能继续参与 OCR 路由。
- 失败错误信息仍需脱敏，不能泄露 API Key。

## File Structure

- Modify: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrHealthCheckerTest.java`
  - 先把在线节点“仅配置即可健康”的旧断言改成权限探测语义，并补无权限失败用例。
- Modify: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/DashScopeOnlineOcrClientTest.java`
  - 为在线 OCR 客户端补权限探测测试，覆盖成功与 403 无权限失败。
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/DashScopeOnlineOcrClient.java`
  - 增加最小权限探测入口，复用现有 endpoint、模型、凭证和脱敏错误逻辑。
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrHealthChecker.java`
  - 在线节点健康检查改为真实权限探测，不再只检查字段存在。
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensOcrResourceAutoConfiguration.java`
  - 如有必要，补充健康检查依赖注入，保证在线探测器可被健康检查复用。

## Task 1: 在线 OCR 健康检查校验执行权限

**Files:**
- Modify: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrHealthCheckerTest.java`
- Modify: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/DashScopeOnlineOcrClientTest.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/DashScopeOnlineOcrClient.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrHealthChecker.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensOcrResourceAutoConfiguration.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void onlineNodeHealthCheckRequiresExecutionPermission() {
    TestContext context = context(onlineNode("node-online"));
    context.healthClient.fail("node-online");

    context.checker.checkOnce();

    assertThat(context.healthClient.checkedNodeIds).containsExactly("node-online");
    assertThat(context.repository.findById("node-online")).get().extracting(OcrNode::status)
            .isEqualTo(OcrNodeStatus.DOWN);
}
```

```java
@Test
void probesOnlineExecutionPermissionThroughDashScopeCompatibleApi() throws IOException {
    HttpServer server = startServer(exchange -> writeResponse(exchange, 403,
            "{\"error\":{\"message\":\"Model access denied.\",\"code\":\"Model.AccessDenied\"}}"));

    try {
        DashScopeOnlineOcrClient client = new DashScopeOnlineOcrClient(objectMapper, endpointFor(server),
                Duration.ofSeconds(5));

        assertThat(client.hasExecutionPermission(new OcrRuntimeNode(onlineNode()))).isFalse();
    } finally {
        server.stop(0);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl doclens-spring-boot-starter -am -Dtest=OcrHealthCheckerTest,DashScopeOnlineOcrClientTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: FAIL because the online health check still treats configuration-complete nodes as healthy and `DashScopeOnlineOcrClient` does not yet expose a permission probe.

- [ ] **Step 3: Write minimal implementation**

```java
private boolean isHealthy(OcrNode node) {
    return healthClient.isHealthy(node);
}
```

```java
boolean hasExecutionPermission(OcrRuntimeNode node) {
    try {
        HttpResponse<String> response = httpClient.send(buildPermissionProbeRequest(node),
                HttpResponse.BodyHandlers.ofString());
        return response.statusCode() >= 200 && response.statusCode() < 300;
    } catch (IOException ex) {
        return false;
    } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        return false;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl doclens-spring-boot-starter -am -Dtest=OcrHealthCheckerTest,DashScopeOnlineOcrClientTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/plans/2026-06-10-online-ocr-permission-health-check.md \
  doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrHealthCheckerTest.java \
  doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/DashScopeOnlineOcrClientTest.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/DashScopeOnlineOcrClient.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrHealthChecker.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensOcrResourceAutoConfiguration.java
git commit -m "fix(ocr): 校验在线节点执行权限"
```
