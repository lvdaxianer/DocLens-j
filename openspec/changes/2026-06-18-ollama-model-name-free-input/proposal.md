## Why

DocLens-j 里的 Ollama OCR 节点表单把“模型类型”和“模型名称”绑定
得太死了。现在的实现只在 `modelKey === 'ollama'` 时才显示可编辑的
模型名称输入框，但仓库里实际存在的 Ollama OCR 模型 key 是类似
`ollama_deepseek_ocr` 这样的派生 key。

这会把 Ollama 当成一个固定字符串，而不是一套标准能力。结果就是：
用户明明选择了 Ollama 家族模型，却不能自由输入真实的 provider
model。

## What Changes

调整 Ollama OCR 节点表单的语义：

- 继续把 Ollama 当成一种 OCR 标准/家族能力，而不是单个固定模型名。
- 对于 Ollama 家族的离线模型，模型名称输入框始终可编辑。
- 模型名称填写后继续作为 `providerModel` 提交给后端。
- 补充回归测试，覆盖真实 Ollama 家族模型 key 的表单行为。

## Impact

这会改变 Ollama 离线节点表单的展示与提交规则，但不会改变
PaddleOCR 或在线节点的现有行为。对已经保存的节点数据没有迁移需求，
只是让新增和编辑 Ollama 节点时可以填写真实模型名。
