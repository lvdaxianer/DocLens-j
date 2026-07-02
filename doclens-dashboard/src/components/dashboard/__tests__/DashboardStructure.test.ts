import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const DASHBOARD_SOURCE_ROOT = resolve(__dirname, '../../..')
const CODE_REVIEW_SPEC_FILE_LINE_LIMIT = 350
// 结构测试只覆盖本次拆分触达文件，避免把无关历史债务混入当前提交。
// 每次拆分新增的文件也必须加入这里，防止“大文件拆成另一个大文件”。
// 这个列表不扫描全仓库，是为了让每个原子任务只承担自己的范围。
// 剩余历史超限文件会通过后续 Task 逐个加入并消化。
// 生产构建产物不加入这里，因为它们是 Vite 输出。
// 测试里的失败信息会展示 sourcePath，便于下一位维护者定位。
// 路径基于 src 根目录，避免跨平台绝对路径差异。
// 文件顺序按任务推进顺序排列，方便对照计划文档。
const STRUCTURE_CHECKED_FILES = [
  'views/BatchDetailView.vue',
  'components/dashboard/BatchSummaryStrip.vue',
  'components/dashboard/BatchIntakeInfoPanel.vue',
  'components/dashboard/BatchCallbackJobsPanel.vue',
  'components/dashboard/BatchDocumentTable.vue',
  'components/dashboard/batchDocumentTableColumns.ts',
  'components/dashboard/batchDocumentTableActions.ts',
  'components/dashboard/UploadDropzone.vue',
  'components/dashboard/UploadFilePicker.vue',
  'components/dashboard/UploadFileList.vue',
  'components/dashboard/PrincipalPartitionStatus.vue',
  'composables/useOcrResources.ts',
  'composables/useOcrNodeActions.ts'
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
