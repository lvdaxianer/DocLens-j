## Why

Operators configure OCR nodes and each node's concurrency from the dashboard,
but the local OCR request executor and page-task worker currently derive their
default concurrency from bootstrap nodes in `application.yml`. When the
dashboard has three healthy OCR nodes but the config file has one bootstrap
node, the runtime still caps OCR requests at 10 instead of using the page
configured OCR capacity.

## What Changes

Make persisted dashboard OCR node configuration the first source for default
local OCR concurrency. When the dashboard has enabled, global, healthy OCR
nodes, sum those nodes' `maxConcurrency` values and use that value for the OCR
request executor and page-task worker defaults. If no persisted dashboard node
is available for that calculation, retain the existing bootstrap-node fallback.

Explicit page-task worker and OCR request thread-pool overrides remain
authoritative.

## Impact

This changes runtime concurrency derivation only. It does not change OCR node
CRUD APIs, node routing semantics, database schema, upload limits, or retry
policy.
