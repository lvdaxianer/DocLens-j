const dateFormatter = new Intl.DateTimeFormat('zh-CN', {
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit'
})

const imageProgressStages = new Set([
  'pdf_to_images_completed',
  'ocr_images',
  'merge_text',
  'save_text',
  'completed',
  'ocr_processing',
  'normalizing',
  'ocr_completed'
])

export function formatPercent(value: number | undefined): string {
  return `${Math.max(0, value ?? 0).toFixed(1)}%`
}

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

export function formatNumber(value: number | undefined): string {
  return new Intl.NumberFormat('zh-CN').format(value ?? 0)
}

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

export function statusLabel(status: string | undefined): string {
  const labels: Record<string, string> = {
    completed: '成功',
    failed: '失败',
    processing: '处理中',
    queued: '排队中',
    created: '已创建'
  }
  return labels[(status ?? '').toLowerCase()] ?? status ?? '-'
}

export function stageLabel(stage: string | undefined): string {
  const labels: Record<string, string> = {
    queued: '待解析',
    direct_text_saved: '直通文本保存',
    word_to_pdf: 'Word 转 PDF 中',
    word_to_pdf_completed: 'Word 转 PDF 完成',
    pdf_to_images: 'PDF 转图片中',
    pdf_to_images_completed: 'PDF 转图片完成',
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
  return labels[(stage ?? '').toLowerCase()] ?? stage ?? '-'
}

export function formatImageProgress(currentPage: number | undefined, totalPages: number | undefined): string {
  const total = Math.max(0, totalPages ?? 0)
  const current = Math.min(Math.max(0, currentPage ?? 0), total)
  if (total > 0) {
    return `${formatNumber(current)} / ${formatNumber(total)} 张`
  }
  return '-'
}

export function hasImageProgressStage(stage: string | undefined): boolean {
  return imageProgressStages.has((stage ?? '').toLowerCase())
}

export function fileTypeLabel(fileType: string | undefined): string {
  const labels: Record<string, string> = {
    image: '图片',
    pdf: 'PDF',
    word: 'Word',
    markdown: 'Markdown',
    text: 'TXT'
  }
  return labels[(fileType ?? '').toLowerCase()] ?? fileType ?? '-'
}
