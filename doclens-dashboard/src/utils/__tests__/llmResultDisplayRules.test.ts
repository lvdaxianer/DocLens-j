import test from 'node:test'
import assert from 'node:assert/strict'

import {
  isLlmMarkdownApplied,
  llmPostProcessingStatus,
  resultTextTitle
} from '../llmResultDisplayRules.ts'

test('llm result display marks markdown output when no fallback warning exists', () => {
  const result = {
    finalText: '# 标题\n\n正文',
    llmMarkdownApplied: true,
    warnings: []
  }

  assert.equal(isLlmMarkdownApplied(result), true)
  assert.equal(resultTextTitle(result), 'Markdown 内容')
  assert.equal(llmPostProcessingStatus(result), 'LLM 已排版')
})

test('llm result display falls back to ocr text label when post processing failed', () => {
  const result = {
    finalText: '原始 OCR 文本',
    llmMarkdownApplied: false,
    warnings: ['llm_markdown_post_processing_failed']
  }

  assert.equal(isLlmMarkdownApplied(result), false)
  assert.equal(resultTextTitle(result), 'OCR 纯文本')
  assert.equal(llmPostProcessingStatus(result), 'LLM 未生效')
})

test('llm result display keeps ocr label when llm is not configured', () => {
  const result = {
    finalText: '原始 OCR 文本',
    llmMarkdownApplied: false,
    warnings: []
  }

  assert.equal(isLlmMarkdownApplied(result), false)
  assert.equal(resultTextTitle(result), 'OCR 纯文本')
  assert.equal(llmPostProcessingStatus(result), 'LLM 未生效')
})
