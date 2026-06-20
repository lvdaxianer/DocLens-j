## 1. Clear retry leftovers

- [x] 1.1 Add a failing test proving document retry must delete prior page tasks and page OCR results before the document is re-scheduled.
- [x] 1.2 Extend the page task and page result repository contracts plus in-memory test doubles with delete-by-document behavior.
- [x] 1.3 Implement the retry cleanup inside `DocumentRetryUseCase` and the MyBatis repositories so retry rebuilds child rows from scratch.
- [x] 1.4 Re-run the focused retry and repository tests, then the broader backend processing slice that covers document retry and page preparation.
