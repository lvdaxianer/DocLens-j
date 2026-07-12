# Configuration

DocLens Server uses Spring Boot configuration binding. Local defaults live in
`doclens-server/src/main/resources/application.yml`.

## Server And Storage

| Property | Default | Description |
| --- | --- | --- |
| `server.port` | `10003` | HTTP service port |
| `doclens.storage-root` | `./var/storage` | Local object storage root |
| `doclens.auto-process-on-upload` | `true` | Schedule uploaded batches on the in-process worker |
| `doclens.worker-id` | `local-worker` | Local worker identifier |

## OCR Routing And Health

| Property | Default | Description |
| --- | --- | --- |
| `doclens.adapter.default-key` | `paddle_ocr` | Default OCR adapter key |
| `doclens.ocr.default-routing-mode` | `GLOBAL_LOAD_BALANCE` | Default OCR routing mode |
| `doclens.ocr.load-balance-strategy` | `weighted-idle` | OCR node selection strategy |
| `doclens.ocr.idle-factor` | `0.7` | Idle-score weight for weighted routing |
| `doclens.ocr.weight-factor` | `0.3` | Configured node weight factor |
| `doclens.ocr.top-bucket-threshold` | `0.15` | Top bucket threshold for weighted routing |
| `doclens.ocr.request-retry-times` | `3` | OCR request retry count |
| `doclens.ocr.probe-interval-seconds` | `5` | OCR health probe interval |
| `doclens.ocr.health-check-timeout-seconds` | `5` | OCR health-check timeout |
| `doclens.ocr.failure-threshold` | `3` | Failures before opening circuit |
| `doclens.ocr.circuit-open-seconds` | `86400` | Circuit open duration |
| `doclens.ocr.recovery-success-threshold` | `3` | Successes required for recovery |
| `doclens.ocr.manual-recovery-attempts` | `3` | Manual reconnect attempts |
| `doclens.ocr.specific-node-fallback-enabled` | `false` | Allow fallback when a specific node fails |

## PaddleOCR

| Property | Default | Description |
| --- | --- | --- |
| `doclens.paddle-ocr.enabled` | `true` | Enable PaddleOCR native adapter |
| `doclens.paddle-ocr.endpoint` | `${DOCLENS_PADDLE_OCR_ENDPOINT:http://10.100.30.215:8080/ocr}` | PaddleOCR native API endpoint |
| `doclens.paddle-ocr.timeout-seconds` | `600` | PaddleOCR request timeout |
| `doclens.paddle-ocr.visualize` | `false` | Ask PaddleOCR to return visualization output |

The default bootstrap node is `paddle-215` on `10.100.30.215:8080`. Override the
endpoint or node configuration for your local environment.

## Document Processing

| Property | Default | Description |
| --- | --- | --- |
| `doclens.extraction.ocr-concurrency` | `4` | OCR concurrency for rendered PDF/Word pages |
| `doclens.pdf-render.dpi` | `36` | PDF page render DPI |
| `doclens.pdf-render.image-format` | `png` | Rendered page image format |
| `doclens.word-conversion.command` | `/opt/homebrew/bin/soffice` | LibreOffice command used for Word conversion |
| `doclens.word-conversion.timeout-seconds` | `60` | Word conversion timeout |

## Callback

| Property | Default | Description |
| --- | --- | --- |
| `doclens.callback.max-retries` | `3` | Maximum callback retries |
| `doclens.callback.timeout-seconds` | `10` | Callback timeout in seconds |

Callback delivery is asynchronous. A completed OCR document with a non-empty
`callback_url` creates a persistent callback job, then the callback worker sends
the stored payload by HTTP `POST`. Delivery succeeds on any `2xx` response.
Failures record both `failure_reason` and `failure_detail`, then retry until
`doclens.callback.max-retries` is exhausted. The current implementation uses a
30-second retry backoff and exposes callback job status through the dashboard
batch detail API as `callback_jobs`.

## Caller Partition And Traffic Limits

Dashboard and API callers must send `X-Doclens-Key` on upload, query,
mutation, and Dashboard data requests. DocLens treats this value as an opaque
caller partition key. It is not an authentication token, is not matched against
a server-side list, and is not used to prove who the caller is.

The same key shares batches, documents, results, events, retries, deletes, and
Dashboard visibility. Different keys are isolated from each other and foreign
resources are returned as not found. Missing or blank keys are rejected so that
requests do not fall into an anonymous shared partition.

Traffic stop-loss is configured globally by interface group under
`doclens.traffic.default-limits`. Each bucket is keyed by caller partition plus
interface group, so one key exhausting `dashboard-read` does not consume another
key's `dashboard-read` allowance. `doclens.traffic.global-protection` remains
the cross-key fallback when callers rotate keys to avoid per-key buckets.

| Property | Default | Description |
| --- | --- | --- |
| `doclens.traffic.enabled` | `true` | Enable caller-partition rate limiting |
| `doclens.traffic.anonymous-enabled` | `false` | Keep anonymous shared partitions disabled |
| `doclens.traffic.default-limits.<group>.qps` | varies | Token refill rate for an interface group |
| `doclens.traffic.default-limits.<group>.burst` | varies | Initial and maximum burst tokens |
| `doclens.traffic.global-protection.enabled` | `true` | Enable service-wide in-flight protection |
| `doclens.traffic.global-protection.max-in-flight` | `100` | Maximum concurrent in-flight requests |

## Environment Variables

LLM Markdown configurations and online OCR nodes store environment variable
names, not real API keys. Set the real values before starting the service:

```bash
export MINIMAX_API_KEY=replace-with-real-value
export DASHSCOPE_API_KEY=replace-with-real-value
```

OpenWebUI integration uses:

```bash
export DOCLENS_INTEGRATIONS_OPEN_WEBUI_INTERNAL_TOKEN=replace-with-real-token
```

Spring Boot maps environment variables to properties, so
`DOCLENS_PADDLE_OCR_ENDPOINT` overrides `doclens.paddle-ocr.endpoint`.

## Related Docs

- [HTTP API Reference](api.md)
- [Permission Configuration](permission-configuration.md)
- [Local Development](development.md)
- [OpenWebUI OCR Contract](integrations/open-webui-ocr-contract.md)
