## Context

调用方凭证的产品目标是“请求级调用方区分”，不是个人账号体系。前端保存凭证
只是为了让 Dashboard 请求能自动带上 caller key；最终可信边界仍在后端的
配置 allowlist 校验。

## Goals / Non-Goals

**Goals:**
- Dashboard 使用会话级存储保存 caller key，关闭浏览器后自然失效。
- 继续使用统一键名 `X-DocLens-Credential-Key`。
- 保持现有请求头兼容：API key 和 Bearer token 都能表达。
- 保持后端 allowlist 校验、caller 解析和限流语义不变。

**Non-Goals:**
- 不引入用户登录、账号、RBAC 或凭证管理 UI。
- 不把明文 caller key 设计成安全登录凭证。
- 不改变 `doclens.clients.credentials` 的配置结构。

## Decisions

- Dashboard API helper 从 `globalThis.sessionStorage` 读取
  `X-DocLens-Credential-Key`。
- 如果 `sessionStorage` 不可用、抛错或没有值，前端不发送 caller credential
  header，让后端按现有规则返回 `401` 或处理允许匿名的非目标接口。
- 即使 `localStorage` 中存在同名新 key，Dashboard 也不读取，避免重新形成
  长期持久化入口。
- 旧 key `X-DocLens-Credential` 继续忽略。
- 后端 `CallerCredentialResolver` 和请求拦截器不改成“信任明文即可”，必须
  继续匹配配置中的 `api-key` 或 `bearer-token`。

## Data Flow

1. 使用者在浏览器会话中写入
   `sessionStorage["X-DocLens-Credential-Key"]`。
2. Dashboard API helper 读取该会话值。
3. 普通值映射为 `X-DocLens-Api-Key`；`Bearer ` 前缀值映射为
   `Authorization`。
4. 后端只接受匹配 `doclens.clients.credentials` allowlist 的凭证。
5. 解析出的 caller identity 继续驱动数据隔离和接口组限流。

## Risks / Trade-offs

- 刷新页面不会丢失凭证，但关闭浏览器会丢失，调试者需要重新设置。
- 这不是安全登录方案；浏览器内能读到的脚本仍能读到 sessionStorage。
- 后端保留校验会要求本地开发配置有效凭证，否则目标接口会继续返回 `401`。

## Testing

- 先更新 Dashboard API tests，证明 `sessionStorage` 新 key 会被读取，并确认
  同名 `localStorage` key 不再生效。
- 保留旧 key 忽略测试，防止回退到 `X-DocLens-Credential`。
- 运行前端 focused tests，先得到 RED，再改 helper 得到 GREEN。
- 运行相关后端 caller credential/traffic contract tests，确认后端 allowlist
  校验和限流语义没有被放宽。
