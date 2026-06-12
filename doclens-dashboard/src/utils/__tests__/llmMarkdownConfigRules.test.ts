import test from 'node:test'
import assert from 'node:assert/strict'

import {
  llmConfigCapabilityHints,
  createDefaultLlmMarkdownConfigForm,
  createLlmMarkdownConfigPayload,
  createLlmMarkdownConfigRow,
  fillLlmMarkdownConfigFormFromResponse,
  isLlmMarkdownConfigFormSubmittable,
  toggleLlmMarkdownConfigRowEnabled,
  sanitizeLlmMarkdownConfigErrorMessage
} from '../llmMarkdownConfigRules.ts'

test('default llm markdown config form starts disabled and without credential', () => {
  const form = createDefaultLlmMarkdownConfigForm()

  assert.deepEqual(form, {
    id: '',
    apiType: 'openai',
    url: '',
    model: '',
    apiKey: '',
    credentialConfigured: false,
    enabled: true,
    healthy: false,
    healthMessage: '',
    lastHealthAt: '',
    priority: 100,
    name: '',
    usageType: 'MARKDOWN_POST_PROCESSING',
    defaultConfig: true
  })
  assert.equal(isLlmMarkdownConfigFormSubmittable(form), true)
  assert.deepEqual(createLlmMarkdownConfigPayload(form), {
    api_type: 'openai',
    url: '',
    model: '',
    enabled: true,
    is_default: true,
    priority: 100,
    usage_type: 'MARKDOWN_POST_PROCESSING'
  })
})

test('llm markdown config response never hydrates api key into edit form', () => {
  const form = fillLlmMarkdownConfigFormFromResponse({
    id: 'llm_config_main',
    name: '主配置',
    api_type: 'openai',
    url: 'https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions',
    model: 'qwen-vl-ocr-2025-11-20',
    credential_configured: true,
    usage_type: 'MARKDOWN_POST_PROCESSING',
    priority: 10,
    is_default: true,
    enabled: false,
    healthy: true,
    health_message: ''
  })

  assert.equal(form.apiKey, '')
  assert.equal(form.credentialConfigured, true)
  assert.equal(form.enabled, false)
  assert.equal(form.id, 'llm_config_main')
  assert.equal(form.name, '主配置')
  assert.equal(form.priority, 10)
  assert.equal(isLlmMarkdownConfigFormSubmittable(form), true)
  assert.equal('api_key' in createLlmMarkdownConfigPayload(form), false)
})

test('llm markdown config payload includes api key only when nonblank', () => {
  const form = fillLlmMarkdownConfigFormFromResponse({
    id: 'llm_config_main',
    name: '主配置',
    api_type: 'openai',
    url: 'https://llm.example.com/v1/chat/completions',
    model: 'markdown-model',
    usage_type: 'MARKDOWN_POST_PROCESSING',
    priority: 10,
    is_default: true,
    enabled: true,
    credential_configured: true,
    healthy: true,
    health_message: ''
  })

  assert.deepEqual(createLlmMarkdownConfigPayload(form), {
    api_type: 'openai',
    url: 'https://llm.example.com/v1/chat/completions',
    model: 'markdown-model',
    enabled: true,
    is_default: true,
    name: '主配置',
    priority: 10,
    usage_type: 'MARKDOWN_POST_PROCESSING'
  })

  form.apiKey = '  sk-new-secret  '

  assert.deepEqual(createLlmMarkdownConfigPayload(form), {
    api_type: 'openai',
    url: 'https://llm.example.com/v1/chat/completions',
    model: 'markdown-model',
    enabled: true,
    is_default: true,
    name: '主配置',
    priority: 10,
    usage_type: 'MARKDOWN_POST_PROCESSING',
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

test('llm markdown config accepts any full http endpoint without path restriction', () => {
  const form = createDefaultLlmMarkdownConfigForm()

  form.url = 'https://api.minimaxi.com/v1/chat/completions'
  form.model = 'MiniMax-M3'
  assert.equal(isLlmMarkdownConfigFormSubmittable(form), true)

  form.apiType = 'anthropic'
  form.url = 'https://api.minimaxi.com/anthropic'
  assert.equal(isLlmMarkdownConfigFormSubmittable(form), true)
})

test('llm markdown config error message masks accidental api key content', () => {
  const message = '调用失败 api_key="sk-new-secret" Authorization: Bearer sk-other-secret'

  assert.equal(
    sanitizeLlmMarkdownConfigErrorMessage(message),
    '调用失败 api_key="***" Authorization: Bearer ***'
  )
})

test('llm markdown config hints keep only full url placeholder and test button availability', () => {
  const emptyForm = createDefaultLlmMarkdownConfigForm()

  assert.deepEqual(llmConfigCapabilityHints(emptyForm), {
    urlPlaceholder: '请输入完整接口地址',
    canTest: false
  })

  emptyForm.url = 'https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions'
  emptyForm.model = 'qwen-vl-ocr-2025-11-20'

  assert.deepEqual(llmConfigCapabilityHints(emptyForm), {
    urlPlaceholder: '请输入完整接口地址',
    canTest: true
  })
})

test('llm markdown config supports anthropic endpoint hints and payload', () => {
  const form = createDefaultLlmMarkdownConfigForm()

  form.apiType = 'anthropic'
  form.url = 'https://api.minimaxi.com/anthropic'
  form.model = 'MiniMax-M3'

  assert.deepEqual(llmConfigCapabilityHints(form), {
    urlPlaceholder: '请输入完整接口地址',
    canTest: true
  })

  assert.deepEqual(createLlmMarkdownConfigPayload(form), {
    api_type: 'anthropic',
    url: 'https://api.minimaxi.com/anthropic',
    model: 'MiniMax-M3',
    enabled: true,
    is_default: true,
    priority: 100,
    usage_type: 'MARKDOWN_POST_PROCESSING'
  })
})

test('llm markdown config empty list keeps non blocking empty state', () => {
  const row = createLlmMarkdownConfigRow(undefined)

  assert.equal(row.id, '')
  assert.equal(row.name, '未命名配置')
  assert.equal(row.enabled, false)
  assert.equal(row.statusLabel, '暂无 LLM 配置，OCR 可正常运行。')
})

test('llm markdown config create payload includes multi config governance fields', () => {
  const form = createDefaultLlmMarkdownConfigForm()
  form.name = '备用配置'
  form.apiType = 'anthropic'
  form.url = 'https://api.example.com/anthropic/v1/messages'
  form.model = 'MiniMax-M3'
  form.priority = 20
  form.defaultConfig = false

  assert.deepEqual(createLlmMarkdownConfigPayload(form), {
    name: '备用配置',
    api_type: 'anthropic',
    url: 'https://api.example.com/anthropic/v1/messages',
    model: 'MiniMax-M3',
    enabled: true,
    is_default: false,
    priority: 20,
    usage_type: 'MARKDOWN_POST_PROCESSING'
  })
})

test('llm markdown config pause one row does not mutate other rows', () => {
  const rows = [
    createLlmMarkdownConfigRow({
      id: 'llm_config_main',
      name: '主配置',
      api_type: 'openai',
      url: 'https://llm.example.com/v1/chat/completions',
      model: 'markdown-model',
      credential_configured: true,
      usage_type: 'MARKDOWN_POST_PROCESSING',
      priority: 10,
      is_default: true,
      enabled: true,
      healthy: true,
      health_message: ''
    }),
    createLlmMarkdownConfigRow({
      id: 'llm_config_backup',
      name: '备用配置',
      api_type: 'anthropic',
      url: 'https://api.example.com/anthropic/v1/messages',
      model: 'MiniMax-M3',
      credential_configured: true,
      usage_type: 'MARKDOWN_POST_PROCESSING',
      priority: 20,
      is_default: false,
      enabled: true,
      healthy: false,
      health_message: 'timeout'
    })
  ]

  const updatedRows = toggleLlmMarkdownConfigRowEnabled(rows, 'llm_config_backup', false)

  assert.equal(updatedRows[0].enabled, true)
  assert.equal(updatedRows[1].enabled, false)
  assert.notEqual(updatedRows, rows)
})
