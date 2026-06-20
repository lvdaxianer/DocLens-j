## Context

OCR node CRUD stores dashboard-created nodes in `doclens_ocr_nodes`.
`OcrRuntimeNodePool` and the routing layer already use those persisted nodes for
dispatch, but `DocLensOcrThreadPoolAutoConfiguration` and
`DocLensPageTaskWorkerAutoConfiguration` derive their default local executor
sizes from `DocLensSpringProperties.paddleOcr().bootstrapNodes()`. This makes
the dispatch layer aware of dashboard nodes while the local execution layer is
still capped by startup YAML.

## Goals / Non-Goals

**Goals:**
- Prefer persisted dashboard OCR node configuration when calculating default
  local OCR concurrency.
- Count only nodes that can participate in real dispatch: enabled,
  `participateGlobal`, and `UP`.
- Fall back to bootstrap node capacity only when no persisted dashboard node is
  available for the default capacity calculation.
- Keep explicit operator overrides for page-task worker and OCR request thread
  pools unchanged.
- Use one shared resolver so page-task worker and OCR request executor cannot
  drift apart.

**Non-Goals:**
- Do not resize an already-created Java executor dynamically after startup.
- Do not include `DOWN` nodes in the derived capacity.
- Do not alter OCR node health transitions, retry counts, or routing weights.
- Do not alter upload limits or dashboard UX in this change.

## Decisions

- Add a small runtime resolver that accepts `DocLensSpringProperties` and, when
  available, `OcrNodeRepository`.
- The resolver first reads persisted nodes and sums `maxConcurrency` for nodes
  where `enabled=true`, `participateGlobal=true`, and `status=UP`.
- If that sum is zero, the resolver falls back to the existing bootstrap-node
  calculation: enabled bootstrap nodes participating in global routing.
- If both persisted and bootstrap sums are zero, preserve the historical page
  worker default.
- Wire both OCR request executor and page-task worker runtime settings through
  the resolver.
- Keep the existing rule that explicit thread-pool/page-task worker properties
  override derived defaults.

## Testing

- Add unit coverage proving persisted UP nodes override bootstrap capacity.
- Add unit coverage proving DOWN persisted nodes are ignored and bootstrap
  capacity is used when no persisted UP global nodes exist.
- Add unit coverage proving explicit worker/thread-pool overrides are still
  respected.
- Run focused auto-configuration tests and a broader module test/build command
  for the touched starter code.
