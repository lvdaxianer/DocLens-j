import test from 'node:test'
import assert from 'node:assert/strict'

import {
  createDefaultOcrNodeForm,
  createOcrNodePayload,
  fillOcrNodeFormFromNode,
  isOcrNodeFormSubmittable,
  shouldShowOcrModelSelect,
  validateOcrNodeCredentialEnvVar
} from '../ocrNodeFormRules.ts'

test('offline node form submits only host and port endpoint fields', () => {
  const form = createDefaultOcrNodeForm('paddle_ocr')

  form.name = '内网 Paddle 节点'
  form.host = '10.100.30.215'
  form.port = 8080

  assert.equal(isOcrNodeFormSubmittable(form), true)
  assert.deepEqual(createOcrNodePayload(form).node, {
    deployment_type: 'OFFLINE',
    name: '内网 Paddle 节点',
    host: '10.100.30.215',
    port: 8080,
    enabled: true,
    participate_global: true,
    weight: 50,
    max_concurrency: 10
  })
})

test('ollama offline node form requires provider model and submits generate channel', () => {
  const form = createDefaultOcrNodeForm('ollama')

  form.name = '内网 Ollama 节点'
  form.host = '10.100.30.215'
  form.port = 11434

  assert.equal(isOcrNodeFormSubmittable(form), false)

  form.providerModel = 'deepseek-ocr:latest'

  assert.equal(isOcrNodeFormSubmittable(form), true)
  assert.deepEqual(createOcrNodePayload(form).node, {
    deployment_type: 'OFFLINE',
    name: '内网 Ollama 节点',
    host: '10.100.30.215',
    port: 11434,
    channel_key: 'ollama',
    provider_model: 'deepseek-ocr:latest',
    enabled: true,
    participate_global: true,
    weight: 50,
    max_concurrency: 10
  })
})

test('online node form submits channel model and credential env var without endpoint fields', () => {
  const form = createDefaultOcrNodeForm('paddle_ocr')

  form.deploymentType = 'ONLINE'
  form.name = '阿里百炼 OCR'
  form.channelKey = 'aliyun_bailian_dashscope'
  form.providerModel = 'qwen-vl-ocr-2025-11-20'
  form.credentialEnvVar = 'DASHSCOPE_API_KEY'

  assert.equal(isOcrNodeFormSubmittable(form), true)
  assert.deepEqual(createOcrNodePayload(form).node, {
    deployment_type: 'ONLINE',
    name: '阿里百炼 OCR',
    channel_key: 'aliyun_bailian_dashscope',
    provider_model: 'qwen-vl-ocr-2025-11-20',
    credential_env_var: 'DASHSCOPE_API_KEY',
    enabled: true,
    participate_global: true,
    weight: 50,
    max_concurrency: 10
  })
})

test('online ocr node requires credential env var but ollama does not', () => {
  assert.equal(validateOcrNodeCredentialEnvVar('', 'online_ocr'), '请输入 API Key 环境变量名')
  assert.equal(validateOcrNodeCredentialEnvVar('', 'ollama'), '')
})

test('ocr node form defaults weight and max concurrency to product values', () => {
  const form = createDefaultOcrNodeForm('paddle_ocr')

  assert.equal(form.weight, 50)
  assert.equal(form.maxConcurrency, 10)
})

test('online node form hides ocr model select from user flow', () => {
  assert.equal(shouldShowOcrModelSelect('OFFLINE'), true)
  assert.equal(shouldShowOcrModelSelect('ONLINE'), false)
})

test('online node form does not require user selected ocr model', () => {
  const form = createDefaultOcrNodeForm('')

  form.deploymentType = 'ONLINE'
  form.name = '阿里百炼 OCR'
  form.channelKey = 'aliyun_bailian_dashscope'
  form.providerModel = 'qwen-vl-ocr-2025-11-20'
  form.credentialEnvVar = 'DASHSCOPE_API_KEY'

  assert.equal(isOcrNodeFormSubmittable(form), true)
  assert.equal(createOcrNodePayload(form).modelKey, 'paddle_ocr')
})

test('editing online node hydrates credential env var into form', () => {
  const form = fillOcrNodeFormFromNode({
    id: 'ocr_node_1',
    model_key: 'paddle_ocr',
    deployment_type: 'ONLINE',
    name: '阿里百炼 OCR',
    host: '',
    port: 0,
    channel_key: 'aliyun_bailian_dashscope',
    provider_model: 'qwen-vl-ocr-2025-11-20',
    credential_env_var: 'DASHSCOPE_API_KEY',
    credential_configured: true,
    enabled: true,
    participate_global: true,
    weight: 50,
    max_concurrency: 10,
    status: 'RECOVERING',
    inflight_images: 0,
    queued_images: 0,
    processed_images_today: 0,
    success_images: 0,
    failed_images: 0,
    avg_latency_ms: 0,
    p95_latency_ms: 0,
    failure_count: 0,
    recovery_success_count: 0,
    last_health_at: '',
    circuit_open_until: '',
    last_manual_recovery_at: '',
    last_error: ''
  })

  assert.equal(form.credentialEnvVar, 'DASHSCOPE_API_KEY')
  assert.equal(form.credentialConfigured, true)
  assert.equal(isOcrNodeFormSubmittable(form), true)
  assert.equal(createOcrNodePayload(form).node.credential_env_var, 'DASHSCOPE_API_KEY')
})
