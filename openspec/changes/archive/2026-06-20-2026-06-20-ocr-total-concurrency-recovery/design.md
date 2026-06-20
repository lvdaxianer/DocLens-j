## Context

The restart symptom is that only one large PDF visibly advances while multiple
documents remain in `OCR_QUEUED`. Code inspection shows two relevant bottlenecks:

- `DocLensPageTaskWorkerAutoConfiguration.derivedNodeConcurrency()` currently
  chooses the maximum single enabled node concurrency instead of total cluster
  capacity.
- `doclens-server/src/main/resources/application.yml` explicitly sets
  `doclens.page-task-worker.pool-size` and `batch-size` to ten, so even if three
  nodes exist, local page dispatch remains capped at ten.
- `MybatisPlusDocumentPageTaskRepository.listQueued()` orders only by
  `created_at, task_id`, so a large document that created many page tasks first
  can dominate each worker scan.

## Goals / Non-Goals

**Goals:**
- Make default local OCR dispatch capacity match the sum of enabled OCR nodes
  that participate in global routing.
- Preserve explicit operator overrides for deployments that intentionally cap
  local worker capacity.
- Distribute queued page-task selection across documents within each scan.
- Add tests for aggregate concurrency derivation and fair page-task selection.

**Non-Goals:**
- Do not change OCR routing score math, health-check behavior, or manual node
  recovery.
- Do not add new database columns or migrations.
- Do not change page result aggregation order.

## Decisions

- Replace maximum-node default concurrency with aggregate node capacity:
  `sum(maxConcurrency)` for enabled nodes participating in global routing.
- Fall back to the existing default when no globally participating nodes are
  enabled.
- Remove the server's explicit page-task worker `pool-size` and `batch-size`
  defaults so auto-configuration can follow aggregate node capacity.
- Keep explicit `doclens.page-task-worker.pool-size` and `batch-size` behavior:
  positive configured values still win.
- Implement fair selection in the repository layer by over-fetching queued tasks
  and performing in-memory round-robin by document. This avoids database-specific
  window functions and keeps H2/PostgreSQL compatibility.

## Data Flow

1. Spring binds bootstrap OCR nodes and optional page-task worker settings.
2. Auto-configuration computes aggregate local OCR concurrency from enabled,
   globally participating nodes.
3. Page-task worker scans the queue using the effective batch size.
4. Repository returns a fair batch across document IDs.
5. Worker claims and executes the returned tasks, while aggregation still reads
   pages by page number when producing final text.

## Risks / Trade-offs

- Increasing default local concurrency can increase OCR cluster load. This is
  intentional because the cluster capacity is already declared by node
  `max-concurrency`; operators can still override worker settings.
- Repository over-fetching reads more queued rows per scan. The multiplier will
  be bounded by a small constant to keep database load predictable.
- Fair selection may process later pages from small documents earlier than older
  pages from a large PDF. Final document output remains ordered by page number.

## Testing

- Add auto-configuration tests for aggregate node concurrency and explicit
  worker overrides.
- Add repository tests proving `listQueued(limit)` returns tasks across
  documents instead of only the oldest large document.
- Run focused starter/core test classes for page-task worker settings and page
  task repository behavior.
