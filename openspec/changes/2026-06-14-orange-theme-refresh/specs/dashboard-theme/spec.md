## ADDED Requirements

### Requirement: Orange dashboard theme
DocLens dashboard MUST use an orange primary theme across the main navigation, buttons, focus state, and core highlights.

#### Scenario: Primary theme renders in orange
- **WHEN** a user opens the dashboard
- **THEN** primary buttons and active navigation use the orange brand color
- **AND** focus rings and active highlights use matching orange-tinted accents
- **AND** charts and selected model states do not retain the old blue primary color

### Requirement: Neutral surfaces remain unchanged
DocLens dashboard MUST keep the existing neutral background, card, and border treatment while changing the main accent color.

#### Scenario: Layout stays visually stable
- **WHEN** the dashboard renders after the theme update
- **THEN** the page layout, card surfaces, and semantic success/error colors remain in place
