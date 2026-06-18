## Context

当前仓库已经把 Ollama OCR 的运行时行为当成一个家族能力在处理，
例如表单会允许输入真实 provider model。问题在于模型标识还停留在
`ollama_deepseek_ocr`，导致“模型 key”与“用户看到的提供方名称”不一致。

## Goals / Non-Goals

**Goals:**
- 把 `ollama` 作为 Ollama OCR 的 canonical key。
- 保留 `ollama_deepseek_ocr` 作为兼容别名。
- 让 API 返回的模型名称可稳定序列化为 `name`，供 Dashboard 显示。
- 保持 Ollama 家族表单里的 provider model 自由输入行为。

**Non-Goals:**
- 不改变 Ollama 运行时的 `/api/generate` 协议。
- 不改 LLM 轨道或文档处理轨道的其它逻辑。
- 不引入新的模型类型枚举。

## Decisions

- 模型定义层使用 `ollama` 作为正式 key，`Ollama` 作为正式名称。
- 旧 key 只做兼容解析，不再作为新建或展示的默认值。
- API 响应显式暴露 `name` 字段，避免依赖 Java getter/record 推断。
- 前端仍然通过现有的 Ollama family 判断决定是否显示 provider model 输入框。

## Data Flow

1. Dashboard 拉取 `/api/v1/ocr-models`。
2. 后端返回 `model_key=ollama`，`name=Ollama`。
3. Dashboard 以 `Ollama · ollama` 的形式展示模型列表。
4. 用户在 Ollama 节点下方输入真实 provider model，例如 `deepseek-ocr:latest`。
5. 提交时 provider model 原样进入节点载荷，旧 key 兼容映射仍可读取历史节点。

## Risks / Trade-offs

- 如果仓库外部还有依赖 `ollama_deepseek_ocr` 的脚本，它们需要在后续切换到 `ollama`。
- 只改显示不改 canonical key 会继续保留命名歧义，所以这次明确做 key 收敛。

## Testing

- 先写 contract test，确认模型列表返回 `ollama` 与可序列化 `name`。
- 再写 dashboard form test，确认页面显示 `Ollama`，且 Ollama family 仍可自由输入 provider model。
- 最后跑后端契约测试和 dashboard 组件测试，覆盖旧 key 兼容路径。

