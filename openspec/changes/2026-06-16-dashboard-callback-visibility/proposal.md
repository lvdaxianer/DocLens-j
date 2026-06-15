## Why

Callback delivery results are now available from the Dashboard batch detail API,
but users cannot inspect them from the batch detail page. Operators still need
to call the API manually to see whether a callback succeeded, is retrying, or
failed with a concrete reason.

## What Changes

Add a callback result section to the Dashboard batch detail page that renders
the existing `callback_jobs` data, including status, retry count, next retry
time, failure reason, and failure detail.

## Impact

The change is UI-only. It consumes the existing batch detail response shape and
does not change callback delivery, retry behavior, persistence, or API paths.
