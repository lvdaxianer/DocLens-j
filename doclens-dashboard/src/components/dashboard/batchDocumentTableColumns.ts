import { h } from 'vue'
import type { DataTableColumns } from 'naive-ui'
import { NProgress } from 'naive-ui'

import {
  createBatchDocumentActionColumns,
  type BatchDocumentTableActionOptions
} from '@/components/dashboard/batchDocumentTableActions'
import StatusTag from '@/components/dashboard/StatusTag.vue'
import type { DocumentRow } from '@/types/dashboard'
import {
  fileTypeLabel,
  formatDateTime,
  formatDuration,
  formatImageProgress,
  formatNumber,
  formatPercent,
  hasImageProgressStage,
  stageLabel
} from '@/utils/formatters'

const COMPLETED_STATUS = 'completed'
const FAILED_STATUS = 'failed'
const PROGRESS_BAR_HEIGHT = 8
const FILE_NAME_COLUMN_CLASS = 'document-name'
const STAGE_LABEL_COLUMN_CLASS = 'stage-label'
const CHUNK_COUNT_COLUMN_CLASS = 'chunk-count'

/**
 * 文档表格动作配置，统一承接父组件传入的加载态与事件回调。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
export type BatchDocumentTableColumnOptions = BatchDocumentTableActionOptions

/**
 * 创建文档表格列配置，保持 Vue 组件只负责模板接线。
 *
 * @param options 表格动作与加载态配置
 * @returns Naive UI 文档表格列
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
export function createBatchDocumentColumns(
  options: BatchDocumentTableColumnOptions
): DataTableColumns<DocumentRow> {
  return [
    createFileNameColumn(),
    createTypeColumn(),
    createStatusColumn(),
    createStageColumn(),
    createChunkCountColumn(),
    createImageProgressColumn(),
    createProgressColumn(),
    createDurationColumn(),
    createUpdatedAtColumn(),
    ...createBatchDocumentActionColumns(options)
  ]
}

/**
 * 构建文档名称列，集中控制长文件名展示。
 *
 * @returns 文件名称列配置
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function createFileNameColumn(): DataTableColumns<DocumentRow>[number] {
  return {
    title: '文件',
    key: 'file_name',
    minWidth: 190,
    render: (row) => h('strong', { class: FILE_NAME_COLUMN_CLASS }, row.file_name)
  }
}

/**
 * 构建文件类型列，统一使用产品化类型文案。
 *
 * @returns 文件类型列配置
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function createTypeColumn(): DataTableColumns<DocumentRow>[number] {
  return { title: '类型', key: 'file_type', width: 100, render: (row) => fileTypeLabel(row.file_type) }
}

/**
 * 构建状态列，复用统一状态标签组件。
 *
 * @returns 状态列配置
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function createStatusColumn(): DataTableColumns<DocumentRow>[number] {
  return { title: '状态', key: 'status', width: 110, render: (row) => h(StatusTag, { status: row.status }) }
}

/**
 * 构建处理阶段列，展示面向用户的阶段文案。
 *
 * @returns 阶段列配置
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function createStageColumn(): DataTableColumns<DocumentRow>[number] {
  return {
    title: '阶段',
    key: 'stage',
    width: 150,
    render: (row) => h('span', { class: STAGE_LABEL_COLUMN_CLASS }, stageLabel(row.stage))
  }
}

/**
 * 构建 chunk 数列，帮助排查分块文档的实际切分数量。
 *
 * @returns 分块数量列配置
 * @author lvdaxianerplus
 * @date 2026-06-19
 */
function createChunkCountColumn(): DataTableColumns<DocumentRow>[number] {
  return {
    title: '分块',
    key: 'llm_chunk_count',
    width: 90,
    render: (row) => h('span', { class: CHUNK_COUNT_COLUMN_CLASS }, formatChunkCount(row.llm_chunk_count))
  }
}

/**
 * 构建图片进度列，仅在图片阶段展示页进度。
 *
 * @returns 图片进度列配置
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function createImageProgressColumn(): DataTableColumns<DocumentRow>[number] {
  return {
    title: '图片进度',
    key: 'image_progress',
    width: 130,
    render: (row) => hasImageProgressStage(row.stage) ? formatImageProgress(row.current_page, row.total_pages) : '-'
  }
}

/**
 * 构建进度列，按失败状态切换进度条语义色。
 *
 * @returns 进度列配置
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function createProgressColumn(): DataTableColumns<DocumentRow>[number] {
  return {
    title: '进度',
    key: 'progress_percent',
    width: 190,
    render: (row) => h(NProgress, progressBarProps(row), progressBarSlots(row))
  }
}

/**
 * 构建耗时列，统一使用耗时格式化函数。
 *
 * @returns 耗时列配置
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function createDurationColumn(): DataTableColumns<DocumentRow>[number] {
  return { title: '耗时', key: 'duration_ms', width: 100, render: (row) => formatDuration(row.duration_ms) }
}

/**
 * 构建更新时间列，统一展示本地时间格式。
 *
 * @returns 更新时间列配置
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function createUpdatedAtColumn(): DataTableColumns<DocumentRow>[number] {
  return { title: '更新时间', key: 'updated_at', width: 150, render: (row) => formatDateTime(row.updated_at) }
}

/**
 * 格式化 chunk 数展示。
 *
 * @param chunkCount chunk 数
 * @returns 展示文本
 * @author lvdaxianerplus
 * @date 2026-06-19
 */
function formatChunkCount(chunkCount: number): string {
  return chunkCount > 0 ? `${formatNumber(chunkCount)} 个` : '-'
}

/**
 * 生成进度条属性，避免列定义中混入状态判断细节。
 *
 * @param row 文档行
 * @returns 进度条属性
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function progressBarProps(row: DocumentRow): Record<string, unknown> {
  return {
    percentage: row.progress_percent,
    height: PROGRESS_BAR_HEIGHT,
    indicatorPlacement: 'outside',
    status: row.status === FAILED_STATUS ? 'error' : 'success'
  }
}

/**
 * 生成进度条内容插槽，统一展示百分比格式。
 *
 * @param row 文档行
 * @returns 进度条插槽
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function progressBarSlots(row: DocumentRow): Record<string, () => string> {
  return {
    default: () => formatPercent(row.progress_percent)
  }
}
