# ocr-route-capacity-clarity Specification

## Purpose
Ensure OCR upload routing, resource capacity, and batch route attribution clearly show which registered OCR model is used and how much OCR concurrency is available.
## Requirements
### Requirement: Upload defaults to a concrete OCR model when available

The upload OCR routing selector MUST prefer a concrete OCR model for default uploads after model data is loaded, choosing PaddleOCR when it is available and otherwise choosing the first available OCR model.

#### Scenario: PaddleOCR is available on the upload page

- **WHEN** the upload routing selector loads a model list containing `paddle_ocr`
- **THEN** the selected routing mode is `MODEL_LOAD_BALANCE`
- **AND** the selected OCR model key is `paddle_ocr`
- **AND** the upload value emitted by the selector does not use global load balancing by default

#### Scenario: PaddleOCR is unavailable but another OCR model exists

- **WHEN** the upload routing selector loads a non-empty model list without `paddle_ocr`
- **THEN** the selected routing mode is `MODEL_LOAD_BALANCE`
- **AND** the selected OCR model key is the first available model key from the loaded list

### Requirement: OCR resources expose model and node concurrency capacity

The OCR resources experience MUST show both model-level and node-level concurrency capacity using existing node `max_concurrency` and runtime inflight counts.

#### Scenario: Multiple nodes belong to one OCR model

- **WHEN** an OCR model has multiple configured nodes
- **THEN** the model card shows the model's current inflight image count
- **AND** the model card shows the model's total max concurrency capacity
- **AND** the node table shows each node's max concurrency capacity next to runtime load signals

#### Scenario: Nodes differ in global-routing eligibility

- **WHEN** an OCR model has enabled nodes and global-participating nodes
- **THEN** the model capacity data distinguishes total enabled capacity from global-participating capacity
- **AND** users can understand why global routing may or may not use that model

### Requirement: Batch OCR route attribution names the OCR model clearly

Batch OCR route hit rows MUST include OCR model names and render them separately from node names and provider model details.

#### Scenario: A batch dispatch hits the Ollama OCR model

- **WHEN** a batch route hit row has model key `ollama`
- **THEN** the backend response includes the registered OCR model name `Ollama`
- **AND** the UI displays `Ollama` as the OCR model attribution
- **AND** provider model text such as `deepseek-ocr:latest` is not presented as the configured OCR model name

#### Scenario: A batch dispatch hits PaddleOCR nodes

- **WHEN** a batch route hit row has model key `paddle_ocr`
- **THEN** the backend response includes the registered OCR model name `PaddleOCR`
- **AND** the UI displays node and image-count details without hiding the OCR model attribution

### Requirement: Batch route panel distinguishes running allocation and final attribution

The Dashboard batch OCR route panel MUST show active OCR allocation and final OCR attribution as separate concepts.

#### Scenario: Current document is still running OCR

- **WHEN** the current document has running OCR allocations but no final OCR hit nodes yet
- **THEN** the route panel displays the current running node allocation
- **AND** the route panel labels final attribution as unavailable until successful OCR rows exist
- **AND** the route panel displays node inflight and max concurrency values when available

#### Scenario: Current document has completed OCR

- **WHEN** the current document has final OCR hit nodes
- **THEN** the route panel displays the final node attribution separately from any batch-level dispatch history

### Requirement: Batch route panel shows batch concurrency snapshot

The Dashboard batch OCR route panel MUST show a batch-level concurrency snapshot derived from OCR runtime capacity.

#### Scenario: OCR runtime capacity is available

- **WHEN** the batch detail response includes model and node runtime capacity
- **THEN** the route panel displays model-level `inflight / max concurrency`
- **AND** the route panel displays node-level `inflight / max concurrency`

### Requirement: Dashboard OCR health exposes configured OCR node capacity

Dashboard OCR health MUST expose OCR resource nodes, model capacity, and runtime
inflight counts from the real OCR metrics provider when OCR resource-routing
infrastructure exists. It MUST NOT return the empty provider response while OCR
node management APIs can list configured OCR nodes.

#### Scenario: OCR health reports configured nodes

- **GIVEN** OCR node management can list configured OCR nodes
- **AND** Dashboard OCR metrics infrastructure is auto-configured
- **WHEN** Dashboard OCR health is queried
- **THEN** `ocr_resources.nodes` contains those configured nodes
- **AND** `healthy_node_count`, `down_node_count`, and runtime capacity fields are based on the real node state

