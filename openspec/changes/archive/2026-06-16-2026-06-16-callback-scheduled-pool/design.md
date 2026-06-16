## Context

页面已经能看到 `callback_jobs`，说明 OCR 完成后任务创建链路正常。
问题出在投递链路：调度任务和 HTTP 投递任务共享同一个回调线程池，
且调度任务会同步等待投递子任务，存在自阻塞风险。

## Decision

- 使用一个 `ScheduledExecutorService` 管理回调投递，核心线程数固定为 3。
- 调度器使用同一个 scheduled pool 周期扫描到期任务，不再把 worker 提交到
  单线程回调池后再等待子任务。
- callback HTTP 投递复用核心线程数为 3 的 scheduled pool；扫描任务和投递
  子任务不再受单工作线程互相等待影响。
- 第一次投递 `PENDING` job 立即执行；`RETRYING` job 只有到达
  `next_retry_at` 后才执行。
- 每次失败都会保存 `failure_reason`、`failure_detail`、`retry_count` 和
  `next_retry_at`。当投递次数达到 3 次仍失败时进入 `FAILED`，不再自动重试。
- 成功后标记 `SUCCESS`，清空失败原因和下次重试时间。
- 手动重试继续复用同一投递处理器，但对 `FAILED` job 重新开始一轮最多
  3 次的投递流程。

## Alternatives

- 仅把调度器提交到独立单线程：可解决死锁，但不满足核心线程 3 和延迟
  任务统一管理的要求。
- 保留现有双层 executor 并把回调池调大：能缓解但仍存在嵌套等待设计风险，
  后续配置变小还会复发。

## Test Plan

- 新增调度器测试，证明核心线程为 3 的 scheduled pool 能执行嵌套投递任务，
  且不会因为单工作线程嵌套等待而卡住。
- 新增/调整 worker 或 processor 测试，覆盖成功一次后不重试、失败后进入
  `RETRYING` 并设置延迟、满 3 次后进入 `FAILED`。
- 运行回调投递相关 focused tests、starter 模块相关测试、OpenSpec strict
  validation 和 diff check。
