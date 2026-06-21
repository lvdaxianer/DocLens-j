import test from 'node:test'
import assert from 'node:assert/strict'

import {
  LLM_DISABLED_WARNING,
  LLM_PAUSED_WARNING,
  friendlyLlmErrorMessage,
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
  assert.equal(llmPostProcessingStatus(result), 'LLM 未应用')
})

test('llm result display distinguishes configured but unused markdown output', () => {
  const result = {
    finalText: '原始 OCR 文本',
    llmMarkdownApplied: false,
    warnings: [LLM_DISABLED_WARNING],
    rawVendorOutput: { llm_markdown_applied: false }
  }

  assert.equal(isLlmMarkdownApplied(result), false)
  assert.equal(llmPostProcessingStatus(result), 'LLM 未配置')
  assert.equal(llmStageDescription(result), '未配置 LLM 后处理，返回 OCR 纯文本')
})

test('llm result display distinguishes paused markdown post processing', () => {
  const result = {
    finalText: '原始 OCR 文本',
    llmMarkdownApplied: false,
    warnings: [LLM_PAUSED_WARNING],
    rawVendorOutput: { llm_markdown_applied: false }
  }

  assert.equal(isLlmMarkdownApplied(result), false)
  assert.equal(resultTextTitle(result), 'OCR 纯文本')
  assert.equal(llmPostProcessingStatus(result), 'LLM 已暂停')
  assert.equal(llmStageDescription(result), 'LLM 后处理已暂停，返回 OCR 纯文本')
})

test('llm result display translates missing credential environment variable errors', () => {
  assert.equal(
    friendlyLlmErrorMessage('credential environment variable DOCLENS_LLM_KEY is not configured'),
    '本次 LLM 尝试未解析到环境变量 DOCLENS_LLM_KEY，请确认后端服务环境已配置该变量后重试文档。'
  )
})

test('llm result display distinguishes success fallback and unused state descriptions', () => {
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
  }), 'LLM 已配置但本次结果未应用，返回 OCR 纯文本')
})
