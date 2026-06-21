## Why

Operators diagnose OCR concurrency from the batch detail page while a document is still processing. The current OCR route panel only shows final successful node attribution after OCR completes, and model/node concurrency capacity is visible only on the separate OCR resources page. This makes active OCR work look idle or unallocated.

The LLM configuration form also accepts a credential environment variable name, but the UI and result drawer do not clearly explain that the backend process must actually have that environment variable set before startup.

## What Changes

Expose a batch-detail OCR runtime snapshot with model/node inflight and capacity values, separate current-document running allocation from final allocation, and improve LLM credential environment variable wording and failure display.

## Impact

The change affects the Dashboard batch detail API, the batch OCR route panel, the LLM config drawer, and result failure messaging. It does not change OCR dispatch concurrency, OCR retry behavior, or LLM credential storage semantics.
