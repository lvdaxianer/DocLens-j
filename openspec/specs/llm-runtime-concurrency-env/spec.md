# llm-runtime-concurrency-env Specification

## Purpose
Define the runtime guarantees for LLM Markdown credential resolution,
per-config chunk concurrency, Dashboard failure wording, upload OCR routing
defaults, and browser tab icon exposure.
## Requirements
### Requirement: Runtime Markdown credentials use the backend process environment by default

The system SHALL resolve runtime Markdown credential environment variable names against the backend service process environment when no explicit environment map is supplied.

#### Scenario: Production processor reads process environment

- **GIVEN** an enabled LLM Markdown config references `DOCLENS_LLM_KEY`
- **AND** the backend service process environment contains `DOCLENS_LLM_KEY`
- **WHEN** document Markdown post-processing creates the runtime LLM processor
- **THEN** credential resolution succeeds using the process environment
- **AND** the result does not fail solely because the explicit environment map is empty

#### Scenario: Tests can provide an explicit environment map

- **GIVEN** a test creates the runtime Markdown processor with an explicit environment map
- **WHEN** credential resolution runs
- **THEN** the resolver uses the explicit map deterministically
- **AND** the test does not depend on the developer machine environment

### Requirement: LLM request intervals do not serialize available concurrency slots

The LLM config rate limiter SHALL enforce `maxConcurrency` as simultaneous provider calls and SHALL apply request intervals per concurrency slot instead of serializing all starts for the config.

#### Scenario: Two chunks use two available slots

- **GIVEN** one LLM config has `maxConcurrency` set to `2`
- **AND** `requestIntervalMillis` is greater than zero
- **WHEN** two chunk requests acquire the same config at the same time
- **THEN** both requests can start without waiting for a full request interval
- **AND** later reuse of the same slot still respects the interval for that slot

#### Scenario: Updated concurrency is applied for an existing config ID

- **GIVEN** the limiter has already seen config ID `default` with concurrency `1`
- **WHEN** the saved config for `default` is later read with concurrency `10`
- **THEN** the limiter applies the new concurrency value
- **AND** it does not permanently keep the old single-slot semaphore

### Requirement: Missing credential result copy does not overstate current process state

The Dashboard SHALL present missing credential failures as saved document attempt failures and SHALL avoid claiming that the currently running backend process lacks the variable unless freshly verified by the backend.

#### Scenario: Drawer shows saved missing credential failure

- **WHEN** a document result contains `credential environment variable DOCLENS_LLM_KEY is not configured`
- **THEN** the drawer names `DOCLENS_LLM_KEY`
- **AND** it tells the operator to confirm the backend environment and retry the document
- **AND** it does not state as fact that the current service process has not read the variable

### Requirement: Upload OCR routing defaults to global load balancing

The Dashboard SHALL keep the upload OCR routing default on global load balancing after OCR model options load.

#### Scenario: Upload page loads OCR models

- **WHEN** the upload OCR routing selector loads available OCR models
- **THEN** the selected route remains `GLOBAL_LOAD_BALANCE`
- **AND** no OCR model is preselected unless the operator explicitly chooses a model-bound route
- **AND** submitted upload options use the global load-balance strategy

### Requirement: Dashboard exposes a browser tab favicon

The Dashboard SHALL declare a browser tab icon from its HTML entrypoint.

#### Scenario: Browser opens the Dashboard

- **WHEN** the browser loads the Dashboard HTML
- **THEN** the page declares a favicon link
- **AND** the referenced icon asset is served with the Dashboard build output
