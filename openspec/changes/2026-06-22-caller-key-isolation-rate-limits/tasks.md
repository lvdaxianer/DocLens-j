## 1. Planning

- [ ] 1.1 Create OpenSpec proposal, design, requirements, and task checklist for
  caller key isolation and rate-limit stop-loss.
- [ ] 1.2 Validate the OpenSpec change strictly and commit the planning asset.

## 2. Dashboard Header

- [x] 2.1 Add failing frontend API tests proving
  `X-DocLens-Credential-Key` is forwarded raw and legacy auth headers are not
  emitted.
- [x] 2.2 Update the Dashboard request layer to send the raw partition key
  header.

## 2a. Recall Header Rename

- [x] 2a.1 Add failing frontend and backend tests proving `X-Recall-Key` is the
  only caller partition header.
- [x] 2a.2 Rename Dashboard storage/header usage and backend request parsing to
  `X-Recall-Key` without legacy header compatibility.

## 3. Backend Partition Key Resolution

- [x] 3.1 Add failing backend resolver tests proving any non-blank raw key is
  accepted and blank keys are rejected.
- [x] 3.2 Update HTTP caller resolution to use `X-DocLens-Credential-Key`
  without allowlist matching.

## 4. Data Isolation

- [x] 4.1 Add failing query tests proving resources owned by another partition
  key are not visible.
- [x] 4.2 Preserve batch, document, result, event, retry, and delete access
  within the resolved partition key only.

## 5. Rate-Limit Stop-Loss

- [x] 5.1 Add or adjust rate limiter tests proving different partition keys do
  not share interface-group buckets.
- [x] 5.2 Keep caller traffic enforcement based on partition key plus traffic
  group, with global protection as the cross-key fallback.

## 6. Documentation And Configuration

- [ ] 6.1 Update configuration and roadmap wording so the key is described as
  an opaque partition key, not a secret credential.
- [ ] 6.2 Run focused backend, frontend, OpenSpec, and diff checks.
