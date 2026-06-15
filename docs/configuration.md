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

## Secrets And Environment Variables

LLM Markdown configurations and online OCR nodes store environment variable
names, not real API keys. Set the real secrets before starting the service:

```bash
export MINIMAX_API_KEY=replace-with-real-secret
export DASHSCOPE_API_KEY=replace-with-real-secret
```

OpenWebUI integration uses:

```bash
export DOCLENS_INTEGRATIONS_OPEN_WEBUI_INTERNAL_TOKEN=replace-with-real-token
```

Spring Boot maps environment variables to properties, so
`DOCLENS_PADDLE_OCR_ENDPOINT` overrides `doclens.paddle-ocr.endpoint`.

## Related Docs

- [HTTP API Reference](api.md)
- [Local Development](development.md)
- [OpenWebUI OCR Contract](integrations/open-webui-ocr-contract.md)
