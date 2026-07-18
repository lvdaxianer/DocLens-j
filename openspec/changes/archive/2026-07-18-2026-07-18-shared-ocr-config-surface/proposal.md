# Proposal

## Why

The default server configuration and the configuration reference still expose
adapter-specific OCR keys such as `doclens.adapter.default-key` and
`doclens.paddle-ocr.*`. The requested runtime contract is narrower: editable
YAML should focus on shared OCR controls instead of per-adapter toggles.

## What Changes

- remove `doclens.adapter.default-key` from the shipped default configuration
- remove `doclens.paddle-ocr.enabled`, `doclens.paddle-ocr.timeout-seconds`,
  and `doclens.paddle-ocr.visualize` from the shipped default configuration
- update the configuration reference so shared OCR controls remain documented
  while the removed adapter-specific keys are no longer presented as editable
  defaults
- add a regression test that fails if those keys are reintroduced into the
  default server configuration surface

## Impact

- users editing the default server YAML see only shared OCR controls
- internal fallback behavior stays unchanged for this branch
- documentation stays aligned with the shipped configuration files
