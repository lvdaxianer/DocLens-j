## 1. Callback Job Model

- [ ] 1.1 Add callback job domain/persistence support with status, retry count,
  failure reason, failure detail, and payload fields.
- [ ] 1.2 Add repository tests covering job creation, status transitions, and
  failure field persistence.

## 2. Callback Delivery

- [ ] 2.1 Add a callback delivery worker that sends the stored payload to the
  configured `callback_url`.
- [ ] 2.2 Add focused tests for success, HTTP failure, timeout/network failure,
  and failure reason capture.

## 3. Retry And Visibility

- [ ] 3.1 Add bounded retry handling with backoff and terminal failure marking.
- [ ] 3.2 Expose callback outcome details through the existing event or query
  path so success and failure can be inspected.
- [ ] 3.3 Run focused and broader verification, then update the callback
  delivery documentation where the public API currently promises callback
  behavior.
