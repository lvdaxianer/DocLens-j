import assert from 'node:assert/strict'
import test from 'node:test'

import { hasImageProgressStage, stageLabel } from '../formatters.ts'

/**
 * 阶段文案应区分文档调度与 OCR 页级排队。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
test('stageLabel distinguishes queued documents and queued OCR pages', () => {
  assert.equal(stageLabel('queued'), '待调度')
  assert.equal(stageLabel('ocr_queued'), 'OCR 排队中')
})

/**
 * OCR 页级排队阶段应展示图片页进度。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
test('hasImageProgressStage includes queued OCR page work', () => {
  assert.equal(hasImageProgressStage('ocr_queued'), true)
})
