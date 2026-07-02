import { h, type Component } from 'vue'
import {
  PauseCircle,
  Pencil,
  PlayCircle,
  Star,
  Trash2
} from '@lucide/vue'
import { NButton, NIcon, NPopconfirm, NTag, type DataTableColumns } from 'naive-ui'

import type { LlmMarkdownConfigRow } from '@/utils/llmMarkdownConfigRules'
import { llmConfigDeleteConfirmMessage } from '@/utils/destructiveConfirmMessages'
import { formatDateTime } from '@/utils/formatters'

export interface LlmMarkdownConfigTableActions {
  actingId: string
  editConfig: (row: LlmMarkdownConfigRow) => void
  toggleEnabled: (row: LlmMarkdownConfigRow) => void
  makeDefault: (row: LlmMarkdownConfigRow) => void
  removeConfig: (row: LlmMarkdownConfigRow) => void
}

/**
 * 创建 LLM Markdown 配置表格列。
 *
 * @param actions - 行操作集合
 * @returns 表格列定义
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
export function createLlmMarkdownConfigColumns(
  actions: LlmMarkdownConfigTableActions
): DataTableColumns<LlmMarkdownConfigRow> {
  return [
    { title: '名称', key: 'name', minWidth: 120 },
    { title: '用途', key: 'usageType', minWidth: 150 },
    { title: '协议', key: 'apiType', width: 130 },
    { title: '模型', key: 'model', minWidth: 150 },
    { title: '完整地址', key: 'url', minWidth: 260, ellipsis: { tooltip: true } },
    { title: '优先级', key: 'priority', width: 90 },
    { title: '治理', key: 'runtimeLimits', width: 190, render: renderRuntimeLimits },
    { title: '状态', key: 'status', width: 150, render: renderStatus },
    { title: '健康', key: 'healthy', width: 110, render: renderHealth },
    { title: '默认', key: 'defaultConfig', width: 90, render: renderDefault },
    { title: '最近心跳', key: 'lastHealthAt', width: 150, render: (row) => formatDateTime(row.lastHealthAt) },
    { title: '操作', key: 'actions', width: 310, render: (row) => renderActions(row, actions) }
  ]
}

/**
 * 渲染运行时治理参数。
 *
 * @param row - 配置行
 * @returns 治理参数文案
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
function renderRuntimeLimits(row: LlmMarkdownConfigRow) {
  return `${row.maxContextTokens} tokens / 并发 ${row.maxConcurrency} / ${row.requestIntervalMillis}ms`
}

/**
 * 渲染启停状态。
 *
 * @param row - 配置行
 * @returns 状态标签
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
function renderStatus(row: LlmMarkdownConfigRow) {
  return hTag(row.enabled ? 'success' : 'warning', row.statusLabel)
}

/**
 * 渲染健康状态。
 *
 * @param row - 配置行
 * @returns 健康标签
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
function renderHealth(row: LlmMarkdownConfigRow) {
  return hTag(row.healthy ? 'success' : 'error', row.healthy ? '正常' : '不可用')
}

/**
 * 渲染默认配置状态。
 *
 * @param row - 配置行
 * @returns 默认标签
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
function renderDefault(row: LlmMarkdownConfigRow) {
  return row.defaultConfig ? hTag('info', '默认') : '-'
}

/**
 * 渲染行操作。
 *
 * @param row - 配置行
 * @param actions - 行操作集合
 * @returns 操作按钮组
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
function renderActions(row: LlmMarkdownConfigRow, actions: LlmMarkdownConfigTableActions) {
  return [
    actionButton('编辑', Pencil, () => actions.editConfig(row), actions),
    actionButton(row.enabled ? '暂停' : '恢复', row.enabled ? PauseCircle : PlayCircle, () => actions.toggleEnabled(row), actions),
    actionButton('默认', Star, () => actions.makeDefault(row), actions, row.defaultConfig),
    deleteButton(row, actions)
  ]
}

/**
 * 创建表格标签渲染函数。
 *
 * @param type - 标签类型
 * @param label - 标签文案
 * @returns 标签渲染结果
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
function hTag(type: 'success' | 'warning' | 'error' | 'info', label: string) {
  return h(NTag, { size: 'small', type }, { default: () => label })
}

/**
 * 创建操作按钮。
 *
 * @param label - 按钮文案
 * @param icon - 按钮图标
 * @param onClick - 点击动作
 * @param actions - 行操作集合
 * @param disabled - 是否禁用
 * @returns 按钮渲染结果
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
function actionButton(
  label: string,
  icon: Component,
  onClick: () => void,
  actions: LlmMarkdownConfigTableActions,
  disabled = false
) {
  const isDisabled = isActionDisabled(actions, disabled)
  return h(NButton, {
    size: 'tiny',
    secondary: true,
    disabled: isDisabled,
    loading: actions.actingId !== '' && !disabled,
    onClick
  }, { icon: () => h(NIcon, { component: icon }), default: () => label })
}

/**
 * 判断配置行操作按钮是否禁用。
 *
 * @param actions - 行操作集合
 * @param disabled - 按钮自身是否禁用
 * @returns 是否禁用按钮
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
function isActionDisabled(actions: LlmMarkdownConfigTableActions, disabled: boolean): boolean {
  return disabled || actions.actingId !== ''
}

/**
 * 创建删除确认按钮。
 *
 * @param row - 配置行
 * @param actions - 行操作集合
 * @returns 删除按钮渲染结果
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
function deleteButton(row: LlmMarkdownConfigRow, actions: LlmMarkdownConfigTableActions) {
  return h(NPopconfirm, { onPositiveClick: () => actions.removeConfig(row) }, {
    trigger: () => actionButton('删除', Trash2, () => undefined, actions),
    default: () => llmConfigDeleteConfirmMessage(row.name, row.id)
  })
}
