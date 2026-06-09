import test from 'node:test'
import assert from 'node:assert/strict'

import {
  createDefaultLlmMarkdownConfigForm,
  createLlmMarkdownConfigPayload,
  fillLlmMarkdownConfigFormFromResponse,
  isLlmMarkdownConfigFormSubmittable,
  sanitizeLlmMarkdownConfigErrorMessage
} from '../llmMarkdownConfigRules'

test('default llm markdown config form starts disabled and without credential', () => {
  const form = createDefaultLlmMarkdownConfigForm()

  assert.deepEqual(form, {
    url: '',
    model: '',
    apiKey: '',
    credentialConfigured: false
  })
  assert.equal(isLlmMarkdownConfigFormSubmittable(form), true)
  assert.deepEqual(createLlmMarkdownConfigPayload(form), {
    url: '',
    model: ''
  })
})

test('llm markdown config response never hydrates api key into edit form', () => {
  const form = fillLlmMarkdownConfigFormFromResponse({
    url: 'https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions',
    model: 'qwen-vl-ocr-2025-11-20',
    credential_configured: true
  })

  assert.equal(form.apiKey, '')
  assert.equal(form.credentialConfigured, true)
  assert.equal(isLlmMarkdownConfigFormSubmittable(form), true)
  assert.equal('api_key' in createLlmMarkdownConfigPayload(form), false)
})

test('llm markdown config payload includes api key only when nonblank', () => {
  const form = fillLlmMarkdownConfigFormFromResponse({
    url: 'https://llm.example.com/v1/chat/completions',
    model: 'markdown-model',
    credential_configured: true
  })

  assert.deepEqual(createLlmMarkdownConfigPayload(form), {
    url: 'https://llm.example.com/v1/chat/completions',
    model: 'markdown-model'
  })

  form.apiKey = '  sk-new-secret  '

  assert.deepEqual(createLlmMarkdownConfigPayload(form), {
    url: 'https://llm.example.com/v1/chat/completions',
    model: 'markdown-model',
    api_key: 'sk-new-secret'
  })
})

test('llm markdown config requires url and model together with http url', () => {
  const form = createDefaultLlmMarkdownConfigForm()

  form.url = 'https://llm.example.com/v1/chat/completions'
  assert.equal(isLlmMarkdownConfigFormSubmittable(form), false)

  form.model = 'markdown-model'
  assert.equal(isLlmMarkdownConfigFormSubmittable(form), true)

  form.url = 'file:///tmp/llm.sock'
  assert.equal(isLlmMarkdownConfigFormSubmittable(form), false)
})

test('llm markdown config error message masks accidental api key content', () => {
  const message = '调用失败 api_key="sk-new-secret" Authorization: Bearer sk-other-secret'

  assert.equal(
    sanitizeLlmMarkdownConfigErrorMessage(message),
    '调用失败 api_key="***" Authorization: Bearer ***'
  )
})
