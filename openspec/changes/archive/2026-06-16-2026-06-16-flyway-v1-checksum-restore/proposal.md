## Why

本地开发库 `var/db/doclens` 已经执行过 V1 Flyway 迁移。最近为了移除
`idempotency_key` 唯一约束直接改动了 `V1__doclens_ocr_schema.sql`，导致
Flyway 启动时检测到 V1 checksum mismatch，后端无法启动。

## What Changes

恢复已发布的 V1 迁移内容，保持历史 checksum 稳定。重复
`idempotency_key` 的 schema 修正继续由 V16 迁移负责，避免要求开发者删除
本地库或手动 repair。

## Impact

影响 `doclens-spring-boot-starter` 的 Flyway schema 文件与相关测试。不会改变
对外 API；开发环境应能在保留既有 H2 数据库的情况下继续启动。
