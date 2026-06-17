## Context

`UploadView.vue` already sends users to the created batch detail page after a
successful upload. `BatchDetailView.vue` already opens `DocumentResultDrawer`
for individual document results. `DocumentResultDrawer.vue` already renders
the final text, OCR original text, metadata, and copy buttons.

The smallest useful Stage 1 implementation should build on that existing
flow instead of adding a new wizard or large onboarding surface.

## Goals / Non-Goals

**Goals:**

- Let users download a completed document result as Markdown, TXT, or JSON.
- Keep all download behavior client-side so the existing result API remains
  unchanged.
- Provide a concise five-minute guide for startup, upload, batch detail, and
  result download.
- Surface the guide from the upload page without interrupting the upload flow.

**Non-Goals:**

- Do not add new backend endpoints.
- Do not change OCR result persistence or callback payloads.
- Do not implement caller credentials, quotas, or Stage 2 governance.
- Do not build a multi-step onboarding wizard.

## Component Map

- `DocumentResultDrawer.vue`: presentation component for a selected document
  result. It will derive downloadable Markdown, TXT, and JSON payloads from
  existing props and expose three download buttons.
- `dashboardResultDownloadRules.ts`: pure utility for creating stable download
  filenames and payloads, with focused unit tests.
- `UploadView.vue`: route-level composition surface. It will add a compact
  first-run guide link while keeping upload orchestration in the store.
- `docs/quick-trial.md`: documentation artifact for the five-minute trial path.

## Data Flow

`DocumentResultDrawer` receives the existing `document` and `result` props.
It derives download availability with computed values and triggers browser
downloads from local button clicks. No child component mutates parent state.

`UploadView` remains responsible only for upload submission and navigation.
The guide link is static documentation navigation, not new state.

## Testing

- Add unit tests for result download filename and payload utility behavior.
- Add a component test proving the result drawer exposes Markdown, TXT, and
  JSON download actions when a result is available.
- Add a component test proving the upload page exposes the five-minute guide
  link.
- Run the relevant frontend tests plus documentation checks.

## Risks / Trade-offs

- Browser download APIs require DOM objects. Keep object URL creation in a
  small function and revoke URLs immediately after the click is triggered.
- JSON payload size can be large. The first implementation downloads the
  already-loaded result only; it does not request additional data.
- Documentation links can go stale. Keep the guide under `docs/` and reference
  existing local startup commands from `docs/development.md`.
