export interface DocumentResultDisplayPayload {
  finalText: string
  llmMarkdownApplied?: boolean
  llm_markdown_applied?: boolean
  rawVendorOutput?: Record<string, unknown>
  warnings: string[]
}

export const LLM_POST_PROCESSING_FAILED_WARNING = 'llm_markdown_post_processing_failed'
export const LLM_DISABLED_WARNING = 'no_available_llm_config'
export const LLM_PAUSED_WARNING = 'llm_markdown_paused'
const OCR_TEXT_FIELD = 'ocr_text'

/**
 * 判断当前结果是否因为 LLM 排版失败而回退。
 *
 * @param result - 文档结果载荷
 * @returns 是否为失败回退
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function isLlmMarkdownFallback(result: DocumentResultDisplayPayload): boolean {
  return result.warnings.includes(LLM_POST_PROCESSING_FAILED_WARNING)
}

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
 * 提取厂商原始 OCR 文本。
 *
 * @param result - 文档结果载荷
 * @returns OCR 原始文本，不存在时返回空字符串
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
export function extractOcrOriginalText(result: DocumentResultDisplayPayload): string {
  const rawText = result.rawVendorOutput?.[OCR_TEXT_FIELD]
  if (typeof rawText === 'string') {
    return rawText
  } else {
    return ''
  }
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
  if (isLlmMarkdownApplied(result)) {
    return 'LLM 已排版'
  } else if (isLlmMarkdownFallback(result)) {
    return 'LLM 回退 OCR'
  } else if (isLlmMarkdownPaused(result)) {
    return 'LLM 已暂停'
  } else if (isLlmMarkdownDisabled(result)) {
    return 'LLM 未配置'
  } else {
    return 'LLM 未应用'
  }
}

/**
 * 获取 LLM 后处理的详细阶段说明。
 *
 * @param result - 文档结果载荷
 * @returns 详细状态说明
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function llmStageDescription(result: DocumentResultDisplayPayload): string {
  if (isLlmMarkdownApplied(result)) {
    return '已输出 Markdown 结构化结果'
  } else if (isLlmMarkdownFallback(result)) {
    return 'LLM 排版失败，已回退 OCR 纯文本'
  } else if (isLlmMarkdownPaused(result)) {
    return 'LLM 后处理已暂停，返回 OCR 纯文本'
  } else if (isLlmMarkdownDisabled(result)) {
    return '未配置 LLM 后处理，返回 OCR 纯文本'
  } else {
    return 'LLM 已配置但本次结果未应用，返回 OCR 纯文本'
  }
}

/**
 * 判断结果是否明确表示当前未配置 LLM。
 *
 * @param result 文档结果载荷
 * @returns 是否明确未配置 LLM
 * @author lvdaxianerplus
 * @date 2026-06-19
 */
function isLlmMarkdownDisabled(result: DocumentResultDisplayPayload): boolean {
  return result.warnings.includes(LLM_DISABLED_WARNING)
}

/**
 * 判断结果是否明确表示当前 LLM 后处理已暂停。
 *
 * @param result 文档结果载荷
 * @returns 是否明确暂停 LLM
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
function isLlmMarkdownPaused(result: DocumentResultDisplayPayload): boolean {
  return result.warnings.includes(LLM_PAUSED_WARNING)
}
