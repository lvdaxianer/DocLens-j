# ocr-route-capacity-clarity Specification Delta

## Modified Requirements

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
