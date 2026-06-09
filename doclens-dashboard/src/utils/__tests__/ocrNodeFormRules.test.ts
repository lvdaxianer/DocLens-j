import test from 'node:test'
import assert from 'node:assert/strict'

import {
  createDefaultOcrNodeForm,
  createOcrNodePayload,
  fillOcrNodeFormFromNode,
  isOcrNodeFormSubmittable,
  shouldShowOcrModelSelect
} from '../ocrNodeFormRules'

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
    weight: 100,
    max_concurrency: 4
  })
})

test('online node form submits channel model and api key without endpoint fields', () => {
  const form = createDefaultOcrNodeForm('paddle_ocr')

  form.deploymentType = 'ONLINE'
  form.name = '阿里百炼 OCR'
  form.channelKey = 'aliyun_bailian_dashscope'
  form.providerModel = 'qwen-vl-ocr-2025-11-20'
  form.apiKey = 'sk-secret'

  assert.equal(isOcrNodeFormSubmittable(form), true)
  assert.deepEqual(createOcrNodePayload(form).node, {
    deployment_type: 'ONLINE',
    name: '阿里百炼 OCR',
    channel_key: 'aliyun_bailian_dashscope',
    provider_model: 'qwen-vl-ocr-2025-11-20',
    api_key: 'sk-secret',
    enabled: true,
    participate_global: true,
    weight: 100,
    max_concurrency: 4
  })
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
  form.apiKey = 'sk-secret'

  assert.equal(isOcrNodeFormSubmittable(form), true)
  assert.equal(createOcrNodePayload(form).modelKey, 'paddle_ocr')
})

test('editing online node never hydrates existing api key into form', () => {
  const form = fillOcrNodeFormFromNode({
    id: 'ocr_node_1',
    model_key: 'paddle_ocr',
    deployment_type: 'ONLINE',
    name: '阿里百炼 OCR',
    host: '',
    port: 0,
    channel_key: 'aliyun_bailian_dashscope',
    provider_model: 'qwen-vl-ocr-2025-11-20',
    credential_configured: true,
    enabled: true,
    participate_global: true,
    weight: 100,
    max_concurrency: 4,
    status: 'RECOVERING',
    inflight_images: 0,
    queued_images: 0,
    processed_images_today: 0,
    success_images: 0,
    failed_images: 0,
    avg_latency_ms: 0,
    p95_latency_ms: 0,
    last_health_at: '',
    last_error: ''
  })

  assert.equal(form.apiKey, '')
  assert.equal(form.credentialConfigured, true)
  assert.equal(isOcrNodeFormSubmittable(form), true)
  assert.equal('api_key' in createOcrNodePayload(form).node, false)
})
