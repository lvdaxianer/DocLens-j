## ADDED Requirements

### Requirement: LLM Markdown configuration editing SHALL use a right-side drawer

The dashboard SHALL open a right-side drawer for both creating and editing LLM
Markdown configurations instead of rendering the editable form inline beside the
table.

#### Scenario: User starts creating a configuration

- **WHEN** the user clicks `新增配置`
- **THEN** the dashboard opens a right-side drawer
- **AND** the configuration table remains visible in the main panel
- **AND** the inline editor is not rendered beside the table

#### Scenario: User edits an existing configuration

- **WHEN** the user clicks `编辑` on a table row
- **THEN** the same right-side drawer opens
- **AND** the form is populated with the selected row values

### Requirement: Drawer editing SHALL preserve existing save and test behavior

The drawer SHALL keep the existing LLM Markdown form fields, validation rules,
test action, and save action behavior unchanged except for the placement of the
editor.

#### Scenario: User saves from the drawer

- **WHEN** the user submits a valid configuration from the drawer
- **THEN** the configuration is saved with the same payload shape as before
- **AND** the table updates with the saved row data

#### Scenario: User tests from the drawer

- **WHEN** the user clicks the test button from the drawer
- **THEN** the existing configuration test flow runs without layout-dependent
  behavior
