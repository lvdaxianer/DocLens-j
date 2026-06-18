## Why

Ollama 家族的 OCR 节点现在已经支持自由输入真实 provider model，
但表单界面仍然只显示一个普通“模型名称”字段。对用户来说，这个输入
框和 PaddleOCR、在线节点看起来过于相似，不够明确地表达“这里填的是
Ollama 的真实模型名”。

## What Changes

给 Ollama 家族节点的模型名称输入框补一条上下文提示，明确告诉用户：

- 这是 Ollama family 节点的自由输入项。
- 可以直接填写真实 provider model，例如 `deepseek-ocr:latest`。
- PaddleOCR 和在线节点保持原有展示方式不变。

## Impact

这只会影响前端表单提示文案，不改变表单校验、提交 payload 或后端
模型协议。它是对上一个修复的 UI 收尾，不是新的功能分支。
