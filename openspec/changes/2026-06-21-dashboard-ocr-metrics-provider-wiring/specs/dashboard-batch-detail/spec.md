# dashboard-batch-detail Specification Delta

## Modified Requirements

### Requirement: Batch detail exposes OCR allocation attribution from persisted calls

Dashboard batch detail MUST use the real OCR metrics provider when OCR node and
call repositories are available. Completed documents with successful OCR node
calls MUST include `ocr_final_hit_nodes`, and the batch MUST include
`batch_dispatch_hit_nodes` derived from those calls. The empty OCR metrics
provider MAY only be used when OCR resource-routing infrastructure is absent.

#### Scenario: Completed document shows final OCR assignment

- **GIVEN** OCR resource-routing infrastructure is auto-configured
- **AND** a batch contains a completed document with successful OCR node calls
- **WHEN** the Dashboard batch detail is queried
- **THEN** the document includes a non-empty `ocr_final_hit_nodes` list
- **AND** the batch includes a non-empty `batch_dispatch_hit_nodes` list

#### Scenario: Empty provider is only a fallback

- **GIVEN** OCR node repositories, call repositories, runtime node pool, and OCR model registry exist
- **WHEN** Dashboard query service is auto-configured
- **THEN** it uses the real OCR metrics provider
- **AND** it does not use the empty fallback provider
