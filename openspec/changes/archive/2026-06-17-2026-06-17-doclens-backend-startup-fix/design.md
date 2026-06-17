# DocLens Backend Startup Reliability Design

## Approach
The fix should stay inside the Spring configuration model. `DocLensSpringProperties`
will remain the single source of truth for `doclens.*` settings, but the
binding path must be made explicit so Spring Boot does not fall back to a
default-constructor instantiation path.

The implementation will add a focused Spring context test that proves the
properties bean can be created from minimal configuration values. The fix will
then remove the ambiguous overload from the record and update existing tests
that still call the old constructor shape.

## Files
- Modify:
  `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensSpringProperties.java`
- Add:
  `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensSpringPropertiesBindingTest.java`
- Modify:
  `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensSpringPropertiesHealthGovernanceTest.java`
- Modify:
  `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensProcessingAutoConfigurationTest.java`
- Modify:
  `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensOcrThreadPoolAutoConfigurationCallbackTest.java`

## Verification
- Focused regression test for Spring binding.
- Existing starter tests that instantiate `DocLensSpringProperties`.
- Backend packaging and dev restart script.
- OpenSpec strict validation for the change directory.

## Risks
- Removing the overloaded constructor changes the public API of the starter,
  so any remaining call sites must be updated in the same change.
- If Spring still reports a binding error after the overload is removed, the
  canonical constructor will need explicit constructor-binding annotation.
