# DocLens Java DDD High Availability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild DocLens as a Java 21 + Spring Boot 3 service using DDD boundaries and an architecture that can evolve from local single-node operation to highly available deployment.

**Architecture:** Phase 1 is a modular monolith with DDD package boundaries, MyBatis-Plus persistence adapters, local object storage, and in-process workers. Phase 2 separates API, Worker, and Callback Worker processes over PostgreSQL HA, object storage, and an outbox-driven event pipeline. Phase 3 splits the codebase into SDK/API, core, Spring Boot starter, and HTTP server modules.

**Tech Stack:** Java 21, Spring Boot 3.5.x, Maven, Spring MVC, MyBatis-Plus, Flyway, Actuator, H2 for tests, PostgreSQL-ready schema, local/S3-compatible storage abstraction.

---

## Phase 1 Scope

### Task 1: Create Spring Boot Project Baseline

**Files:**
- Create: `pom.xml`
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/DocLensApplication.java`
- Create: `doclens-server/src/main/resources/application.yml`
- Create: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/DocLensApplicationTests.java`

- [x] Add Spring Boot parent `3.5.14`, Java `21`, and dependencies for web, validation, MyBatis-Plus, actuator, flyway, H2 tests, and PostgreSQL runtime.
- [x] Configure local defaults so the app can run without external services.
- [x] Add a smoke test that loads the Spring context.

### Task 2: Model DDD Boundaries

**Files:**
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/domain/*`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/*`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/*`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/query/application/*`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/shared/*`

- [x] Define `Batch`, `DocumentJob`, `OcrResult`, `OcrEvent`, and adapter capability models.
- [x] Keep write-side domain models separate from query response models.
- [x] Add repository interfaces around aggregate roots instead of table-centric universal repositories.

### Task 3: Add Persistence and Storage

**Files:**
- Create: `doclens-spring-boot-starter/src/main/resources/db/migration/V1__doclens_ocr_schema.sql`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/*/infrastructure/*Entity.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/*/infrastructure/*Mapper.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/*/infrastructure/*MybatisPlusRepository.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/storage/*`

- [x] Create OCR batch, document, result, event, and callback tables.
- [x] Add future HA fields such as `locked_by`, `locked_until`, and retry state.
- [x] Store JSON as text in phase 1 to keep H2 and PostgreSQL compatible.
- [x] Keep MyBatis-Plus inside infrastructure adapters so domain and application layers depend only on repository interfaces.
- [x] Use `ObjectStorage` abstraction with local filesystem implementation.

### Task 4: Implement API Vertical Slice

**Files:**
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/interfaces/OcrBatchController.java`
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/query/interfaces/OcrQueryController.java`
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/AdapterController.java`
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/system/interfaces/HealthController.java`

- [x] Implement `POST /api/v1/batches`.
- [x] Implement `GET /api/v1/batches/{batch_id}`.
- [x] Implement `GET /api/v1/documents/{document_id}`.
- [x] Implement `GET /api/v1/documents/{document_id}/result`.
- [x] Implement `GET /api/v1/batches/{batch_id}/events`.
- [x] Implement `GET /api/v1/adapters`.
- [x] Implement `GET /api/v1/health`.

### Task 5: Implement Processing Worker Skeleton

**Files:**
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCase.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/StubOcrAdapter.java`

- [x] Process documents by `sort_order`.
- [x] Generate page progress events.
- [x] Persist normalized OCR result payload.
- [x] Mark document and batch final states.

### Task 6: Add Contract Tests

**Files:**
- Create: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/DocLensOcrApiContractTest.java`

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

## Phase 3 SDK and HTTP Dual Delivery

**Current modules:**
- `doclens-api`: stable embedded Jar contract, including `DocLensEngine`, request DTOs, document input DTOs, and event sink SPI.
- `doclens-core`: DDD domain model, application use cases, query service, and default engine implementation without Spring, Web, Servlet, or MyBatis dependencies.
- `doclens-spring-boot-starter`: Spring Boot auto-configuration, MyBatis-Plus persistence adapters, Flyway schema, transaction adapter, local object storage, and default OCR adapter.
- `doclens-server`: HTTP service entrypoint and thin REST controllers that call `DocLensEngine`.

**Published coordinates and package namespace:**
- Maven group id: `io.github.lvdaxianer`
- Parent artifact id: `doclens-j`
- Java package root: `io.github.lvdaxianer.doclens.j`

**Integration modes:**
- Embedded SDK mode: host applications depend on `doclens-api` and `doclens-core`, then provide repository, storage, OCR adapter, transaction, and event sink implementations.
- Spring embedded mode: host Spring Boot applications depend on `doclens-spring-boot-starter` and inject `DocLensEngine`.
- HTTP service mode: deploy `doclens-server`, which exposes `/api/v1/**` while reusing the same engine and starter infrastructure.

**Guardrails:**
- `doclens-core` must not import Spring, Servlet/Jakarta Web, MyBatis, or MyBatis-Plus types.
- HTTP controllers must stay adapter-thin and enter business capabilities through `DocLensEngine`.
- MyBatis-Plus entities, mappers, and repositories must remain in `doclens-spring-boot-starter`.
