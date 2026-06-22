# Caller Key Isolation And Rate Limits Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Treat `X-Doclens-Key` as an opaque caller partition key for data isolation and traffic shaping, while removing backend ownership checks for that key and keeping the existing stop-loss layers.

**Architecture:** The Dashboard forwards one raw caller key on every request. The server reads that key as an opaque partition key, stores and queries batch data with it, and feeds it into caller-scoped rate limiting plus global protection. There is no backend key ownership lookup or user-account layer; the only trust boundary is the per-key isolation boundary and service-side limits.

**Tech Stack:** Java 21, Spring Boot 3.5, Vue 3, TypeScript, Maven, JUnit 5, Vitest.

---

## Confirmed Requirements

- `X-Doclens-Key` is the caller header the Dashboard sends.
- Backend must not compare the key against a configured ownership registry.
- Different keys must isolate batch, document, query, and dashboard visibility.
- Different keys must also land in separate traffic-limit buckets.
- Missing or blank key handling must be explicit and test-covered.
- Global protection remains enabled as a second stop-loss layer.
- No RBAC, user table, or login UI is introduced.

## File Structure

- Modify: `doclens-dashboard/src/api/callerCredential.ts`
  - Stop translating the stored key into legacy auth headers; forward the raw key as `X-Doclens-Key`.
- Modify: `doclens-dashboard/src/api/__tests__/dashboard.test.ts`
  - Prove the Dashboard client keeps emitting the raw caller key header and no legacy auth headers.
- Modify: `doclens-dashboard/src/api/__tests__/upload.test.ts`
  - Prove upload requests use the same raw caller key header.
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/shared/web/CallerCredentialInterceptor.java`
  - Read `X-Doclens-Key` and treat it as the partition key input.
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/infrastructure/CallerCredentialResolver.java`
  - Remove key ownership matching and map any non-blank raw key to a trusted in-process caller partition.
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/domain/CallerIdentity.java`
  - Clarify that the stored identity fields now carry caller partition values, not proven user identity.
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/shared/infrastructure/CallerTrafficRateLimiter.java`
  - Keep the bucket key derived from the resolved caller partition and traffic group.
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/query/application/OcrQueryService.java`
  - Keep every read path caller-scoped and make foreign resources look not found.
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/infrastructure/MybatisPlusBatchRepository.java`
  - Persist and query by the caller partition key that comes from `X-Doclens-Key`.
- Modify: `doclens-server/src/main/resources/application.yml`
  - Remove wording that implies caller key ownership checks and keep the sample configuration focused on partitioning and rate limits.
- Modify: `docs/configuration.md`
  - Document the new isolation-key semantics and the fact that the backend does not treat the key as identity proof.

## Task 1: Make the Dashboard send the raw caller key

**Files:**
- Modify: `doclens-dashboard/src/api/callerCredential.ts`
- Modify: `doclens-dashboard/src/api/__tests__/dashboard.test.ts`
- Modify: `doclens-dashboard/src/api/__tests__/upload.test.ts`

- [ ] **Step 1: Write the failing test**

```ts
it('attaches X-Doclens-Key without translating the value', async () => {
  sessionStorage.setItem('X-Doclens-Key', 'tenant-a')

  const headers = withCallerCredentialHeaders({})

  expect(headers).toEqual({ 'X-Doclens-Key': 'tenant-a' })
  expect(headers).not.toHaveProperty('X-DocLens-Api-Key')
  expect(headers).not.toHaveProperty('Authorization')
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd doclens-dashboard && npm run test:ui -- src/api/__tests__/dashboard.test.ts src/api/__tests__/upload.test.ts`

Expected: FAIL because the client still maps the stored value to `X-DocLens-Api-Key` or `Authorization`.

- [ ] **Step 3: Write minimal implementation**

```ts
const CALLER_CREDENTIAL_STORAGE_KEY = 'X-Doclens-Key'
const CALLER_HEADER = 'X-Doclens-Key'

function callerCredentialHeaders(): Record<string, string> {
  const credential = readCallerCredential()
  return credential ? { [CALLER_HEADER]: credential } : {}
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd doclens-dashboard && npm run test:ui -- src/api/__tests__/dashboard.test.ts src/api/__tests__/upload.test.ts`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add doclens-dashboard/src/api/callerCredential.ts \
  doclens-dashboard/src/api/__tests__/dashboard.test.ts \
  doclens-dashboard/src/api/__tests__/upload.test.ts
git commit -m "📝 docs: raw caller key header for dashboard"
```

## Task 2: Rebase server request resolution onto the raw key

**Files:**
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/shared/web/CallerCredentialInterceptor.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/infrastructure/CallerCredentialResolver.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/domain/CallerIdentity.java`
- Modify: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/ingestion/infrastructure/CallerCredentialResolverTest.java`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/shared/web/CallerIdentityRequestResolver.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void resolvesAnyNonBlankRawKeyAsPartitionKey() {
    CallerIdentity caller = resolver().resolve("tenant-a");

    assertThat(caller.clientId()).isEqualTo("tenant-a");
    assertThat(caller.sourceApp()).isEqualTo("dashboard");
    assertThat(caller.tenantKey()).contains("tenant-a");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl doclens-spring-boot-starter -am -Dtest=CallerCredentialResolverTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: FAIL because the resolver still requires a configured ownership entry.

- [ ] **Step 3: Write minimal implementation**

```java
public CallerIdentity resolve(String rawKey) {
    if (!StringUtils.hasText(rawKey)) {
        throw new CallerCredentialException("missing caller key");
    } else {
        String callerKey = rawKey.trim();
        return new CallerIdentity(callerKey, "dashboard", Optional.of(callerKey));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl doclens-spring-boot-starter -am -Dtest=CallerCredentialResolverTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/shared/web/CallerCredentialInterceptor.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/infrastructure/CallerCredentialResolver.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/domain/CallerIdentity.java \
  doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/ingestion/infrastructure/CallerCredentialResolverTest.java \
  doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/shared/web/CallerIdentityRequestResolver.java
git commit -m "✨ feat(auth): use raw caller key as partition key"
```

## Task 3: Keep data isolation caller-scoped

**Files:**
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/infrastructure/MybatisPlusBatchRepository.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/query/application/OcrQueryService.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/query/application/DashboardQueryService.java`
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/query/application/OcrQueryServiceTest.java`
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/query/application/DashboardQueryServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void foreignBatchStillLooksNotFoundForAnotherPartitionKey() {
    CallerIdentity callerA = new CallerIdentity("tenant-a", "dashboard", Optional.of("tenant-a"));
    CallerIdentity callerB = new CallerIdentity("tenant-b", "dashboard", Optional.of("tenant-b"));
    repository.save(batchFor(callerA, "batch-a"));

    assertThatThrownBy(() -> service.getBatch(callerB, "batch-a"))
            .isInstanceOf(ResourceNotFoundException.class);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl doclens-core -am -Dtest=OcrQueryServiceTest,DashboardQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: FAIL because the current query path still assumes a trusted caller identity model.

- [ ] **Step 3: Write minimal implementation**

```java
private Batch findCallerBatch(CallerIdentity caller, String batchId) {
    return batchRepository.findByIdForCaller(caller, batchId)
            .orElseThrow(() -> new ResourceNotFoundException("batch " + batchId + " not found"));
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl doclens-core -am -Dtest=OcrQueryServiceTest,DashboardQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/infrastructure/MybatisPlusBatchRepository.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/query/application/OcrQueryService.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/query/application/DashboardQueryService.java \
  doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/query/application/OcrQueryServiceTest.java \
  doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/query/application/DashboardQueryServiceTest.java
git commit -m "🐛 fix(query): isolate reads by caller key"
```

## Task 4: Preserve stop-loss rate limiting

**Files:**
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/shared/infrastructure/CallerTrafficRateLimiter.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/shared/infrastructure/CallerTrafficLimitPolicy.java`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/shared/web/CallerTrafficInterceptor.java`
- Modify: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/shared/infrastructure/CallerTrafficRateLimiterTest.java`
- Modify: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/shared/web/CallerTrafficInterceptorTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void distinctPartitionKeysMustNotShareTheSameBucket() {
    CallerIdentity callerA = new CallerIdentity("tenant-a", "dashboard", Optional.of("tenant-a"));
    CallerIdentity callerB = new CallerIdentity("tenant-b", "dashboard", Optional.of("tenant-b"));
    RateLimitProperties limit = new RateLimitProperties(1.0D, 1);

    assertThat(limiter.tryAcquire(callerA, "detail-read", limit)).isTrue();
    assertThat(limiter.tryAcquire(callerB, "detail-read", limit)).isTrue();
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl doclens-spring-boot-starter -am -Dtest=CallerTrafficRateLimiterTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: FAIL if the bucket key still collapses isolation keys into one shared bucket.

- [ ] **Step 3: Write minimal implementation**

```java
private String bucketKey(CallerIdentity callerIdentity, String trafficGroup) {
    return String.join("|", callerIdentity.clientId(), callerIdentity.tenantKey().orElse(""), trafficGroup);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl doclens-spring-boot-starter -am -Dtest=CallerTrafficRateLimiterTest,CallerTrafficInterceptorTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/shared/infrastructure/CallerTrafficRateLimiter.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/shared/infrastructure/CallerTrafficLimitPolicy.java \
  doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/shared/web/CallerTrafficInterceptor.java \
  doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/shared/infrastructure/CallerTrafficRateLimiterTest.java \
  doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/shared/web/CallerTrafficInterceptorTest.java
git commit -m "⚡️ perf(rate-limit): isolate buckets by caller key"
```

## Task 5: Update docs and config wording

**Files:**
- Modify: `doclens-server/src/main/resources/application.yml`
- Modify: `docs/configuration.md`
- Modify: `docs/product-roadmap.md`
- Modify: `docs/superpowers/plans/2026-06-22-caller-key-isolation-and-rate-limits.md`

- [ ] **Step 1: Write the failing test**

```bash
rg -n "X-DocLens-Credential-Key|X-DocLens-Api-Key|Authorization|API Key|Bearer Token|鉴权|凭证|密钥|白名单" docs/configuration.md docs/product-roadmap.md doclens-server/src/main/resources/application.yml
```

- [ ] **Step 2: Run test to verify it fails**

Run: `rg -n "X-DocLens-Credential-Key|X-DocLens-Api-Key|Authorization|API Key|Bearer Token|鉴权|凭证|密钥|白名单" docs/configuration.md docs/product-roadmap.md doclens-server/src/main/resources/application.yml`

Expected: FAIL because at least one file still uses the old security wording.

- [ ] **Step 3: Write minimal implementation**

```md
- `X-Doclens-Key` is a caller partition key, not an identity proof.
- The backend does not compare it against a stored ownership list.
- Isolation comes from partitioned reads plus caller-scoped rate limits.
```

- [ ] **Step 4: Run test to verify it passes**

Run: `rg -n "X-DocLens-Credential-Key|X-DocLens-Api-Key|Authorization|API Key|Bearer Token|鉴权|凭证|密钥|白名单" docs/configuration.md docs/product-roadmap.md doclens-server/src/main/resources/application.yml`

Expected: PASS only for the new partition-key wording or no matches in the touched sections.

- [ ] **Step 5: Commit**

```bash
git add doclens-server/src/main/resources/application.yml \
  docs/configuration.md \
  docs/product-roadmap.md \
  docs/superpowers/plans/2026-06-22-caller-key-isolation-and-rate-limits.md
git commit -m "📝 docs: describe caller key as partition key"
```

## Self-Review Checklist

- Every task has a specific file list.
- Every test step names a concrete command and an expected failure or pass.
- The plan does not ask for RBAC, user accounts, or a key ownership list.
- The plan keeps the isolation key and the limiters aligned.
- The plan leaves the Dashboard on the raw `X-Doclens-Key` header.
