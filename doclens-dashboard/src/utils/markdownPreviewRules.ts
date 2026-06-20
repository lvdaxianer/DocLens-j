const EMPTY_MARKDOWN_PLACEHOLDER = '暂无文本内容'
const SAFE_LINK_PROTOCOLS = ['http:', 'https:', 'mailto:']
const HEADING_PATTERN = /^(#{1,6})\s+(.+)$/
const STRONG_PATTERN = /\*\*([^*]+)\*\*/g
const LINK_PATTERN = /\[([^\]]+)]\(([^)]+)\)/g
const TABLE_SEPARATOR_CELL_PATTERN = /^:?-{3,}:?$/

/**
 * 将 Markdown 文本渲染为安全预览 HTML。
 *
 * @param markdown - Markdown 原文
 * @returns 安全预览 HTML
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
export function renderMarkdownPreviewHtml(markdown: string): string {
  const normalizedMarkdown = markdown.trim() || EMPTY_MARKDOWN_PLACEHOLDER
  return normalizedMarkdown
    .split(/\n{2,}/)
    .map(renderMarkdownBlock)
    .join('')
}

/**
 * 渲染单个 Markdown 块。
 *
 * @param block - Markdown 块
 * @returns HTML 块
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
function renderMarkdownBlock(block: string): string {
  const trimmedBlock = block.trim()
  const headingMatch = trimmedBlock.match(HEADING_PATTERN)
  if (headingMatch) {
    // 标题块按井号数量映射为 h1-h6。
    return renderHeading(headingMatch[1].length, headingMatch[2])
  } else if (isMarkdownTable(trimmedBlock)) {
    // 表格块渲染为真实 table，便于查看结构化 OCR/LLM 输出。
    return renderTable(trimmedBlock)
  } else {
    // 非标题块统一作为段落展示，保留块内换行。
    return `<p>${renderInlineMarkdown(trimmedBlock).replace(/\n/g, '<br>')}</p>`
  }
}

/**
 * 渲染 Markdown 标题。
 *
 * @param level - 标题层级
 * @param content - 标题内容
 * @returns 标题 HTML
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
function renderHeading(level: number, content: string): string {
  const safeLevel = Math.min(Math.max(level, 1), 6)
  return `<h${safeLevel}>${renderInlineMarkdown(content)}</h${safeLevel}>`
}

/**
 * 判断 Markdown 块是否为有效管道表格。
 *
 * @param block - Markdown 块
 * @returns 是否为表格块
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
function isMarkdownTable(block: string): boolean {
  const rows = tableRows(block)
  if (rows.length >= 3) {
    // 至少需要表头、分隔行和一行数据才视为表格。
    return rows[1].every((cell) => TABLE_SEPARATOR_CELL_PATTERN.test(cell.trim()))
  } else {
    // 行数不足时保留普通段落渲染。
    return false
  }
}

/**
 * 渲染 Markdown 管道表格。
 *
 * @param block - Markdown 表格块
 * @returns 表格 HTML
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
function renderTable(block: string): string {
  const rows = tableRows(block)
  const headerCells = rows[0].map((cell) => `<th>${renderInlineMarkdown(cell)}</th>`).join('')
  const bodyRows = rows.slice(2)
    .map((row) => `<tr>${renderTableBodyCells(row)}</tr>`)
    .join('')
  return `<table><thead><tr>${headerCells}</tr></thead><tbody>${bodyRows}</tbody></table>`
}

/**
 * 渲染表格正文单元格。
 *
 * @param row - 表格行
 * @returns 表格正文单元格 HTML
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
function renderTableBodyCells(row: string[]): string {
  return row.map((cell) => `<td>${renderInlineMarkdown(cell)}</td>`).join('')
}

/**
 * 提取 Markdown 表格行。
 *
 * @param block - Markdown 表格块
 * @returns 表格单元格矩阵
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
function tableRows(block: string): string[][] {
  return block
    .split('\n')
    .map((line) => splitTableRow(line))
}

/**
 * 拆分 Markdown 表格行。
 *
 * @param line - Markdown 表格行
 * @returns 单元格内容
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
function splitTableRow(line: string): string[] {
  const trimmedLine = line.trim()
  const content = trimmedLine.replace(/^\|/, '').replace(/\|$/, '')
  return content.split('|').map((cell) => cell.trim())
}

/**
 * 渲染行内 Markdown 标记。
 *
 * @param value - 行内 Markdown
 * @returns 行内 HTML
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
function renderInlineMarkdown(value: string): string {
  return renderLinks(escapeHtml(value)).replace(STRONG_PATTERN, '<strong>$1</strong>')
}

/**
 * 渲染安全链接。
 *
 * @param value - 已转义文本
 * @returns 链接 HTML
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
function renderLinks(value: string): string {
  return value.replace(LINK_PATTERN, (_matched, label: string, href: string) => {
    const safeHref = safeLinkHref(href)
    if (safeHref) {
      // 只允许明确安全协议，避免 javascript: 链接进入预览。
      return `<a href="${safeHref}" target="_blank" rel="noopener noreferrer">${label}</a>`
    } else {
      // 非安全链接降级为纯文本标签。
      return label
    }
  })
}

/**
 * 转义 HTML 特殊字符。
 *
 * @param value - 原始文本
 * @returns 转义后文本
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

/**
 * 归一化并校验链接地址。
 *
 * @param href - Markdown 链接地址
 * @returns 安全链接地址
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
function safeLinkHref(href: string): string {
  const normalizedHref = href.trim()
  try {
    const url = new URL(normalizedHref)
    if (SAFE_LINK_PROTOCOLS.includes(url.protocol)) {
      // 校验协议后保留用户原始地址，避免预览展示时额外改写路径。
      return escapeHtml(normalizedHref)
    } else {
      // 不在白名单内的协议不允许渲染为可点击链接。
      return ''
    }
  } catch {
    // 非法 URL 不能作为链接渲染，降级为空地址后由调用方输出纯文本。
    return ''
  }
}
