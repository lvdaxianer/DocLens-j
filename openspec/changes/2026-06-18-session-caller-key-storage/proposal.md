## Why

DocLens-j Dashboard 当前会从浏览器持久化存储读取调用方凭证。这个凭证
用于区分接入方，不是个人登录态；继续放在 `localStorage` 会让关闭浏览器后
凭证仍长期保留，不符合“临时输入、会话内使用”的产品预期。

同时，后端 caller key 校验仍然有必要。即使请求凭证是明文，它仍用于把请求
映射到已配置的调用方、租户和限流预算，不能退回到匿名访问或只由前端决定
caller 身份。

## What Changes

- Dashboard 只从 `sessionStorage` key `X-DocLens-Credential-Key` 读取当前
  caller key。
- Dashboard 不再从 `localStorage` 读取 `X-DocLens-Credential-Key` 或旧的
  `X-DocLens-Credential`。
- Header 映射规则保持不变：普通值发送为 `X-DocLens-Api-Key`，`Bearer `
  开头的值发送为 `Authorization`。
- 后端继续通过 `doclens.clients.credentials[*].api-key` 和
  `doclens.clients.credentials[*].bearer-token` 校验凭证；缺失或无效凭证
  仍返回 `401`。

## Impact

关闭浏览器后 Dashboard caller key 会随会话存储一起消失。使用者需要在新会话
重新写入 `sessionStorage["X-DocLens-Credential-Key"]`。后端 API 行为不放宽：
没有匹配配置凭证的请求不会被接受。
