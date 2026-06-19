# Page Task Worker Runtime Tuning Implementation Plan

**Goal:** Prevent slow OCR pages from being reset by stale-task recovery and let local page-task/OCR request concurrency match configured node capacity.

**Architecture:** Add Spring properties for the page-task worker, resolve effective runtime settings in auto-configuration, and use the same derived concurrency to size the OCR request executor when no explicit override exists.

**Tech Stack:** Java 21, Spring Boot configuration properties, JUnit 5, AssertJ

## Task 1: Runtime configuration coverage

**Files:**
- Add: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensPageTaskWorkerAutoConfigurationTest.java`
- Modify: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensSpringPropertiesBindingTest.java`

- [ ] Write failing tests for property binding and effective page-task worker settings.
- [ ] Verify RED with focused Maven test command.

## Task 2: Page-task worker runtime implementation

**Files:**
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensSpringProperties.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensPageTaskWorkerAutoConfiguration.java`

- [ ] Add `PageTaskWorkerProperties`.
- [ ] Resolve effective lock seconds, batch size, pool size, queue capacity, recovery limit, and interval.
- [ ] Use effective settings in execution options, executor creation, and scheduler creation.

## Task 3: OCR request executor default alignment

**Files:**
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensOcrThreadPoolAutoConfiguration.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensSpringProperties.java`
- Modify: `doclens-server/src/main/resources/application.yml`

- [ ] Track whether OCR request thread-pool settings were explicitly configured.
- [ ] Derive default OCR request executor size from enabled bootstrap-node max concurrency.
- [ ] Keep explicit thread-pool overrides authoritative.

## Task 4: Verification

- [ ] Run focused auto-configuration tests.
- [ ] Run relevant page-task processing/retry tests.
- [ ] Run `openspec validate 2026-06-19-page-task-worker-runtime-tuning --strict`.
