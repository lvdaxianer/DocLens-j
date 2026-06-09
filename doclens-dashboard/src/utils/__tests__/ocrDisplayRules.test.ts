import test from 'node:test'
import assert from 'node:assert/strict'

import {
  displayModelName,
  displayNodeName,
  displayNodeSubtitle
} from '../ocrDisplayRules'

test('node display prefers alias over internal node id', () => {
  const node = { id: 'ocr_node_215', name: '财务发票节点' }

  assert.equal(displayNodeName(node), '财务发票节点')
})

test('node display falls back to id only when alias is blank', () => {
  const node = { id: 'ocr_node_215', name: ' ' }

  assert.equal(displayNodeName(node), 'ocr_node_215')
})

test('node subtitle keeps internal id secondary', () => {
  const node = { id: 'ocr_node_215', modelKey: 'paddle_ocr', imageCount: 12 }

  assert.equal(displayNodeSubtitle(node), 'paddle_ocr · ocr_node_215 · 12 张图片')
})

test('model display prefers readable model name', () => {
  const model = { modelKey: 'paddle_ocr', name: 'PaddleOCR' }

  assert.equal(displayModelName(model), 'PaddleOCR')
})
