# Permission Configuration

DocLens-j protects `/api/v1/**` with Spring MVC interceptors instead of Spring
Security, login sessions, or JWT validation. This document explains how to
configure that model in production and development.

## Permission Model

Every `/api/v1/**` request passes through these checks before reaching the
controller:

1. `GlobalProtectionInterceptor` limits total in-flight API requests.
2. `TrustedGatewayInterceptor` optionally verifies that the request came through
   a trusted gateway.
3. `CallerPartitionInterceptor` requires `X-Doclens-Key` and turns it into the
   caller data partition.
4. `CallerTrafficInterceptor` applies per-partition traffic limits.

The most important distinction is that `X-Doclens-Key` is a data partition key,
not an authentication token. Production authentication is provided by the
trusted gateway headers when `doclens.gateway-auth.enabled=true`.

## Request Headers

| Header | Required When | Description |
| --- | --- | --- |
| `X-Doclens-Gateway-Secret` | Production gateway auth is enabled | Shared proof that the request came from the trusted gateway. The value must match `doclens.gateway-auth.trusted-gateway.header-value`. |
| `X-Doclens-Principal` | Production gateway auth is enabled | Principal name injected by the gateway. It must match one configured `doclens.gateway-auth.principals[].principal`. |
| `X-Doclens-Roles` | Optional | Gateway-injected role context. Current admin authorization is based on configured principal roles, not on trusting this header alone. |
| `X-Doclens-Key` | All `/api/v1/**` requests | Opaque caller partition key used for data isolation and rate limiting. |

HTTP header names are case-insensitive, but examples use the names above to
match the implementation and dashboard code.

## Production Configuration

Production should enable trusted gateway authentication and inject identity only
after the edge gateway has authenticated the external user or system. Direct
public access to the Spring Boot service should not be exposed without that
gateway.

`doclens-server/src/main/resources/application-prod.yml` already enables
gateway auth:

```yaml
doclens:
  gateway-auth:
    enabled: true
    trusted-gateway:
      header-value: ${DOCLENS_GATEWAY_SECRET}
    principals:
      - principal: ${DOCLENS_GATEWAY_PRINCIPAL}
        roles:
          - admin
        allowed-partitions:
          - ${DOCLENS_GATEWAY_PARTITION}
```

For a single production dashboard principal, set environment variables before
starting the service:

```bash
export DOCLENS_GATEWAY_SECRET='replace-with-long-random-secret'
export DOCLENS_GATEWAY_PRINCIPAL='dashboard-admin'
export DOCLENS_GATEWAY_PARTITION='tenant-a'
```

With that configuration, the gateway must forward requests like this:

```bash
curl -sS http://localhost:10003/api/v1/dashboard/summary \
  -H 'X-Doclens-Gateway-Secret: replace-with-long-random-secret' \
  -H 'X-Doclens-Principal: dashboard-admin' \
  -H 'X-Doclens-Roles: admin' \
  -H 'X-Doclens-Key: tenant-a'
```

The request is accepted only when:

- `X-Doclens-Gateway-Secret` matches `DOCLENS_GATEWAY_SECRET`.
- `X-Doclens-Principal` is `dashboard-admin`.
- `X-Doclens-Key` is one of `dashboard-admin`'s allowed partitions.
- The route does not require `admin`, or the configured principal roles contain
  `admin`.

### Multiple Production Partitions

Use multiple principal entries when different gateway principals should access
different partitions:

```yaml
doclens:
  gateway-auth:
    enabled: true
    trusted-gateway:
      header-value: ${DOCLENS_GATEWAY_SECRET}
    principals:
      - principal: dashboard-admin
        roles:
          - admin
        allowed-partitions:
          - tenant-a
          - tenant-b
      - principal: tenant-a-viewer
        roles:
          - user
        allowed-partitions:
          - tenant-a
```

In this example, `dashboard-admin` can use `X-Doclens-Key: tenant-a` or
`tenant-b`, while `tenant-a-viewer` can only use `tenant-a`. Admin routes still
require a configured `admin` role.

### What principal roles and allowed partitions mean

Read the gateway authorization block as three separate decisions:

| Field | Meaning | Controls |
| --- | --- | --- |
| `principal` | Who the trusted gateway says is calling. | Which configured identity is used for authorization. |
| `roles` | What operation level that principal has. | Whether admin routes can be used. |
| `allowed-partitions` | Which data partitions that principal may request. | Which `X-Doclens-Key` values are accepted. |

For example:

```yaml
principals:
  - principal: company-a-user
    roles:
      - user
    allowed-partitions:
      - company-a

  - principal: platform-admin
    roles:
      - admin
    allowed-partitions:
      - company-a
      - company-b
```

This means:

- `company-a-user` can only use `X-Doclens-Key: company-a`.
- `platform-admin` can use `X-Doclens-Key: company-a` or
  `X-Doclens-Key: company-b`.
- `platform-admin` can access admin routes because its configured roles contain
  `admin`.
- `company-a-user` cannot access admin routes because `user` is not enough for
  admin route policies.

The following request is accepted for `platform-admin`:

```http
X-Doclens-Principal: platform-admin
X-Doclens-Key: company-a
```

This request is also accepted:

```http
X-Doclens-Principal: platform-admin
X-Doclens-Key: company-b
```

This request is rejected with `403 Forbidden` because `company-c` is not in
`platform-admin`'s `allowed-partitions`:

```http
X-Doclens-Principal: platform-admin
X-Doclens-Key: company-c
```

In short:

```text
principal = who is calling
roles = whether that identity can use admin operations
allowed-partitions = which isolated data spaces that identity can access
```

`roles` do not decide which tenant data can be read. Data access is bounded by
`allowed-partitions` and the requested `X-Doclens-Key`. Conversely,
`allowed-partitions` does not grant admin operations by itself.

### Admin Routes

The default admin route policies include:

- `/api/v1/ocr-governance-config/**`
- `/api/v1/llm-markdown-config/**`
- `/api/v1/ocr-models/**`
- `/api/v1/ocr-nodes/**`
- `/api/v1/dashboard/callback-jobs/*/retry`

Requests to those paths must use a principal whose configured roles include
`admin`.

## Development Configuration

The default local configuration keeps `doclens.gateway-auth.enabled=false`.
Developers do not need to provide gateway secret or principal headers, but they
still must send `X-Doclens-Key` on `/api/v1/**` requests.

Minimal local request:

```bash
curl -sS http://localhost:10003/api/v1/dashboard/summary \
  -H 'X-Doclens-Key: local-dev'
```

Local upload example:

```bash
curl -sS http://localhost:10003/api/v1/batches \
  -H 'X-Doclens-Key: local-dev' \
  -F 'files=@./sample.pdf'
```

Use different local keys to test isolation:

```bash
curl -sS http://localhost:10003/api/v1/dashboard/summary \
  -H 'X-Doclens-Key: tenant-a'

curl -sS http://localhost:10003/api/v1/dashboard/summary \
  -H 'X-Doclens-Key: tenant-b'
```

Batches created under `tenant-a` are not visible under `tenant-b`.

## Development With Gateway Auth Enabled

To test production-like behavior locally, create a local profile or environment
override:

```yaml
doclens:
  gateway-auth:
    enabled: true
    trusted-gateway:
      header-value: gateway-secret-local
    principals:
      - principal: local-admin
        roles:
          - admin
        allowed-partitions:
          - local-dev
```

Then call the API with all gateway headers:

```bash
curl -sS http://localhost:10003/api/v1/ocr-nodes \
  -H 'X-Doclens-Gateway-Secret: gateway-secret-local' \
  -H 'X-Doclens-Principal: local-admin' \
  -H 'X-Doclens-Roles: admin' \
  -H 'X-Doclens-Key: local-dev'
```

This mode is useful for validating gateway integration, admin routes, and
partition allowlists before deploying to production.

## Traffic Limits

Caller traffic limits are configured under `doclens.traffic`. The default
`application.yml` enables traffic limits and global protection:

```yaml
doclens:
  traffic:
    enabled: true
    anonymous-enabled: false
    default-limits:
      dashboard-read:
        qps: 10
        burst: 20
      detail-read:
        qps: 5
        burst: 10
      upload-write:
        qps: 0.5
        burst: 2
      ocr-mutation:
        qps: 0.2
        burst: 1
      config-mutation:
        qps: 0.1
        burst: 1
      admin-health:
        qps: 2
        burst: 5
    global-protection:
      enabled: true
      max-in-flight: 100
```

Each rate bucket is keyed by `X-Doclens-Key` plus traffic group. One partition
exhausting `upload-write` does not consume another partition's bucket.

Tests disable traffic limits in `doclens-server/src/test/resources/application.yml`
so focused application tests do not depend on token bucket timing.

## Failure Responses

| Status | Typical Cause |
| --- | --- |
| `401 Unauthorized` | Missing or invalid `X-Doclens-Key`, missing trusted gateway proof, or missing principal when gateway auth is enabled. |
| `403 Forbidden` | Principal is not configured for the requested partition, or the route requires `admin` and the configured principal does not have that role. |
| `429 Too Many Requests` | Caller partition exceeded the configured traffic limit. Response headers include `Retry-After` and `X-DocLens-Traffic-Group`. |
| `503 Service Unavailable` | Global in-flight protection rejected the request. |

## Caveats

- The current server does not implement a user login flow, JWT validation, or
  Spring Security authorization rules for these APIs.
- `X-Doclens-Key` is required for data isolation, but it is not a secret and
  should not be used as the only production authentication proof.
- The compatibility resolver for old API key or bearer-token signatures no
  longer performs the current authentication or allowlist decision.
- The default `/api/v1/** -> user` route policy is present in configuration,
  but the current interceptor only enforces policies whose required role is
  `admin`.

## Related Docs

- [Configuration](configuration.md)
- [HTTP API Reference](api.md)
- [Local Development](development.md)
