## Context

当前 OCR 节点表单已经支持离线/在线两种部署类型，也已经支持
Ollama 节点把真实 provider model 写入 `provider_model`。
问题出在表单的展示条件过窄：它把 Ollama 家族判断写成了一个固定
`modelKey` 比较，导致真实的 Ollama 家族 key 无法触发同样的输入态。

## Goals / Non-Goals

**Goals:**
- 让 Ollama 家族离线节点始终可以编辑模型名称。
- 保持现有节点创建、编辑和提交数据结构不变。
- 只改最小必要的表单语义，不扩展后端协议。

**Non-Goals:**
- 不修改后端 OCR 节点协议。
- 不重构 OCR 模型列表接口。
- 不改变 PaddleOCR 或在线节点的字段规则。

## Decisions

- 继续使用 `providerModel` 作为 Ollama 的真实模型名输入。
- 把 Ollama 的判定从固定字符串比对改成家族前缀判断。
- 表单展示逻辑只负责“是否显示输入框”，不负责解释模型标准本身。
- 回归测试覆盖真实的 `ollama_*` 模型 key，避免再次退化成固定字符串判定。

## Data Flow

1. 用户在 OCR 节点表单中选择一个 Ollama 家族离线模型。
2. 表单判断它属于 Ollama 家族后，展示“模型名称”输入框。
3. 用户输入真实 provider model，例如 `deepseek-ocr:latest`。
4. 提交时 `providerModel` 原样进入节点载荷，后端继续按既有逻辑处理。

## Risks / Trade-offs

- 如果未来出现非 Ollama 的模型 key 也以前缀 `ollama` 开头，它们会
  被归到 Ollama 家族输入态。当前仓库里这是符合现状的。
- 这次改动只覆盖 UI 语义，不改变后端模型定义；如果后续还要把
  Ollama 语义显式建模到 API，需要另起 change。

## Testing

- 先写失败测试，锁定真实 `ollama_deepseek_ocr` key 会展示可编辑模型名称。
- 再验证提交载荷把用户输入的 provider model 原样带上。
- 最后跑 OCR 表单规则相关测试，确认 Paddle/online 行为未回归。
