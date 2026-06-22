## Why

The caller partition header was previously written as `X-Recall-Key`, but the
confirmed product contract is `X-Doclens-Key`. Keeping the wrong header makes
Dashboard clients, backend request resolution, documentation, and the approved
caller-key isolation spec disagree with the intended integration contract.

## What Changes

Rename the caller partition header and Dashboard session storage key from
`X-Recall-Key` to `X-Doclens-Key`.

The existing semantics do not change: the value remains an opaque caller
partition key, the backend does not validate it as identity proof, missing or
blank keys are rejected, data visibility remains partition-scoped, and caller
traffic limits still use the resolved partition.

## Impact

Callers must send `X-Doclens-Key`. `X-Recall-Key`,
`X-DocLens-Credential-Key`, legacy credential storage, API key headers, and
`Authorization` are not accepted or translated for caller partitioning.
