# DocLens Java DDD High Availability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild DocLens as a Java 21 + Spring Boot 3 service using DDD boundaries and an architecture that can evolve from local single-node operation to highly available deployment.

**Architecture:** Phase 1 is a modular monolith with DDD package boundaries, JDBC persistence, local object storage, and in-process workers. Phase 2 separates API, Worker, and Callback Worker processes over PostgreSQL HA, object storage, and an outbox-driven event pipeline.

**Tech Stack:** Java 21, Spring Boot 3.5.x, Maven, Spring MVC, Spring JDBC, Flyway, Actuator, H2 for tests, PostgreSQL-ready schema, local/S3-compatible storage abstraction.

---

## Phase 1 Scope

### Task 1: Create Spring Boot Project Baseline

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/doclens/DocLensApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/test/java/com/doclens/DocLensApplicationTests.java`

- [x] Add Spring Boot parent `3.5.14`, Java `21`, and dependencies for web, validation, jdbc, actuator, flyway, H2 tests, and PostgreSQL runtime.
- [x] Configure local defaults so the app can run without external services.
- [x] Add a smoke test that loads the Spring context.

### Task 2: Model DDD Boundaries

**Files:**
- Create: `src/main/java/com/doclens/ingestion/domain/*`
- Create: `src/main/java/com/doclens/processing/domain/*`
- Create: `src/main/java/com/doclens/adapter/domain/*`
- Create: `src/main/java/com/doclens/query/application/*`
- Create: `src/main/java/com/doclens/callback/domain/*`
- Create: `src/main/java/com/doclens/shared/*`

- [x] Define `Batch`, `DocumentJob`, `OcrResult`, `OcrEvent`, and adapter capability models.
- [x] Keep write-side domain models separate from query response models.
- [x] Add repository interfaces around aggregate roots instead of table-centric universal repositories.

### Task 3: Add Persistence and Storage

**Files:**
- Create: `src/main/resources/db/migration/V1__doclens_ocr_schema.sql`
- Create: `src/main/java/com/doclens/*/infrastructure/*JdbcRepository.java`
- Create: `src/main/java/com/doclens/storage/*`

- [x] Create OCR batch, document, result, event, and callback tables.
- [x] Add future HA fields such as `locked_by`, `locked_until`, and retry state.
- [x] Store JSON as text in phase 1 to keep H2 and PostgreSQL compatible.
- [x] Use `ObjectStorage` abstraction with local filesystem implementation.

### Task 4: Implement API Vertical Slice

**Files:**
- Create: `src/main/java/com/doclens/ingestion/interfaces/OcrBatchController.java`
- Create: `src/main/java/com/doclens/query/interfaces/OcrQueryController.java`
- Create: `src/main/java/com/doclens/adapter/interfaces/AdapterController.java`
- Create: `src/main/java/com/doclens/system/interfaces/HealthController.java`

- [x] Implement `POST /api/v1/batches`.
- [x] Implement `GET /api/v1/batches/{batch_id}`.
- [x] Implement `GET /api/v1/documents/{document_id}`.
- [x] Implement `GET /api/v1/documents/{document_id}/result`.
- [x] Implement `GET /api/v1/batches/{batch_id}/events`.
- [x] Implement `GET /api/v1/adapters`.
- [x] Implement `GET /api/v1/health`.

### Task 5: Implement Processing Worker Skeleton

**Files:**
- Create: `src/main/java/com/doclens/processing/application/BatchProcessingUseCase.java`
- Create: `src/main/java/com/doclens/adapter/infrastructure/StubOcrAdapter.java`

- [x] Process documents by `sort_order`.
- [x] Generate page progress events.
- [x] Persist normalized OCR result payload.
- [x] Mark document and batch final states.

### Task 6: Add Contract Tests

**Files:**
- Create: `src/test/java/com/doclens/contract/DocLensOcrApiContractTest.java`

- [x] Verify upload response contract.
- [x] Verify batch, document, result, events, adapters, and health endpoints.
- [x] Verify invalid metadata and missing files are rejected.

## Phase 2 HA Evolution

- Split deployment into `doclens-api`, `doclens-worker`, and `doclens-callback-worker`.
- Switch local storage profile to S3/MinIO object storage.
- Run PostgreSQL with HA topology and connection pooling.
- Replace in-process event dispatch with outbox relay.
- Enable multi-worker task acquisition using `FOR UPDATE SKIP LOCKED`.
- Add callback retry worker with bounded retry policy and dead-letter status.
- Add Kubernetes readiness and liveness probes through Spring Boot Actuator.
