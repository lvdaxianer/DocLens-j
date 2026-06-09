import test from 'node:test'
import assert from 'node:assert/strict'

import {
  createDefaultUploadAdvancedOptions,
  createDefaultUploadOcrRouting,
  validateMetadataJson
} from '../uploadFormRules'

test('upload routing defaults to global load balance', () => {
  const routing = createDefaultUploadOcrRouting()

  assert.equal(routing.ocrRoutingMode, 'GLOBAL_LOAD_BALANCE')
  assert.equal(routing.ocrModelKey, '')
  assert.equal(routing.ocrNodeId, '')
  assert.equal(routing.ocrLoadBalanceStrategy, 'least-inflight')
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
