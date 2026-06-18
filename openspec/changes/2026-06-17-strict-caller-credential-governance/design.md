## Context

当前实现已经有 caller 归因模型和凭证解析器，但解析器在未配置
凭证时会回退到匿名 caller，Dashboard 端也没有统一的请求凭证注入。
结果就是同一个实例里的所有数据都能被浏览器直接看到。

## Goals / Non-Goals

**Goals:**
- 所有面向外部的 DocLens-j 接口都要求 caller 凭证。
- 查询结果只返回所属 caller 的数据。
- Dashboard 不引入个人登录、用户表或 RBAC。
- Dashboard 继续使用 API key / Bearer token 这类请求凭证。

**Non-Goals:**
- 不做账号体系。
- 不做权限矩阵或组织级授权。
- 不做凭证管理 UI。

## Decisions

- `CallerCredentialResolver` 不再允许匿名回退；没有可匹配凭证时直接拒绝。
- 上传入口继续通过 `X-DocLens-Api-Key` / `Authorization: Bearer ...`
  解析 caller。
- Dashboard 和 OCR 查询入口在 controller 层解析同一套 caller 凭证，
  再把 caller 传入查询和操作用例。
- 查询服务以 caller 为边界过滤列表和详情；查不到所属数据时按
  “不存在” 处理，避免暴露别的 caller 的资源存在性。
- Dashboard 前端从环境变量读取当前 caller 凭证，并在所有 API 请求里
  自动附加对应请求头。
- `scripts/run-backend-dev.sh` 在本地开发启动时为
  `DOCLENS_LOCAL_CREDENTIAL_KEY` 提供一个默认复杂 key，但使用
  shell 的 `${VAR:-default}` 形式保留外部覆盖能力。

## Data Flow

1. 浏览器或外部调用方发送请求凭证。
2. HTTP 层把请求头解析成 `CallerIdentity`。
3. 查询/操作层使用 caller 过滤批次、文档、事件和回调任务。
4. 前端只渲染 caller 自己的汇总、列表和详情。

## Risks / Trade-offs

- 这会让未配置凭证的本地环境全部返回 401，因此本地 runner 需要
  提供默认 key，避免普通 `./scripts/dev-restart.sh` 直接进入 401。
- 目前没有做凭证输入 UI，所以前端凭证仍依赖环境变量注入。
- 对外观测上会更安静，但也会让越权访问看起来像“资源不存在”。

## Testing

- 先用测试锁定匿名回退被移除。
- 再用服务测试锁定 caller-scoped 查询和操作行为。
- 最后验证 Dashboard API 客户端会把 caller 头带到所有请求里。
