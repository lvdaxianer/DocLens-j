import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const SOURCE_ROOT = resolve(__dirname, '../../..')
const BLUE_ACCENT_PATTERNS = [
  '#256d85',
  '#2f829c',
  '#1f5b70',
  '#dceff5',
  'rgba(37, 109, 133'
]

const CHECKED_FILES = [
  'components/dashboard/LatencyChart.vue',
  'components/dashboard/ProcessingRail.vue',
  'components/dashboard/UploadFilePicker.vue',
  'components/ocr/OcrModelList.vue'
]

/**
 * 读取源码并确认旧主色已经清干净。
 *
 * @param sourcePath 源码相对路径
 * @returns 源码文本
 * @author lvdaxianerplus
 * @date 2026-06-14
 */
function readSource(sourcePath: string): string {
  return readFileSync(resolve(SOURCE_ROOT, sourcePath), 'utf8')
}

describe('dashboard accent sweep', () => {
  it('removes the old blue brand accents from remaining dashboard components', () => {
    CHECKED_FILES.forEach((sourcePath) => {
      const source = readSource(sourcePath)
      BLUE_ACCENT_PATTERNS.forEach((pattern) => {
        expect(source, `${sourcePath} still contains ${pattern}`).not.toContain(pattern)
      })
    })
  })
})
