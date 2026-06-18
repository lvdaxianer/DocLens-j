## Context

当前 Dashboard 文档处理轨道把 `LLM 排版` 步骤是否适用，绑定到了文件类型的 `hasMerge` 配置上。这样 Markdown/TXT 文档会被固定显示为“该文件类型不需要”，但实际上这类文档也会进入 `DocumentMarkdownPostProcessingService`，并且是否真正执行 LLM 后处理取决于运行时配置是否可用，而不是文件类型本身。

## Goals / Non-Goals

**Goals:**
- 让 Dashboard 处理轨道中的 `LLM 排版` 步骤反映真实的后处理语义，而不是文件类型硬编码。
- Markdown/TXT、PDF、Word、图片等所有会进入 Markdown 后处理链路的文档，都应一致显示 LLM 步骤。
- 保持文档处理轨道的总步数和现有展示结构不变，只修正步骤适用性判定与说明文案。

**Non-Goals:**
- 不改变 `DocumentMarkdownPostProcessingService` 的执行策略。
- 不改动 LLM 配置选择器、健康检查或后处理重试逻辑。
- 不重构 Dashboard 的整条处理轨道结构。

## Decisions

- `LLM 排版` 步骤不再由 `ProcessingTrackProfile.hasMerge()` 决定是否跳过。
- `LLM 排版` 适用性改为表示“该文档会进入 Markdown 后处理阶段”，也就是对当前产品所有可处理文档都视为适用。
- 轨道中“跳过”状态仍保留给真正不参与当前处理链路的步骤，例如 Markdown/TXT 的转换、渲染页图、OCR、合并文本。
- 当前实现优先采用最小改动：只修正 `ProcessingTrackAssembler` 的判定逻辑和回归测试，不引入新的全局配置依赖。

## Data Flow

1. Dashboard 批次详情读取 `DocumentJob`。
2. `ProcessingTrackAssembler` 根据文件类型决定转换、渲染、OCR、合并等前置步骤的适用性。
3. `LLM 排版` 步骤改为独立适用，不再受 `hasMerge` 约束。
4. 文档详情里的轨道因此会在 Markdown/TXT 上正确显示 `LLM 排版` 为当前或完成状态，而不是跳过。

## Risks / Trade-offs

- 这会让轨道展示与文件类型直觉产生轻微偏差：Markdown/TXT 会显示 LLM 步骤“适用”，但它们前面的 OCR/合并步骤仍然会被跳过。这是合理的，因为 LLM 后处理本来就是独立阶段。
- 如果未来要按“运行时是否配置了 LLM”动态控制适用性，需要再引入配置可见性到查询层；这次不做，以免把 Dashboard 读模型耦合进运行时配置。

## Testing

- 用 Dashboard 批次详情测试锁定 Markdown/TXT 文档的 `LLM 排版` 不再显示 `skipped`。
- 保持已有 PDF/Word 场景的轨道断言不变，验证前置步骤继续按文件类型跳过。
