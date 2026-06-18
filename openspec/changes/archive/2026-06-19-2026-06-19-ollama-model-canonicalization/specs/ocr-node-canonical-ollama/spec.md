# ocr-node-canonical-ollama Specification

## Purpose

Define the canonical Ollama OCR model identity and keep the UI/contract
consistent while preserving `ollama_deepseek_ocr` as a compatibility alias.

## ADDED Requirements

### Requirement: Ollama OCR SHALL use `ollama` as the canonical model key

The system SHALL expose `ollama` as the primary OCR model key for Ollama-based
OCR nodes and model listings.

#### Scenario: Model list is requested

- **WHEN** `/api/v1/ocr-models` is requested
- **THEN** the Ollama model entry uses `model_key=ollama`
- **AND** the model entry uses `name=Ollama`

#### Scenario: Legacy Ollama key is still encountered

- **WHEN** a node or test fixture still uses `ollama_deepseek_ocr`
- **THEN** the system still recognizes it as an Ollama-compatible alias
- **AND** existing Ollama family behavior remains available

### Requirement: Dashboard SHALL show Ollama by display name

The Dashboard SHALL display the Ollama OCR model using the readable `Ollama`
name while continuing to show its key as supporting context.

#### Scenario: OCR model selector is rendered

- **WHEN** the OCR model list is shown in the dashboard
- **THEN** the Ollama entry is rendered with the display name `Ollama`
- **AND** the selector can still differentiate it by `model_key`

### Requirement: Ollama family nodes SHALL keep provider model free-form

The OCR node form SHALL continue to allow free-form provider model input for
Ollama family nodes.

#### Scenario: Ollama family node is edited

- **WHEN** the selected OCR model belongs to the Ollama family
- **THEN** the form shows the provider model input
- **AND** the user can type the real OCR provider model name

### Requirement: Non-Ollama behaviors SHALL remain unchanged

The OCR node form and model list SHALL keep existing PaddleOCR and online-node
behaviors unchanged while applying the Ollama canonicalization rule.

#### Scenario: PaddleOCR offline node is edited

- **WHEN** the selected OCR model key is `paddle_ocr`
- **THEN** the form behavior remains unchanged

#### Scenario: Online node is edited

- **WHEN** the deployment type is online
- **THEN** the form continues to use the existing online channel and model name
  fields without any Ollama-specific changes

