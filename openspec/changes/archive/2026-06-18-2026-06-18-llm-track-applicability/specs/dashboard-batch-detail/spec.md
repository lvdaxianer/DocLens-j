# dashboard-batch-detail Specification

## ADDED Requirements

### Requirement: LLM post-processing step is independent from file-type merge capability

The Dashboard processing track MUST show `LLM 排版` as applicable for supported documents that enter the Markdown post-processing pipeline, rather than skipping it based on file-type merge capability.

#### Scenario: Markdown document reaches LLM post-processing stage
- **WHEN** a completed Markdown document is displayed in Dashboard batch detail
- **THEN** the `LLM 排版` node is not marked as `skipped`
- **AND** the `LLM 排版` node is shown as a normal processing step in the track

#### Scenario: Word document still skips pre-LLM OCR-only steps
- **WHEN** a completed Word document is displayed in Dashboard batch detail
- **THEN** conversion, render, OCR, and merge steps remain available as before
- **AND** the processing track shape remains eight steps
