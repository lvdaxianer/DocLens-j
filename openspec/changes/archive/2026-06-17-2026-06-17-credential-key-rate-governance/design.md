## Context

DocLens-j 的 Stage 2 目标是区分接入方，而不是识别自然人。已有
caller attribution 能把批次归因到 `client_id`、`source_app` 和
`tenant_key`，当前正在推进的 strict caller credential change 负责
把匿名访问收紧为必须携带有效凭证。

本 change 在该基础上补齐产品治理层：统一凭证 key 命名，并为没有
用户系统的部署提供 per-caller、per-interface-group 的流量保护。

## Product Boundary

`X-DocLens-Credential-Key` 是调用方凭证，不是登录 token。它只用于：

- 解析调用方身份。
- 过滤调用方所属资源。
- 计算调用方维度的限流桶。
- 输出非敏感诊断标签。

它不用于：

- 代表个人用户。
- 创建登录态或 Session。
- 做 RBAC 权限矩阵。
- 承载业务角色或组织授权。

## Naming

最终对外命名统一为 `X-DocLens-Credential-Key`。

Dashboard 前端：

- 从 `localStorage` key `X-DocLens-Credential-Key` 读取凭证。
- 若值以 `Bearer ` 开头，则发送 `Authorization: Bearer <token>`。
- 若值不以 `Bearer ` 开头，则发送 `X-DocLens-Api-Key: <value>`。
- 若没有该 key，则不发送凭证头，由服务端返回 `401`。
- 为了平滑迁移，不再读取 `X-DocLens-Credential`，避免两个入口长期
  并存造成排查混乱。

服务端：

- 现有 `doclens.clients.credentials[*].api-key` 和
  `doclens.clients.credentials[*].bearer-token` 仍是底层匹配字段。
- 文档、测试和错误提示使用“credential key / 调用方凭证”语言，
  不使用“用户 key”或“登录”语言。

## Configuration

初版配置直接放在 `application.yml`。

建议结构：

```yaml
doclens:
  clients:
    credentials:
      - client-id: local-demo
        source-app: dashboard
        tenant-key: local
        api-key: ${DOCLENS_LOCAL_CREDENTIAL_KEY:}
        rate-limits:
          dashboard-read:
            qps: 20
            burst: 40
          detail-read:
            qps: 10
            burst: 20
          upload-write:
            qps: 1
            burst: 3
          ocr-mutation:
            qps: 0.5
            burst: 2
          config-mutation:
            qps: 0.2
            burst: 1
          admin-health:
            qps: 5
            burst: 10
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

配置优先级：

1. 调用方自己的 `credentials[*].rate-limits.<group>`。
2. `doclens.traffic.default-limits.<group>`。
3. 内置保守默认值。

## Interface Groups

初版通过 Spring MVC path + method 映射接口组。

| Group | Typical APIs | Cost | Default posture |
| --- | --- | --- | --- |
| `dashboard-read` | summary, batch list, health overview | Low | Higher QPS |
| `detail-read` | batch detail, document result, event timeline | Medium | Medium QPS |
| `upload-write` | native batch upload | High | Low QPS, small burst |
| `ocr-mutation` | retry, delete, manual recovery, OCR node mutation | High | Very low QPS |
| `config-mutation` | LLM Markdown config, OCR governance config write | High/Risky | Very low QPS |
| `admin-health` | actuator health/info and cheap service checks | Low | Small independent budget |

如果某个接口无法匹配明确分组，默认使用 `detail-read`，避免误给高额度。

## Runtime Design

新增服务端组件：

- `CredentialKeyConstants`：统一 `X-DocLens-Credential-Key`、
  `X-DocLens-Api-Key`、`Authorization`、`Bearer ` 常量。
- `TrafficGroup`：枚举接口组和默认 fallback。
- `TrafficLimitProperties`：绑定 `qps`、`burst` 和全局保护配置。
- `CallerRateLimitPolicy`：合并 caller override、default limits、
  内置默认值。
- `CallerRateLimiter`：单实例内存令牌桶，key 为
  `client_id + tenant_key + trafficGroup`。
- `TrafficGroupResolver`：根据 request method/path 得到接口组。
- `CallerTrafficInterceptor`：在 caller credential resolver 之后执行，
  获取 caller identity、计算接口组、申请令牌。
- `RateLimitExceededException`：映射为 `429`。
- `GlobalProtectionException`：映射为 `503`。

令牌桶选择：

- 初版不引第三方依赖，使用 JDK 时间和并发原语实现简单 token bucket。
- `qps` 支持小数，例如 `0.5` 表示每 2 秒 1 个令牌。
- `burst` 必须为正整数，小于 1 时按 1 处理。
- 限流状态按 caller + group 隔离，避免某个调用方的上传影响另一个
  调用方的 Dashboard 查询。

## Error Semantics

- `401 Unauthorized`：缺少 `X-DocLens-Api-Key` / `Authorization`
  或凭证无法匹配任何 `X-DocLens-Credential-Key` 配置项。
- `404 Not Found`：凭证有效，但访问了其它 caller 的批次、文档、
  回调任务或结果。
- `429 Too Many Requests`：caller 在当前接口组超过配置额度。
- `503 Service Unavailable`：全局保护触发，例如 in-flight 请求超过
  `doclens.traffic.global-protection.max-in-flight`。

`429` 响应建议包含：

- `Retry-After`
- `X-DocLens-Traffic-Group`
- `X-DocLens-RateLimit-Limit`
- `X-DocLens-RateLimit-Remaining`

响应和日志不得输出凭证原文。

## Testing

每个实现任务按 TDD 执行：

- 前端 API 测试先证明 `localStorage` 新 key 会被读取，旧 key 不再生效。
- Spring properties 测试先证明配置结构能绑定到调用方和默认限流。
- 限流器单元测试先证明 caller + group 隔离、小数 QPS、burst 和
  refill 行为。
- Server contract test 先证明不同接口组返回 `429` 的阈值不同。
- Contract test 继续覆盖 `401`、`404`、`503` 语义。

Broader verification:

- `mvn -pl doclens-spring-boot-starter -Dtest=<focused-test> test`
- `mvn -pl doclens-server -Dtest=<focused-test> test`
- `mvn test`
- `npm --prefix doclens-dashboard run test:ui`
- `npm --prefix doclens-dashboard run build`
- `openspec validate 2026-06-17-credential-key-rate-governance --strict`

## Rollout

1. 先落命名迁移，让 Dashboard 请求只读取
   `X-DocLens-Credential-Key`。
2. 再落服务端配置和限流器，但保留可通过 `doclens.traffic.enabled=false`
   关闭的本地调试开关。
3. 最后打开默认配置示例，并在 `application.yml` 写清本地凭证 key 和
   限流默认值。

生产环境建议：

- `doclens.traffic.enabled=true`
- `doclens.traffic.anonymous-enabled=false`
- 所有调用方必须配置 `api-key` 或 `bearer-token`
- 上传和 mutation 接口的 qps 明显低于查询接口
