## Why

DocLens-j 里 Ollama OCR 目前存在两个并存的语义：

- 后端和测试里还在使用 `ollama_deepseek_ocr` 作为模型 key
- 前端表单和用户认知却把它当作 `Ollama` 家族能力

这会让用户同时看到“`Ollama`”和“`ollama_deepseek_ocr`”两套命名，
并且表单里输入的真实 OCR 模型名也很容易被误解为固定字段名。

## What Changes

把 Ollama OCR 统一成更清晰的语义：

- `ollama` 成为 OCR 模型的 canonical key
- API 和 Dashboard 显示名称统一为 `Ollama`
- `ollama_deepseek_ocr` 作为兼容别名继续可用，避免已有配置失效
- Ollama 家族节点继续支持在表单里自由输入真实 provider model

## Impact

这会影响模型列表、节点表单、契约测试和少量基础测试数据。
对已有节点的主要要求是继续识别旧 key，不需要迁移数据。

