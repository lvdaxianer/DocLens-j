import test from 'node:test'
import assert from 'node:assert/strict'

import {
  OCR_NODE_TABLE_ACTION_BUTTON_COUNT,
  OCR_NODE_TABLE_ACTION_COLUMN_WIDTH,
  OCR_NODE_TABLE_ACTION_GAP,
  OCR_NODE_TABLE_ACTION_WIDTH,
  shouldStickOcrNodeTableActions
} from '../ocrNodeTableLayout.ts'

test('ocr node table action column fits all icon actions', () => {
  const requiredWidth = (OCR_NODE_TABLE_ACTION_BUTTON_COUNT * OCR_NODE_TABLE_ACTION_WIDTH)
    + ((OCR_NODE_TABLE_ACTION_BUTTON_COUNT - 1) * OCR_NODE_TABLE_ACTION_GAP)

  assert.equal(shouldStickOcrNodeTableActions(), true)
  assert.ok(OCR_NODE_TABLE_ACTION_COLUMN_WIDTH >= requiredWidth)
})
