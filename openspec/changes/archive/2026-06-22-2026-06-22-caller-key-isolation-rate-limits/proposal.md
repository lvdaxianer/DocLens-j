## Why

DocLens-j 当前 caller 设计把 `X-DocLens-Credential-Key` 的行为混在
“凭证认证”和“数据隔离”之间，并且 header 名仍绑定 DocLens 产品语义。
用户确认后的真实目标不是让 DocLens 判断这个 key 属于谁，而是让
Recall 体系调用方传入一个不透明分区键：

- 同 key 的数据可共享。
- 不同 key 的数据隔离。
- 即使 key 被伪造，也通过 caller 分桶限流和全局保护把损失压住。

如果按用户隔离，会破坏协作共享；如果按系统隔离，隔离粒度又太粗。
DocLens 本身没有工作空间概念，因此不应该引入 workspace、账号或 RBAC。

## What Changes

- Dashboard 从 `sessionStorage` 读取 `X-Recall-Key`，请求时原样发送同名
  header，不再发送 `X-DocLens-Credential-Key`、`X-DocLens-Api-Key` 或
  `Authorization` 作为 caller partition 机制。
- 后端读取 `X-Recall-Key` 作为 opaque caller partition key。
- 后端不再用 allowlist、API key 或 Bearer token 校验该 key。
- 批次归因、Dashboard 查询、OCR 查询、文档结果、事件和变更操作都按
  partition key 隔离。
- caller 级限流继续按 partition key 和接口组分桶。
- 全局保护继续作为换 key 刷请求时的兜底止损层。
- 文档和配置措辞从“credential/authenticated caller”收敛为
  “caller partition key / isolation key”。

## Impact

这是语义调整和行为调整：

- 任意非空 `X-Recall-Key` 都可进入业务路径。
- 缺失或空 key 仍会被拒绝，避免全部落到匿名共享桶。
- 不同 key 之间的资源访问表现为 not found。
- 这不是认证系统，不能证明请求者身份；安全边界来自隔离与限流止损。
