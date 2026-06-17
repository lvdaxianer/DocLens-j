# DocLens Backend Startup Reliability

## Summary
DocLens backend startup currently fails when `./scripts/dev-restart.sh`
rebuilds and launches the server because Spring Boot cannot bind
`DocLensSpringProperties` from configuration.

## Why this change
The dev restart script is the normal local entry point, so startup failures
block every downstream workflow. The log shows Spring trying to instantiate
`DocLensSpringProperties` with a default constructor, which indicates the
record is no longer being treated as an unambiguous constructor-bound
configuration type.

## Scope
- Restore constructor-based binding for `DocLensSpringProperties`.
- Add a focused regression test that fails before the fix and passes after
  it.
- Verify the backend starts through the existing dev restart flow.

## Non-goals
- Do not change authentication, login, or caller credential behavior.
- Do not change the dev restart script unless verification shows it is the
  source of the failure.
- Do not alter runtime business logic beyond what is needed to make startup
  succeed.
