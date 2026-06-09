export interface DocumentResultDisplayPayload {
  finalText: string
  llmMarkdownApplied?: boolean
  llm_markdown_applied?: boolean
  warnings: string[]
}

const LLM_POST_PROCESSING_FAILED_WARNING = 'llm_markdown_post_processing_failed'

/**
 * 判断当前结果是否成功使用了 LLM Markdown 后处理。
 *
 * @param result - 文档结果载荷
 * @returns 是否为成功的 LLM Markdown 结果
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function isLlmMarkdownApplied(result: DocumentResultDisplayPayload): boolean {
  const applied = result.llmMarkdownApplied ?? result.llm_markdown_applied ?? false
  return applied
    && result.finalText.trim() !== ''
    && !result.warnings.includes(LLM_POST_PROCESSING_FAILED_WARNING)
}

/**
 * 获取结果抽屉中的文本标题。
 *
 * @param result - 文档结果载荷
 * @returns 文本标题
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function resultTextTitle(result: DocumentResultDisplayPayload): string {
  return isLlmMarkdownApplied(result) ? 'Markdown 内容' : 'OCR 纯文本'
}

/**
 * 获取 LLM 后处理状态文案。
 *
 * @param result - 文档结果载荷
 * @returns LLM 后处理状态
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function llmPostProcessingStatus(result: DocumentResultDisplayPayload): string {
  return isLlmMarkdownApplied(result) ? 'LLM 已排版' : 'LLM 未生效'
}
