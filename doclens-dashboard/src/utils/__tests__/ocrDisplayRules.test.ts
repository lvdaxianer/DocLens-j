import test from 'node:test'
import assert from 'node:assert/strict'

import {
  displayLoadBalanceStrategy,
  displayRoutingMode,
  displayModelName,
  displayNodeName,
  displayNodeSubtitle,
  summarizeOcrNode
} from '../ocrDisplayRules.ts'

test('node display prefers alias over internal node id', () => {
  const node = { id: 'ocr_node_215', name: '财务发票节点' }

  assert.equal(displayNodeName(node), '财务发票节点')
})

test('node display falls back to id only when alias is blank', () => {
  const node = { id: 'ocr_node_215', name: ' ' }

  assert.equal(displayNodeName(node), 'ocr_node_215')
})

test('node subtitle prefers alias over internal id when alias exists', () => {
  const node = { id: 'ocr_node_215', name: '财务发票节点', modelKey: 'paddle_ocr', imageCount: 12 }

  assert.equal(displayNodeSubtitle(node), 'paddle_ocr · 财务发票节点 · 12 张图片')
})

test('node subtitle falls back to internal id only when alias is missing', () => {
  const node = { id: 'ocr_node_215', modelKey: 'paddle_ocr', imageCount: 12 }

  assert.equal(displayNodeSubtitle(node), 'paddle_ocr · ocr_node_215 · 12 张图片')
})

test('model display prefers readable model name', () => {
  const model = { modelKey: 'paddle_ocr', name: 'PaddleOCR' }

  assert.equal(displayModelName(model), 'PaddleOCR')
})

test('routing mode display hides internal default semantics from dashboard users', () => {
  assert.equal(displayRoutingMode('DEFAULT'), '全局负载均衡')
  assert.equal(displayRoutingMode('GLOBAL_LOAD_BALANCE'), '全局负载均衡')
  assert.equal(displayRoutingMode('MODEL_LOAD_BALANCE'), '指定 OCR')
  assert.equal(displayRoutingMode('SPECIFIC_NODE'), '指定节点')
})

test('load balance strategy display prefers product wording', () => {
  assert.equal(displayLoadBalanceStrategy('least-inflight'), '最少解析中图片')
  assert.equal(displayLoadBalanceStrategy('weighted-idle'), '加权空闲优先')
  assert.equal(displayLoadBalanceStrategy('weighted-random'), 'weighted-random')
  assert.equal(displayLoadBalanceStrategy(''), '-')
})

test('ocr node display exposes reconnect affordance and circuit window state', () => {
  const summary = summarizeOcrNode({
    status: 'DOWN',
    circuit_open_until: '2026-06-10T10:00:00+08:00',
    queued_images: 2
  })

  assert.equal(summary.canReconnect, true)
  assert.equal(summary.queueLabel, '排队 2 张')
  assert.equal(summary.circuitLabel, '熔断至 2026-06-10T10:00:00+08:00')
})
