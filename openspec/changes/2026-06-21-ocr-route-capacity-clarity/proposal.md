## Why

Operators cannot clearly see the effective concurrent OCR capacity across multiple OCR nodes. During upload, the default global route can also use any enabled node that participates in global routing, including the built-in Ollama model whose provider model defaults to `deepseek-ocr:latest`. That makes a PaddleOCR-focused setup appear to be unexpectedly assigned to DeepSeek OCR.

## What Changes

Make upload routing explicit by preferring a selected OCR model for default uploads, expose model and node concurrency capacity in the OCR resource UI, and make batch OCR route hit rows identify the OCR model clearly instead of relying on provider-model-looking text.

## Impact

The change affects upload-page defaults, dashboard OCR capacity presentation, and batch route observability. It preserves document-level model affinity and existing explicit global routing for users who choose to dispatch across all globally participating OCR models.
