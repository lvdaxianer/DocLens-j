## Context

当前系统已经有 `X-Doclens-Key` 分区解析、Dashboard 请求透传和一部分
分区过滤能力，但还缺少可信网关证明、principal 到分区的授权映射、
admin 路由单独授权，以及前端对失败原因的可恢复提示。

## Goals

- `X-Doclens-Key` 只承担数据隔离职责。
- 网关负责认证，DocLens 负责验证可信 principal 是否可访问某个分区。
- 普通用户和 admin 路由分开治理。
- 前端能从后端错误里拿到稳定的恢复动作。

## Non-Goals

- 不引入个人账号体系。
- 不做面向终端用户的登录页。
- 不做通用 RBAC 平台，只覆盖当前产品需要的最小授权面。

## Task Groups

- B1-B8: 后端改造，先把分区、网关和错误边界打稳。
- F1-F7: 前端交互改造，补齐分区可见性和操作保护。

## Verification Strategy

- 每个任务先写最小 failing test。
- 后端用单测、Spring 容器测试和契约测试验证。
- 前端用组件测试、composable 测试和 API 单测验证。
- 最后跑 OpenSpec 严格校验和 plan-implementation 对照检查。
