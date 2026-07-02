import { h } from 'vue'
import type { DataTableColumns } from 'naive-ui'
import { NButton, NPopconfirm } from 'naive-ui'

import type { DocumentRow } from '@/types/dashboard'
import { documentDeleteConfirmMessage } from '@/utils/destructiveConfirmMessages'

const COMPLETED_STATUS = 'completed'
const FAILED_STATUS = 'failed'
const STALLED_STATUS = 'stalled'

/**
 * 文档表格动作配置，统一承接父组件传入的加载态与事件回调。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
export interface BatchDocumentTableActionOptions {
  resultLoading: boolean
  selectedResultDocumentId: string
  retryingDocumentId: string
  deletingDocumentId: string
  openResult: (document: DocumentRow) => void
  retryDocument: (documentId: string) => void
  deleteDocument: (documentId: string) => void
}

/**
 * 创建文档动作列集合。
 *
 * @param options 表格动作配置
 * @returns 查看、重试、删除三列
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
export function createBatchDocumentActionColumns(
  options: BatchDocumentTableActionOptions
): DataTableColumns<DocumentRow> {
  return [
    createResultActionColumn(options),
    createRetryActionColumn(options),
    createDeleteActionColumn(options)
  ]
}

/**
 * 判断文档当前状态是否允许显示重试操作。
 *
 * @param status 文档状态
 * @returns 是否可重试
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function canRetryDocument(status: string): boolean {
  return status === FAILED_STATUS || status === STALLED_STATUS
}

/**
 * 判断文档当前状态是否允许显示删除操作。
 *
 * @param status 文档状态
 * @returns 是否可删除
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function canDeleteDocument(status: string): boolean {
  return status === COMPLETED_STATUS || status === FAILED_STATUS || status === STALLED_STATUS
}

/**
 * 构建结果查看按钮列。
 *
 * @param options 表格动作配置
 * @returns 查看文本列配置
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function createResultActionColumn(
  options: BatchDocumentTableActionOptions
): DataTableColumns<DocumentRow>[number] {
  return {
    title: '操作',
    key: 'result_action',
    width: 110,
    render: (row) => h(NButton, resultButtonProps(row, options), { default: () => '查看文本' })
  }
}

/**
 * 生成结果查看按钮属性。
 *
 * @param row 文档行
 * @param options 表格动作配置
 * @returns 按钮属性
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function resultButtonProps(
  row: DocumentRow,
  options: BatchDocumentTableActionOptions
): Record<string, unknown> {
  return {
    size: 'small',
    secondary: true,
    disabled: row.status !== COMPLETED_STATUS,
    loading: options.resultLoading && options.selectedResultDocumentId === row.document_id,
    onClick: () => options.openResult(row)
  }
}

/**
 * 构建重试按钮列。
 *
 * @param options 表格动作配置
 * @returns 重试列配置
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function createRetryActionColumn(
  options: BatchDocumentTableActionOptions
): DataTableColumns<DocumentRow>[number] {
  return {
    title: '重试',
    key: 'retry_action',
    width: 90,
    fixed: 'right',
    render: (row) => h(NButton, retryButtonProps(row, options), { default: () => '重试' })
  }
}

/**
 * 生成重试按钮属性。
 *
 * @param row 文档行
 * @param options 表格动作配置
 * @returns 按钮属性
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function retryButtonProps(
  row: DocumentRow,
  options: BatchDocumentTableActionOptions
): Record<string, unknown> {
  return {
    size: 'small',
    secondary: true,
    disabled: !canRetryDocument(row.status),
    loading: options.retryingDocumentId === row.document_id,
    onClick: () => options.retryDocument(row.document_id)
  }
}

/**
 * 构建删除确认列。
 *
 * @param options 表格动作配置
 * @returns 删除列配置
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function createDeleteActionColumn(
  options: BatchDocumentTableActionOptions
): DataTableColumns<DocumentRow>[number] {
  return {
    title: '删除',
    key: 'delete_action',
    width: 100,
    fixed: 'right',
    render: (row) => h(NPopconfirm, deleteConfirmProps(row, options), deleteConfirmSlots(row, options))
  }
}

/**
 * 生成删除确认框属性。
 *
 * @param row 文档行
 * @param options 表格动作配置
 * @returns 确认框属性
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function deleteConfirmProps(
  row: DocumentRow,
  options: BatchDocumentTableActionOptions
): Record<string, unknown> {
  return {
    positiveText: '确认删除',
    negativeText: '取消',
    onPositiveClick: () => emitDeletableDocument(row, options)
  }
}

/**
 * 仅对可删除文档发送删除事件，等待中任务保持禁用。
 *
 * @param row 文档行
 * @param options 表格动作配置
 * @returns 删除事件发送结果
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function emitDeletableDocument(
  row: DocumentRow,
  options: BatchDocumentTableActionOptions
): void {
  if (canDeleteDocument(row.status)) {
    // 可删除状态才把动作交给父组件执行远程请求。
    options.deleteDocument(row.document_id)
  } else {
    // 不支持删除的状态忽略确认动作。
  }
}

/**
 * 生成删除确认框插槽。
 *
 * @param row 文档行
 * @param options 表格动作配置
 * @returns 确认框插槽
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function deleteConfirmSlots(
  row: DocumentRow,
  options: BatchDocumentTableActionOptions
): Record<string, () => unknown> {
  return {
    trigger: () => h(NButton, deleteButtonProps(row, options), { default: () => '删除' }),
    default: () => documentDeleteConfirmMessage(row.file_name, row.document_id)
  }
}

/**
 * 生成删除按钮属性。
 *
 * @param row 文档行
 * @param options 表格动作配置
 * @returns 删除按钮属性
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function deleteButtonProps(
  row: DocumentRow,
  options: BatchDocumentTableActionOptions
): Record<string, unknown> {
  return {
    size: 'small',
    secondary: true,
    disabled: !canDeleteDocument(row.status),
    loading: options.deletingDocumentId === row.document_id
  }
}
