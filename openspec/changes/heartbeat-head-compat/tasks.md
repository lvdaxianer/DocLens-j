## 1. Contract Coverage

- [ ] 1.1 Add a failing contract test for `HEAD /api/v1/heartbeat`
  that asserts HTTP 200 and no response body.
- [ ] 1.2 Verify the existing `GET /api/v1/heartbeat` contract remains
  unchanged.

## 2. Controller Delivery

- [ ] 2.1 Implement `HEAD /api/v1/heartbeat` alongside the current GET
  handler.
- [ ] 2.2 Run focused and broader backend verification, then validate
  the OpenSpec change.
