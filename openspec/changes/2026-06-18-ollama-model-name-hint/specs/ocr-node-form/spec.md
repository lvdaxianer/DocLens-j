# ocr-node-form Specification

## Purpose
Define the UI cue that makes the Ollama family model-name field obviously a
free-form provider-model input.

## ADDED Requirements
### Requirement: Ollama family model-name input SHALL show free-form hint

The OCR node form SHALL show a helper hint under the model-name input when the
selected offline model belongs to the Ollama family.

#### Scenario: Ollama family node is being edited

- **WHEN** the form is opened for an offline OCR node whose model key belongs to
  the Ollama family
- **THEN** the model-name field shows a helper hint that indicates the value is
  a free-form provider model
- **AND** the helper hint may mention an example such as `deepseek-ocr:latest`

#### Scenario: PaddleOCR offline node is being edited

- **WHEN** the selected OCR model key is `paddle_ocr`
- **THEN** the model-name field does not show the Ollama-specific helper hint

### Requirement: Existing OCR form behavior SHALL remain unchanged

The OCR node form SHALL keep its existing deployment-type, validation, and
submission behavior unchanged when adding the Ollama helper hint.

#### Scenario: Online node is being edited

- **WHEN** the deployment type is online
- **THEN** the existing online-channel and model-name fields continue to work
  without any Ollama-specific hint
