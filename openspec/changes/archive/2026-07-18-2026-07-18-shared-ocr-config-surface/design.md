# Design

## Scope

This is a configuration-surface cleanup, not an OCR routing redesign. The
internal runtime defaults used by the current branch remain in code so existing
tests and startup behavior stay stable.

## Decisions

1. Remove only the requested adapter-specific keys from the default
   `application.yml` surface.
2. Keep profile-only Paddle bootstrap connection settings unchanged for now,
   because they are still used by local and production bootstrap flows in this
   branch.
3. Treat the configuration reference as part of the public config surface and
   remove the same keys there.
4. Guard the shipped default YAML with a focused regression test so the removed
   keys do not quietly return.

## Risks

- If documentation alone is changed, future edits can accidentally reintroduce
  the keys in `application.yml`.
- If runtime defaults are removed from code in this pass, unrelated OCR startup
  tests would need broader redesign.

## Verification

- focused test: run the configuration-surface regression test
- broader test: run the Spring properties binding test suite for the starter
