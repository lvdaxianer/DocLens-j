const dateFormatter = new Intl.DateTimeFormat('zh-CN', {
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit'
})

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
