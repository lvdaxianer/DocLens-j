# HTTP API Reference

DocLens Server exposes native OCR APIs under `/api/v1/**` and integration APIs
under `/api/v1/integrations/**`. The native APIs are intended for first-party
clients and the dashboard. OpenWebUI should use the dedicated integration
prefix because it has separate authorization and metadata rules.

## Health

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/v1/health` | Basic DocLens service health check |
| `GET` | `/api/v1/heartbeat` | Service heartbeat response |
| `HEAD` | `/api/v1/heartbeat` | Lightweight heartbeat compatibility check |
| `GET` | `/actuator/health` | Spring Boot Actuator health endpoint |

## OCR Batch Upload And Query

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/v1/batches` | Create an OCR batch from multipart files |
| `GET` | `/api/v1/batches/{batchId}` | Query batch status and document summaries |
| `GET` | `/api/v1/batches/by-idempotency-key/{idempotencyKey}` | Query the latest matching batch by idempotency key |
| `GET` | `/api/v1/batches/{batchId}/events` | Query batch processing events |
| `DELETE` | `/api/v1/batches/{batchId}` | Delete an OCR batch and related documents |

Batch upload uses `multipart/form-data`:

```bash
curl -X POST http://localhost:10003/api/v1/batches \
  -F 'files=@demo.pdf' \
  -F 'metadata={"bizId":"A-1001","source":"curl"}' \
  -F 'idempotency_key=idem-demo-001' \
  -F 'callback_url=http://localhost:9000/callback' \
  -F 'pdf_mode=page_image_fallback'
```

Common fields:

| Field | Required | Description |
| --- | --- | --- |
| `files` | Yes | One or more uploaded files |
| `metadata` | No | JSON object string stored with the batch |
| `idempotency_key` | No | Client-provided third-party correlation key; DocLens stores and forwards it without duplicate rejection |
| `callback_url` | No | Callback URL for completion notification |
| `pdf_mode` | No | PDF handling mode, commonly `page_image_fallback` |

When `callback_url` is present, DocLens creates a durable callback job after a
document reaches OCR completion and sends an asynchronous HTTP `POST` to that
URL. The request body uses the completion callback contract:

```json
{
  "meta": {},
  "text": {},
  "idempotency_key": ""
}
```

Any HTTP `2xx` response marks the callback job as `success`. Non-`2xx`
responses, timeouts, network failures, and unexpected delivery exceptions are
recorded with a machine-readable `failure_reason` and a readable
`failure_detail`. Failed jobs are retried with bounded backoff until the retry
limit is reached.

`idempotency_key` is not a DocLens-side uniqueness key. If the same value is
uploaded more than once, DocLens creates separate batches and forwards the
stored value in each callback. The callback receiver owns any duplicate or
idempotency decision.

## OCR Documents And Results

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/v1/documents/{documentId}` | Query document status |
| `GET` | `/api/v1/documents/{documentId}/result` | Query OCR or Markdown result |
| `POST` | `/api/v1/documents/{documentId}/retry` | Retry a failed document |
| `DELETE` | `/api/v1/documents/{documentId}` | Delete a document |

## Adapter Capabilities

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/v1/adapters` | List available OCR adapter capabilities |

## Dashboard Support APIs

The Vue dashboard uses these endpoints to display summary, batch details, and
OCR health information.

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/v1/dashboard/summary` | Dashboard summary counters |
| `GET` | `/api/v1/dashboard/batches` | Dashboard batch list |
| `GET` | `/api/v1/dashboard/batches/{batchId}` | Dashboard batch details |
| `GET` | `/api/v1/dashboard/ocr-health` | OCR node and model health summary |

`GET /api/v1/dashboard/batches/{batchId}` includes a `callback_jobs` array for
callback inspection. Each item exposes:

| Field | Description |
| --- | --- |
| `callback_job_id` | Callback job identifier |
| `event_id` | Completion event that produced the callback job |
| `batch_id` | Batch identifier |
| `document_id` | Document identifier, or empty when unavailable |
| `callback_url` | Delivery target URL |
| `status` | `pending`, `retrying`, `success`, or `failed` |
| `retry_count` | Failed delivery attempts already recorded |
| `next_retry_at` | Next scheduled retry time for retrying jobs |
| `failure_reason` | Failure category such as `http_status`, `timeout`, `network`, or `unexpected` |
| `failure_detail` | Human-readable failure detail; terminal failures always keep this value |
| `updated_at` | Last callback job update time |

Failed callback jobs can be replayed from the Dashboard:

```bash
curl -X POST http://localhost:8080/api/v1/dashboard/callback-jobs/{callbackJobId}/retry
```

The response body exposes:

| Field | Description |
| --- | --- |
| `callback_job_id` | Retried callback job identifier |
| `delivered_count` | `1` when the retry POST succeeds, otherwise `0` |

If the manual retry fails, DocLens records the latest `failure_reason` and
`failure_detail` on the same callback job fields returned by the batch detail
API.

## OCR Models And Nodes

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/v1/ocr-models` | List OCR models |
| `GET` | `/api/v1/ocr-models/{modelKey}/nodes` | List nodes for one OCR model |
| `POST` | `/api/v1/ocr-models/{modelKey}/nodes` | Create an OCR node |
| `GET` | `/api/v1/ocr-nodes/{nodeId}/calls` | Query OCR node call history |
| `PUT` | `/api/v1/ocr-nodes/{nodeId}` | Update an OCR node |
| `PATCH` | `/api/v1/ocr-nodes/{nodeId}/enabled` | Enable or disable an OCR node |
| `DELETE` | `/api/v1/ocr-nodes/{nodeId}` | Delete an OCR node |
| `POST` | `/api/v1/ocr-nodes/{nodeId}/test` | Test an OCR node |
| `POST` | `/api/v1/ocr-nodes/{nodeId}/reconnect` | Manually reconnect or recover an OCR node |

## OCR Governance

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/v1/ocr-governance-config` | Read routing and health governance settings |
| `PUT` | `/api/v1/ocr-governance-config` | Update routing and health governance settings |

## LLM Markdown Configuration

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/v1/llm-markdown-config` | List or read LLM Markdown configurations |
| `POST` | `/api/v1/llm-markdown-config` | Create an LLM Markdown configuration |
| `PUT` | `/api/v1/llm-markdown-config` | Update the legacy/default LLM Markdown configuration |
| `PUT` | `/api/v1/llm-markdown-config/{id}` | Update one LLM Markdown configuration |
| `DELETE` | `/api/v1/llm-markdown-config/{id}` | Delete one LLM Markdown configuration |
| `PATCH` | `/api/v1/llm-markdown-config/{id}/enabled` | Enable or disable one configuration |
| `PATCH` | `/api/v1/llm-markdown-config/{id}/default` | Mark one configuration as default |
| `POST` | `/api/v1/llm-markdown-config/test` | Test an LLM Markdown configuration |

LLM and online OCR credentials are stored as environment variable names in
DocLens configuration records. Set the real secret in the process environment
before starting the service.

## OpenWebUI OCR Integration

OpenWebUI callers should use:

```text
/api/v1/integrations/open-webui/ocr
```

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/v1/integrations/open-webui/ocr/batches` | Create an OCR batch for OpenWebUI |
| `GET` | `/api/v1/integrations/open-webui/ocr/batches/{batchId}` | Query integration batch status |
| `GET` | `/api/v1/integrations/open-webui/ocr/batches/{batchId}/events` | Query integration batch events |
| `GET` | `/api/v1/integrations/open-webui/ocr/documents/{documentId}` | Query integration document status |
| `GET` | `/api/v1/integrations/open-webui/ocr/documents/{documentId}/result` | Query integration OCR result |
| `POST` | `/api/v1/integrations/open-webui/ocr/documents/{documentId}/retry` | Retry an integration document |
| `GET` | `/api/v1/integrations/open-webui/ocr/health` | Integration health check |

These endpoints require the internal bearer token configured by
`doclens.integrations.open-webui.internal-token`.

See [OpenWebUI OCR Contract](integrations/open-webui-ocr-contract.md) for the
full request/response contract and metadata conventions.
