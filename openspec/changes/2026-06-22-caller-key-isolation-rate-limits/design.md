## Context

现有实现通过 `doclens.clients.credentials` 配置 API Key / Bearer Token，
再把请求解析为 `CallerIdentity`。这适合“可信 caller 凭证”模型，但用户
已经明确选择另一种模型：`X-DocLens-Credential-Key` 只是隔离键。

DocLens 不知道这个 key 表示用户、系统、项目、知识库、部门还是会话。
它只承诺同 key 共享、不同 key 隔离，并使用限流降低伪造 key 的损失。

## Goals / Non-Goals

**Goals:**

- 使用 `X-DocLens-Credential-Key` 作为唯一 caller partition header。
- 任意非空 key 都能解析成 caller partition。
- 查询、上传、删除、重试和 Dashboard 读模型按 partition key 隔离。
- 限流按 partition key 和接口组分桶。
- 文档明确说明该 key 不是 secret，也不是认证凭证。

**Non-Goals:**

- 不做用户系统。
- 不做工作空间模型。
- 不做 RBAC 或权限矩阵。
- 不做 key allowlist、禁用、轮换或 secret 管理 UI。

## Decisions

- 前端只发送 `X-DocLens-Credential-Key`。
- 后端拦截器只读取 `X-DocLens-Credential-Key`。
- `CallerCredentialResolver` 退化为 partition key resolver：空值拒绝，
  非空值 trim 后进入 `CallerIdentity`。
- `CallerIdentity.clientId` 和 `tenantKey` 都使用 partition key；
  `sourceApp` 使用稳定值 `dashboard`，除非后续引入单独来源 header。
- 批次表沿用 `client_id/source_app/tenant_key` 字段，避免迁移扩散。
- 旧 `doclens.clients.credentials[*].rate-limits` 可以在实现阶段改为默认
  限流或保留兼容读取，但不能再表达“允许访问的凭证列表”。

## Data Flow

1. 调用方决定共享边界并传入 `X-DocLens-Credential-Key`。
2. Dashboard 原样透传该 header。
3. HTTP 拦截器解析非空 key 为 `CallerIdentity`。
4. 上传批次写入该 caller partition。
5. 查询和变更只在当前 partition 内查找资源。
6. 限流器按 partition key + traffic group 消费令牌。
7. 全局保护限制总并发，防止换 key 绕过 caller 分桶。

## Risks / Trade-offs

- 该 key 可伪造，因此它不能作为认证证据。
- 同 key 内天然共享，调用方必须自己决定共享边界。
- 如果调用方选择过粗 key，数据会混在一起；如果选择过细 key，协作会受限。
- 保留旧字段名会带来命名历史包袱，但能减少数据库迁移风险。

## Testing

- 前端 API 测试验证 raw header 透传。
- resolver 测试验证非空 key 被接受，空 key 被拒绝。
- query service 测试验证不同 key 的资源互不可见。
- rate limiter 测试验证不同 key 拥有独立桶。
- 文档检查验证不再把 isolation key 写成 secret allowlist。
