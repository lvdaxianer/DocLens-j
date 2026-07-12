## Why

Operators need a dedicated permission reference that explains how DocLens-j
protects `/api/v1/**` requests in production and how local developers can call
the same APIs without deploying a gateway. The current configuration reference
mentions caller partitioning and traffic limits, but it does not give complete
production and development examples for trusted gateway authentication,
principal allowlists, admin routes, request headers, and failure responses.

## What Changes

Add a focused permission configuration document under `docs/` and link it from
the main configuration reference. The document will explain the interceptor
chain, required headers, production `application-prod.yml` and environment
variable examples, local development defaults, a local gateway-auth simulation
example, curl samples, failure status codes, and important limitations.

## Impact

This is documentation-only. It does not change authentication, authorization,
rate limiting, runtime defaults, public APIs, database schema, or test behavior.
