# ocr-node-form Specification

## Purpose
Define how the OCR node form treats Ollama as a model family standard while
keeping the model name field free-form for the user to enter the real provider
model.

## ADDED Requirements
### Requirement: Ollama family nodes SHALL expose a free-form model name input

The form SHALL render the model name as an editable text input when the user
creates or edits an OCR node that belongs to the Ollama model family instead of
treating the value as a fixed predefined option.

#### Scenario: Ollama family node is being edited

- **WHEN** the form is opened for an offline OCR node whose model key belongs to
  the Ollama family
- **THEN** the form shows the model name field as a text input
- **AND** the user can type a custom provider model value
- **AND** the typed value is preserved in the submission payload

#### Scenario: Ollama family node uses a real family key

- **WHEN** the selected OCR model key is `ollama_deepseek_ocr`
- **THEN** the form still treats the node as Ollama family
- **AND** the model name input remains editable

### Requirement: Non-Ollama behaviors SHALL remain unchanged

The OCR node form SHALL keep the existing PaddleOCR and online-node behaviors
unchanged while applying the Ollama family input rule.

#### Scenario: PaddleOCR offline node is edited

- **WHEN** the selected OCR model key is `paddle_ocr`
- **THEN** the form does not require an Ollama-specific model name field

#### Scenario: Online node is edited

- **WHEN** the deployment type is online
- **THEN** the form continues to use the existing online channel and model name
  fields without any Ollama-specific changes
