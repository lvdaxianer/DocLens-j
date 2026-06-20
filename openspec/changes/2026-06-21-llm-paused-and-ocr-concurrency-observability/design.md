## Context

The current LLM selector returns an empty selection when no enabled and complete
config exists. The post-processor then emits `no_available_llm_config`, which the
dashboard renders as `LLM 未配置`. This does not distinguish an empty system from
one where configs exist but have been intentionally paused.

For OCR, the page-task worker creates a fixed worker pool and the OCR request
executor creates another pool. The current dashboard metrics expose active count,
queue size, and runtime-created pool size, but not core or maximum configured
capacity. As a result, a capacity of 30 can be invisible when only a few threads
have been created so far.

## Goals / Non-Goals

**Goals:**
- Show `LLM 已暂停` when configs exist for Markdown post-processing but none are
  enabled for execution.
- Keep `LLM 排版失败` reserved for calls that were actually attempted and failed.
- Surface executor capacity metrics for OCR request execution and page-task OCR
  execution.
- Prove by test that two queued pages can be dispatched concurrently to the same
  OCR node when capacity allows.

**Non-Goals:**
- Do not make LLM health status a processing gate; existing unhealthy-but-enabled
  configs remain eligible.
- Do not change OCR node selection or fallback semantics.
- Do not add dynamic executor resizing after startup in this change.
- Do not change the external PaddleOCR service behavior; if the external server
  serializes work internally, this change only makes DocLens-side concurrency
  observable.

## Decisions

- Add a selector status API or equivalent classification that can tell the
  post-processor whether configs are absent, paused, incomplete, or selectable.
- Represent paused LLM fallback as a warning distinct from
  `llm_markdown_post_processing_failed` and `no_available_llm_config`.
- Extend thread-pool metrics with `core_pool_size`, `maximum_pool_size`, and
  `largest_pool_size` when the executor is a `ThreadPoolExecutor`.
- Add page-task worker runtime settings to dashboard OCR resource metrics so the
  UI can show effective `pool_size` and `batch_size`.
- Update the resource cards to display active usage against configured capacity
  instead of only active count.

## Data Flow

1. OCR aggregation asks the Markdown post-processor to format the merged OCR text.
2. The configurable post-processor classifies current Markdown LLM config state.
3. If configs are paused, it returns OCR text with a paused warning and no LLM
   failure message.
4. If a config is selectable, the runtime LLM call proceeds as before; exceptions
   still become failure fallback warnings.
5. Dashboard metrics read thread-pool runtime state and page-task worker settings.
6. The dashboard renders active OCR work against effective capacity so users can
   verify whether page-level OCR concurrency matches page configuration.

## Risks / Trade-offs

- A paused warning requires dashboard text updates and tests, but keeps old
  failure semantics intact for real attempted calls.
- Executor capacity metrics show DocLens process capacity, not whether the remote
  OCR process itself handles requests in parallel.
- Dynamic resizing remains out of scope; users still need a service restart after
  changing persisted OCR node concurrency if they expect local executor defaults
  to follow those changes.

## Testing

- Add backend tests for LLM paused classification and passthrough warning.
- Add dashboard utility tests for paused LLM result wording.
- Add backend metrics tests for configured OCR/page-task capacity fields.
- Add a focused page-task execution test that blocks two OCR calls and proves
  both calls are in flight before either completes.
- Run focused backend and frontend tests plus the relevant broader slices.
