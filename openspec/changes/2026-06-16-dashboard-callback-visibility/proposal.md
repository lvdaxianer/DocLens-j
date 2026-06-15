## Why

Callback delivery results are now available from the Dashboard batch detail API,
but users cannot inspect them from the batch detail page. Operators still need
to call the API manually to see whether a callback succeeded, is retrying, or
failed with a concrete reason. When a callback has reached failure, operators
also need a page-level retry action that immediately replays the same callback
payload and shows any latest failure reason after refresh.

## What Changes

Add a callback result section to the Dashboard batch detail page that renders
the existing `callback_jobs` data, including status, retry count, next retry
time, failure reason, and failure detail. Add a manual retry action for failed
callback jobs that triggers immediate delivery through the existing callback
delivery mechanism and refreshes the batch detail page after completion.

## Impact

The change touches Dashboard UI, a Dashboard retry API path, and the callback
delivery worker surface. It reuses the existing callback delivery persistence,
HTTP delivery, retry policy, and failure reason contract.
