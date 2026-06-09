import test from 'node:test'
import assert from 'node:assert/strict'

import {
  callbackContractHints,
  createDefaultUploadAdvancedOptions,
  createDefaultUploadOcrRouting,
  validateMetadataJson
} from '../uploadFormRules.ts'

test('upload routing defaults to global load balance', () => {
  const routing = createDefaultUploadOcrRouting()

  assert.equal(routing.ocrRoutingMode, 'GLOBAL_LOAD_BALANCE')
  assert.equal(routing.ocrModelKey, '')
  assert.equal(routing.ocrNodeId, '')
  assert.equal(routing.ocrLoadBalanceStrategy, 'weighted-idle')
})

test('advanced upload options expose only user-facing fields', () => {
  const options = createDefaultUploadAdvancedOptions()

  assert.deepEqual(Object.keys(options).sort(), ['callbackUrl', 'idempotencyKey', 'metadata'].sort())
  assert.equal(options.metadata, '{}')
})

test('metadata validation rejects malformed json before submit', () => {
  assert.equal(validateMetadataJson('{"source":"mail"}'), '')
  assert.match(validateMetadataJson('{"source":'), /元数据 JSON 格式不正确/)
})

test('callback hints declare fixed post body contract for receivers', () => {
  assert.deepEqual(callbackContractHints(), {
    method: 'POST',
    body: {
      meta: {},
      text: {},
      idempotency_key: ''
    },
    textSourceHint: '若启用且成功执行 LLM Markdown 后处理则返回 Markdown，否则返回 OCR 合并纯文本'
  })
})
