import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildResultDownloadFilename,
  buildResultDownloadPayload
} from '../dashboardResultDownloadRules.ts'
import type { DocumentResultResponse, DocumentRow } from '../../types/dashboard.ts'

test('buildResultDownloadFilename keeps document name and result id', () => {
  const filename = buildResultDownloadFilename(documentRow(), documentResult(), 'markdown')

  assert.equal(filename, 'invoice-result-1.md')
})

test('buildResultDownloadPayload returns markdown final text', () => {
  const payload = buildResultDownloadPayload(documentResult(), 'markdown')

  assert.equal(payload.mimeType, 'text/markdown;charset=utf-8')
  assert.equal(payload.content, '# Markdown 结果')
})

test('buildResultDownloadPayload returns ocr original text for txt when present', () => {
  const payload = buildResultDownloadPayload(documentResult(), 'txt')

  assert.equal(payload.mimeType, 'text/plain;charset=utf-8')
  assert.equal(payload.content, '原始 OCR 文本')
})

test('buildResultDownloadPayload falls back to final text for txt', () => {
  const result = documentResult()
  result.result.rawVendorOutput = {}

  const payload = buildResultDownloadPayload(result, 'txt')

  assert.equal(payload.content, '# Markdown 结果')
})

test('buildResultDownloadPayload returns pretty json payload', () => {
  const payload = buildResultDownloadPayload(documentResult(), 'json')

  assert.equal(payload.mimeType, 'application/json;charset=utf-8')
  assert.match(payload.content, /"result_id": "result-1"/)
  assert.match(payload.content, /"finalText": "# Markdown 结果"/)
})

/**
 * 创建文档行测试数据。
 *
 * @returns 文档行
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
function documentRow(): DocumentRow {
  return {
    document_id: 'doc-1',
    batch_id: 'batch-1',
    file_name: 'invoice.pdf',
    file_type: 'pdf',
    status: 'COMPLETED',
    stage: 'DONE',
    progress_percent: 100,
    current_page: 1,
    total_pages: 1,
    duration_ms: 1000,
    llm_chunk_count: 0,
    track: [],
    ocr_running_hit_nodes: [],
    ocr_final_hit_nodes: [],
    error_code: '',
    error_message: '',
    updated_at: '2026-06-12 10:00:00'
  }
}

/**
 * 创建文档结果测试数据。
 *
 * @returns 文档结果
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
function documentResult(): DocumentResultResponse {
  return {
    document_id: 'doc-1',
    result_id: 'result-1',
    result: {
      finalText: '# Markdown 结果',
      llm_markdown_applied: true,
      rawVendorOutput: { ocr_text: '原始 OCR 文本' },
      markdownStorageUri: 'file:///tmp/result.md',
      pages: [],
      confidence: 0.98,
      warnings: [],
      summary: {
        pageCount: 1,
        blockCount: 1,
        tableCount: 0,
        confidence: 0.98
      }
    }
  }
}
