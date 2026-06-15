## Context

DocLens Java is a multi-module OCR SDK and HTTP service. The repository already
has useful documentation, including packaging guides, SDK/HTTP delivery notes,
and an OpenWebUI integration contract. The root READMEs duplicate some of that
content and contain a growing list of operational details such as Ollama notes,
LLM credential behavior, endpoint tables, and configuration values.

The user asked to document the project with broad introductions in
`README.md` and `README-en.md`, and to move API-style details into a separate
`api.md`. After scanning the codebase, the recommended minimal split is:
`api.md`, `sdk.md`, `configuration.md`, and `development.md`, while keeping the
existing packaging and OpenWebUI integration pages.

## Goals / Non-Goals

**Goals:**

- Make the root READMEs good first-read documents in Chinese and English.
- Create a dedicated API reference document for native DocLens endpoints,
  dashboard support endpoints, OCR node administration, LLM Markdown config,
  and OpenWebUI integration endpoints.
- Create focused SDK, configuration, and development documents to reduce README
  size without losing important project information.
- Preserve existing useful docs by linking to them rather than copying them.
- Keep terminology and endpoint names aligned with current controllers and
  existing contract tests.

**Non-Goals:**

- No Java, Vue, build, database, or runtime behavior changes.
- No generated OpenAPI specification.
- No exhaustive request/response schema for every DTO unless already visible
  from existing docs or controllers.
- No changes to packaging scripts or development scripts.

## Decisions

- Use root-level `README.md` and `README-en.md` as the bilingual entry points.
  They will include positioning, module map, quick start, common workflows, and
  links to detailed docs.
- Use `docs/api.md` as the main HTTP reference. It will list endpoint groups,
  methods, paths, purpose, and important request details such as multipart
  batch uploads and integration authorization.
- Use `docs/sdk.md` for embedded Spring Boot starter and pure Java SDK usage.
  Existing material from `docs/sdk-http-delivery.md` can be consolidated there.
- Use `docs/configuration.md` for key `application.yml` properties and
  environment variable conventions for secrets.
- Use `docs/development.md` for local scripts, ports, logs, tests, and useful
  development commands.
- Keep `docs/packaging.md`, `docs/packaging_en.md`, and
  `docs/integrations/open-webui-ocr-contract.md` as focused detailed docs.

## Risks / Trade-offs

- A hand-written API reference can drift from controllers. Mitigation: derive
  endpoint lists from controller mappings and keep wording at reference-summary
  level rather than inventing undocumented DTO fields.
- The repository currently has `README_EN.md`, while the user named
  `README-en.md`. To avoid breaking existing links unexpectedly, the change will
  preserve compatibility by keeping the current English README path or adding a
  bridge only if needed after inspecting repository conventions.
- Documentation-only changes do not benefit from ordinary unit-test TDD. The
  useful check is a failing/passing documentation verification script or shell
  check for required files, expected headings, and link targets.
