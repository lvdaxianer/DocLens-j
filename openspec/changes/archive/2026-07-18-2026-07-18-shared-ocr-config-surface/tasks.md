## Task Boundary And Agent Dispatch Plan

### Task 1: Remove adapter-specific OCR keys from the default config surface
- Module-oriented agent name: main-agent/config-surface
- Owned responsibility: update the default server YAML, configuration
  reference, and focused regression coverage for the removed keys
- Allowed files or modules:
  - `doclens-server/src/main/resources/application.yml`
  - `docs/configuration.md`
  - focused test files under `doclens-server` or `doclens-spring-boot-starter`
  - `openspec/changes/2026-07-18-shared-ocr-config-surface/**`
- Out-of-scope work:
  - redesigning Paddle bootstrap node provisioning
  - changing OCR runtime behavior beyond keeping existing internal defaults
  - touching unrelated user changes such as `docs/permission-configuration_zh.md`
- Dependencies:
  - existing default configuration files
  - Spring property binding tests
- Focused verification commands:
  - `mvn -pl doclens-server -am -Dtest=RuntimeProfileConfigurationTest test`
  - `mvn -pl doclens-spring-boot-starter -am -Dtest=DocLensSpringPropertiesBindingTest test`
- Broader verification commands:
  - `openspec validate 2026-07-18-shared-ocr-config-surface --strict`
  - `git diff --check`
- Handoff evidence: removed keys no longer appear in default config or config
  docs, and focused tests pass
- Direct-execution fallback reason: the change is localized and mixes config,
  docs, and tests in a small shared surface, so delegation would add overhead

## Tasks

- [x] 1. Remove adapter-specific OCR keys from the default config surface and
  verify the shared OCR configuration contract.
