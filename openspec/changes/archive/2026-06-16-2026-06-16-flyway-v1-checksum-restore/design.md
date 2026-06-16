## Context

Flyway 的版本化迁移文件一旦在数据库中应用，就不能再修改内容。当前开发库中
V1 checksum 是旧值，但代码中的 V1 因删除 `idempotency_key UNIQUE` 发生变化，
所以 Flyway 在创建 `sqlSessionTemplate` 之前中止，表现为 MyBatis mapper 依赖失败。

## Decision

- 恢复 V1 中 `ocr_batches.idempotency_key` 的 `UNIQUE` 定义，保持历史迁移不可变。
- 保留 V16 `DROP CONSTRAINT` 迁移，负责把新库和旧库最终都迁移到无唯一约束状态。
- 补充测试锁定 V1 原始内容，避免后续再次直接修改已发布迁移。

## Alternatives

- 删除本地 `var/db/doclens`：只能修开发者本机，不能修根因。
- 运行 Flyway repair：会掩盖已发布迁移被修改的问题，不适合作为代码库修复。
