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
  sanitizeLlmMarkdownConfigErrorMessage,
  validateCredentialEnvVar,
  validateMaxConcurrency,
  validateMaxContextTokens,
  validateRequestIntervalMillis
} from '../llmMarkdownConfigRules.ts'

test('default llm markdown config form starts with env credential and runtime limits', () => {
  const form = createDefaultLlmMarkdownConfigForm()

  assert.deepEqual(form, {
    id: '',
    apiType: 'openai',
    url: '',
    model: '',
    credentialEnvVar: '',
    credentialConfigured: false,
    maxContextTokens: 16000,
    maxConcurrency: 1,
    requestIntervalMillis: 1000,
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
    credential_env_var: '',
    enabled: true,
    is_default: true,
    max_concurrency: 1,
    max_context_tokens: 16000,
    priority: 100,
    request_interval_millis: 1000,
    usage_type: 'MARKDOWN_POST_PROCESSING'
  })
})

test('llm markdown config response hydrates credential env var into edit form', () => {
  const form = fillLlmMarkdownConfigFormFromResponse({
    id: 'llm_config_main',
    name: '主配置',
    api_type: 'openai',
    url: 'https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions',
    model: 'qwen-vl-ocr-2025-11-20',
    credential_env_var: 'DASHSCOPE_API_KEY',
    credential_configured: true,
    usage_type: 'MARKDOWN_POST_PROCESSING',
    priority: 10,
    max_context_tokens: 32000,
    max_concurrency: 2,
    request_interval_millis: 1500,
    is_default: true,
    enabled: false,
    healthy: true,
    health_message: ''
  })

  assert.equal(form.credentialEnvVar, 'DASHSCOPE_API_KEY')
  assert.equal(form.credentialConfigured, true)
  assert.equal(form.enabled, false)
  assert.equal(form.id, 'llm_config_main')
  assert.equal(form.name, '主配置')
  assert.equal(form.priority, 10)
  assert.equal(form.maxContextTokens, 32000)
  assert.equal(form.maxConcurrency, 2)
  assert.equal(form.requestIntervalMillis, 1500)
  assert.equal(isLlmMarkdownConfigFormSubmittable(form), true)
  assert.equal(createLlmMarkdownConfigPayload(form).credential_env_var, 'DASHSCOPE_API_KEY')
})

test('llm markdown config payload uses credential env var and runtime limits', () => {
  const form = fillLlmMarkdownConfigFormFromResponse({
    id: 'llm_config_main',
    name: '主配置',
    api_type: 'openai',
    url: 'https://llm.example.com/v1/chat/completions',
    model: 'markdown-model',
    credential_env_var: 'MINIMAX_API_KEY',
    usage_type: 'MARKDOWN_POST_PROCESSING',
    priority: 10,
    max_context_tokens: 16000,
    max_concurrency: 1,
    request_interval_millis: 1000,
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
    credential_env_var: 'MINIMAX_API_KEY',
    enabled: true,
    is_default: true,
    max_concurrency: 1,
    max_context_tokens: 16000,
    name: '主配置',
    priority: 10,
    request_interval_millis: 1000,
    usage_type: 'MARKDOWN_POST_PROCESSING'
  })

  form.credentialEnvVar = '  DASHSCOPE_API_KEY  '

  assert.deepEqual(createLlmMarkdownConfigPayload(form), {
    api_type: 'openai',
    url: 'https://llm.example.com/v1/chat/completions',
    model: 'markdown-model',
    credential_env_var: 'DASHSCOPE_API_KEY',
    enabled: true,
    is_default: true,
    max_concurrency: 1,
    max_context_tokens: 16000,
    name: '主配置',
    priority: 10,
    request_interval_millis: 1000,
    usage_type: 'MARKDOWN_POST_PROCESSING'
  })
})

test('llm markdown config validators reject invalid runtime fields', () => {
  assert.equal(validateMaxContextTokens(undefined), '请输入最大上下文 Token 数')
  assert.equal(
    validateCredentialEnvVar('bad-name'),
    '环境变量名只能包含大写字母、数字和下划线，且不能以数字开头'
  )
  assert.equal(validateMaxConcurrency(0), '最大并发数必须大于等于 1')
  assert.equal(validateRequestIntervalMillis(-1), '请求间隔不能小于 0')
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
    credential_env_var: '',
    enabled: true,
    is_default: true,
    max_concurrency: 1,
    max_context_tokens: 16000,
    priority: 100,
    request_interval_millis: 1000,
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
  form.credentialEnvVar = 'MINIMAX_API_KEY'

  assert.deepEqual(createLlmMarkdownConfigPayload(form), {
    name: '备用配置',
    api_type: 'anthropic',
    url: 'https://api.example.com/anthropic/v1/messages',
    model: 'MiniMax-M3',
    credential_env_var: 'MINIMAX_API_KEY',
    enabled: true,
    is_default: false,
    max_concurrency: 1,
    max_context_tokens: 16000,
    priority: 20,
    request_interval_millis: 1000,
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
