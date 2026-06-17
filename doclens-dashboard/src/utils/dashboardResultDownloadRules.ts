import type { DocumentResultResponse, DocumentRow } from '../types/dashboard.ts'
import { extractOcrOriginalText } from './llmResultDisplayRules.ts'

export type ResultDownloadFormat = 'markdown' | 'txt' | 'json'

interface ResultDownloadPayload {
  content: string
  mimeType: string
}

const FILE_EXTENSION_BY_FORMAT: Record<ResultDownloadFormat, string> = {
  markdown: 'md',
  txt: 'txt',
  json: 'json'
}

const MIME_TYPE_BY_FORMAT: Record<ResultDownloadFormat, string> = {
  markdown: 'text/markdown;charset=utf-8',
  txt: 'text/plain;charset=utf-8',
  json: 'application/json;charset=utf-8'
}

/**
 * 构建文档结果下载文件名。
 *
 * @param document 文档行
 * @param result 文档结果
 * @param format 下载格式
 * @returns 下载文件名
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
export function buildResultDownloadFilename(
  document: DocumentRow,
  result: DocumentResultResponse,
  format: ResultDownloadFormat
): string {
  const baseName = document.file_name.replace(/\.[^.]+$/, '')
  const extension = FILE_EXTENSION_BY_FORMAT[format]
  return `${baseName}-${result.result_id}.${extension}`
}

/**
 * 构建文档结果下载载荷。
 *
 * @param result 文档结果
 * @param format 下载格式
 * @returns 下载内容与 MIME 类型
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
export function buildResultDownloadPayload(
  result: DocumentResultResponse,
  format: ResultDownloadFormat
): ResultDownloadPayload {
  const content = resultDownloadContent(result, format)
  return {
    content,
    mimeType: MIME_TYPE_BY_FORMAT[format]
  }
}

/**
 * 触发浏览器本地文件下载。
 *
 * @param payload 下载载荷
 * @param filename 文件名
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
export function triggerResultDownload(payload: ResultDownloadPayload, filename: string): void {
  const blob = new Blob([payload.content], { type: payload.mimeType })
  const objectUrl = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = objectUrl
  anchor.download = filename
  anchor.click()
  URL.revokeObjectURL(objectUrl)
}

/**
 * 按格式提取下载内容。
 *
 * @param result 文档结果
 * @param format 下载格式
 * @returns 下载内容
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
function resultDownloadContent(result: DocumentResultResponse, format: ResultDownloadFormat): string {
  if (format === 'json') {
    return JSON.stringify(result, null, 2)
  }
  if (format === 'txt') {
    return extractOcrOriginalText(result.result) || result.result.finalText
  }
  return result.result.finalText
}
