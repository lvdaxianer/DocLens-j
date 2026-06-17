## Why

DocLens-j 不做个人登录系统，但仍然需要把同一个部署里的不同调用方
隔离开来。当前产品语义已经从“用户身份”调整为“请求携带调用方凭证”，
所以请求头命名、配置模型和错误语义都需要统一到调用方凭证治理。

同时，只有凭证区分还不够。上传、OCR 触发、配置变更、Dashboard 查询
这些接口的资源成本不同，如果全部使用同一个限流额度，轻查询会被重任务
拖垮，重任务也可能被无节制请求压垮后端。Stage 2 应该补上接口分级
限流，让系统即使没有用户体系，也能按调用方保护服务稳定性。

## What Changes

新增一个独立的调用方流量治理切片：

- 将 Dashboard 本地存储键和产品术语统一为
  `X-DocLens-Credential-Key`，不再使用旧的本地存储键或内部短名。
- 服务端继续接受请求级调用方凭证，不引入用户、登录态、Session、
  用户表或 RBAC。
- 在 `application.yml` 的 `doclens.clients.credentials[*]` 下配置
  调用方凭证和 per-caller 限流覆盖项。
- 在 `application.yml` 的 `doclens.traffic` 下配置全局默认限流、
  接口组限流和全局保护开关。
- 按接口组拆分限流：`dashboard-read`、`detail-read`、`upload-write`、
  `ocr-mutation`、`config-mutation`、`admin-health`。
- 明确错误语义：缺凭证或无效凭证为 `401`，跨调用方资源为 `404`，
  调用方或接口组超额为 `429`，全局保护触发为 `503`。
- 在返回头里暴露安全的限流诊断信息，不泄露凭证原文。

## Impact

这会影响 Dashboard API 请求层、服务端凭证解析、Web 拦截链、
Spring 配置属性、默认 `application.yml` 示例、后端契约测试和前端
API 测试。

该计划不会实现个人账号系统，不会把凭证管理数据库化，也不会引入
分布式限流组件。初版以单实例内存限流为准，配置位置固定在
`application.yml`；如果后续部署为多实例，再把同一能力迁移到 Redis
或网关层。
