import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const DASHBOARD_SOURCE_ROOT = resolve(__dirname, '../../..')
const CODE_REVIEW_SPEC_FILE_LINE_LIMIT = 350
// 结构测试只覆盖本次拆分触达文件，避免把无关历史债务混入当前提交。
const STRUCTURE_CHECKED_FILES = [
  'views/BatchDetailView.vue',
  'components/dashboard/BatchSummaryStrip.vue',
  'components/dashboard/BatchDocumentTable.vue',
  'components/dashboard/batchDocumentTableColumns.ts',
  'components/dashboard/batchDocumentTableActions.ts',
  'components/dashboard/UploadDropzone.vue'
]

/**
 * 读取前端源码文件的真实行数，避免视图组件再次膨胀。
 *
 * 测试动机：
 * - `code-review-spec` 要求单文件不超过 350 行。
 * - 详情页拆分后，新增子组件也必须一起受约束。
 * - 使用真实文件读取，避免仅靠人工 shell 扫描。
 * - 断言消息包含 sourcePath，失败时能直接定位文件。
 * - 只读源码文件，不依赖浏览器或组件挂载。
 * - 行数口径包含尾部换行，比 `wc -l` 略严格。
 *
 * @param sourcePath 源码相对路径
 * @returns 源码文件行数
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function countSourceLines(sourcePath: string): number {
  return readFileSync(resolve(DASHBOARD_SOURCE_ROOT, sourcePath), 'utf8').split('\n').length
}

describe('dashboard code-review-spec structure', () => {
  it('keeps split batch detail components within the single-file line limit', () => {
    // 遍历本次拆出的文件，确保不会把大文件拆成另一个大文件。
    STRUCTURE_CHECKED_FILES.forEach((sourcePath) => {
      expect(countSourceLines(sourcePath), sourcePath).toBeLessThanOrEqual(CODE_REVIEW_SPEC_FILE_LINE_LIMIT)
    })
  })
})
