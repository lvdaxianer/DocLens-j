const dateFormatter = new Intl.DateTimeFormat('zh-CN', {
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit'
})

const imageProgressStages = new Set([
  'pdf_to_images_completed',
  'ocr_queued',
  'ocr_images',
  'merge_text',
  'save_text',
  'completed',
  'ocr_processing',
  'normalizing',
  'ocr_completed'
])

/** 文档状态中文文案映射。 */
const statusLabels: Record<string, string> = {
  completed: '成功',
  failed: '失败',
  processing: '处理中',
  stalled: '已卡住',
  queued: '排队中',
  created: '已创建'
}

/** 处理阶段中文文案映射。 */
const stageLabels: Record<string, string> = {
  queued: '待调度',
  direct_text_saved: '直通文本保存',
  word_to_pdf: 'Word 转 PDF 中',
  word_to_pdf_completed: 'Word 转 PDF 完成',
  pdf_to_images: 'PDF 转图片中',
  pdf_to_images_completed: 'PDF 转图片完成',
  ocr_queued: 'OCR 排队中',
  ocr_images: 'OCR 图片解析中',
  merge_text: '文本合并中',
  save_text: 'LLM 排版中',
  completed: '解析完成',
  failed: '解析失败',
  rendering: 'PDF 转图片中',
  ocr_processing: 'OCR 图片解析中',
  normalizing: '文本合并中',
  ocr_completed: '解析完成',
  ocr_failed: '解析失败'
}

/** 文件类型中文文案映射。 */
const fileTypeLabels: Record<string, string> = {
  image: '图片',
  pdf: 'PDF',
  word: 'Word',
  markdown: 'Markdown',
  text: 'TXT'
}

/**
 * 格式化百分比展示值。
 *
 * @param value 原始百分比
 * @returns 一位小数百分比文本
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
export function formatPercent(value: number | undefined): string {
  return `${Math.max(0, value ?? 0).toFixed(1)}%`
}

/**
 * 格式化毫秒耗时。
 *
 * @param milliseconds 毫秒数
 * @returns 可读耗时文本
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
export function formatDuration(milliseconds: number | undefined): string {
  const safeValue = Math.max(0, milliseconds ?? 0)
  if (safeValue < 1000) {
    return `${safeValue} ms`
  }
  if (safeValue < 60_000) {
    return `${(safeValue / 1000).toFixed(1)} s`
  }
  return `${(safeValue / 60_000).toFixed(1)} min`
}

/**
 * 格式化数字展示值。
 *
 * @param value 原始数字
 * @returns 中文区域数字文本
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
export function formatNumber(value: number | undefined): string {
  return new Intl.NumberFormat('zh-CN').format(value ?? 0)
}

/**
 * 格式化日期时间。
 *
 * @param value 日期时间字符串
 * @returns 可读日期时间文本
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
export function formatDateTime(value: string | undefined): string {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return dateFormatter.format(date)
}

/**
 * 转换文档状态文案。
 *
 * @param status 文档状态编码
 * @returns 状态中文文案
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
export function statusLabel(status: string | undefined): string {
  return statusLabels[(status ?? '').toLowerCase()] ?? status ?? '-'
}

/**
 * 转换处理阶段文案。
 *
 * @param stage 处理阶段编码
 * @returns 阶段中文文案
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
export function stageLabel(stage: string | undefined): string {
  return stageLabels[(stage ?? '').toLowerCase()] ?? stage ?? '-'
}

/**
 * 格式化图片页进度。
 *
 * @param currentPage 当前完成页
 * @param totalPages 总页数
 * @returns 图片页进度文本
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
export function formatImageProgress(currentPage: number | undefined, totalPages: number | undefined): string {
  const total = Math.max(0, totalPages ?? 0)
  const current = Math.min(Math.max(0, currentPage ?? 0), total)
  if (total > 0) {
    return `${formatNumber(current)} / ${formatNumber(total)} 张`
  }
  return '-'
}

/**
 * 判断阶段是否需要展示图片页进度。
 *
 * @param stage 处理阶段编码
 * @returns true 表示展示图片页进度
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
export function hasImageProgressStage(stage: string | undefined): boolean {
  return imageProgressStages.has((stage ?? '').toLowerCase())
}

/**
 * 转换文件类型文案。
 *
 * @param fileType 文件类型编码
 * @returns 文件类型中文文案
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
export function fileTypeLabel(fileType: string | undefined): string {
  return fileTypeLabels[(fileType ?? '').toLowerCase()] ?? fileType ?? '-'
}
