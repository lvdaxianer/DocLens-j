## MODIFIED Requirements

### Requirement: Durable Callback Delivery

The system SHALL create a persistent callback job for each completed OCR
document that has a non-empty `callback_url`.

#### Scenario: OCR completes with callback URL

- **WHEN** a document reaches the completed OCR state and the batch contains a callback URL
- **THEN** the system creates a callback job that stores the callback URL, event ID, batch ID, document ID, payload body, retry count, and delivery status
- **AND** the callback job payload is the `callback_body` recorded on the `document.completed` event
- **AND** the callback job payload preserves the uploaded `idempotency_key`
