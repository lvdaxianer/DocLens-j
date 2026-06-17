## 1. Constructor Binding Recovery

- [ ] 1.1 Add a focused Spring context regression test that loads
  `DocLensSpringProperties` through configuration binding and proves the bean
  can be created from minimal `doclens.*` values.
- [ ] 1.2 Run the new focused test and confirm it fails with the current
  constructor shape and the `No default constructor found` binding error.
- [ ] 1.3 Remove the ambiguous overloaded constructor from
  `DocLensSpringProperties` and update remaining test call sites to use the
  canonical record constructor.
- [ ] 1.4 Re-run the focused properties tests and the broader starter test set
  that touches `DocLensSpringProperties` instantiation, then package and start
  the backend with `./scripts/dev-restart.sh`.
- [ ] 1.5 Validate the change with `openspec validate 2026-06-17-doclens-backend-startup-fix --strict`, review the diff, and commit the fix.
