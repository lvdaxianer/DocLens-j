## ADDED Requirements

### Requirement: LLM credential environment variable guidance is explicit

The Dashboard LLM configuration experience MUST explain that credential input is an environment variable name resolved by the backend service process.

#### Scenario: Operator edits an LLM config

- **WHEN** the operator opens the LLM config drawer
- **THEN** the credential field label and helper text explain that the value is an environment variable name
- **AND** the UI states that the environment variable must be set before starting or restarting the backend service

#### Scenario: Runtime LLM fails because the env var is missing

- **WHEN** a document result contains a credential environment variable missing error
- **THEN** the result drawer displays a Chinese actionable message naming the missing environment variable
- **AND** the message tells the operator to configure the backend service environment and restart the service
