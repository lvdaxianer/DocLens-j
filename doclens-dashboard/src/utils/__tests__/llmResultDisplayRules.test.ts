import test from 'node:test'
import assert from 'node:assert/strict'

import {
  isLlmMarkdownApplied,
  llmStageDescription,
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
  assert.equal(llmPostProcessingStatus(result), 'LLM 回退 OCR')
})

test('llm result display keeps ocr label when llm is not configured', () => {
  const result = {
    finalText: '原始 OCR 文本',
    llmMarkdownApplied: false,
    warnings: []
  }

  assert.equal(isLlmMarkdownApplied(result), false)
  assert.equal(resultTextTitle(result), 'OCR 纯文本')
  assert.equal(llmPostProcessingStatus(result), 'LLM 未启用')
})

test('llm result display distinguishes success fallback and disabled state descriptions', () => {
  assert.equal(llmStageDescription({
    finalText: '# 发票',
    llmMarkdownApplied: true,
    warnings: []
  }), '已输出 Markdown 结构化结果')

  assert.equal(llmStageDescription({
    finalText: '原始 OCR 文本',
    llmMarkdownApplied: false,
    warnings: ['llm_markdown_post_processing_failed']
  }), 'LLM 排版失败，已回退 OCR 纯文本')

  assert.equal(llmStageDescription({
    finalText: '原始 OCR 文本',
    llmMarkdownApplied: false,
    warnings: []
  }), '未配置 LLM 后处理，返回 OCR 纯文本')
})
