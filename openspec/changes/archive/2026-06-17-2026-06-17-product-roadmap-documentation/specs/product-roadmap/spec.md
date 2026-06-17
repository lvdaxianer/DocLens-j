## ADDED Requirements

### Requirement: Product roadmap document

DocLens-j MUST provide a product roadmap document that records the agreed
product direction for moving from OCR infrastructure capability to a usable
document parsing product.

#### Scenario: Reader reviews the roadmap stages

- **WHEN** a reader opens `docs/product-roadmap.md`
- **THEN** the document describes Stage 1 as product trial and result
  consumption
- **AND** the document describes Stage 2 as caller credentials and request
  governance
- **AND** the document describes Stage 3 as scenario templates and
  differentiation
- **AND** each stage includes priorities, value, acceptance checks, and success
  metrics

### Requirement: Caller credential governance boundary

The product roadmap MUST state that DocLens-j identifies calling systems
through request credentials and does not own a personal login or user account
system in the current product direction.

#### Scenario: Reader checks identity scope

- **WHEN** a reader reviews the Stage 2 roadmap
- **THEN** the document explains API Key, Bearer Token, signature, or header
  credentials as caller identification options
- **AND** the document says batches, documents, and callbacks should be
  attributable to a caller
- **AND** the document explicitly lists personal login, user tables, and RBAC
  as non-goals for the current roadmap
