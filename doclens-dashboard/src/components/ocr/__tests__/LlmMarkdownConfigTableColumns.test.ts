import { describe, expect, it, vi } from 'vitest'
import type { VNode } from 'vue'

import { createLlmMarkdownConfigColumns } from '../LlmMarkdownConfigTableColumns'
import type { LlmMarkdownConfigRow } from '@/utils/llmMarkdownConfigRules'

type ActionColumn = {
  key: string
  render: (row: LlmMarkdownConfigRow, index: number) => unknown
}

/**
 * LLM Markdown 配置表格列交互测试。
 *
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
describe('LlmMarkdownConfigTableColumns', () => {
  it('disables row actions while a config action is running', () => {
    const columns = createLlmMarkdownConfigColumns({
      actingId: 'llm-config-1',
      editConfig: vi.fn(),
      toggleEnabled: vi.fn(),
      makeDefault: vi.fn(),
      removeConfig: vi.fn()
    })
    const actionColumn = columns.find(isActionColumn) as ActionColumn | undefined
    const renderedActions = actionColumn?.render(row(), 0) as VNode[]

    expect(renderedActions[0].props?.disabled).toBe(true)
  })
})

/**
 * 判断列定义是否为操作列。
 *
 * @param column - 表格列定义
 * @returns 是否为操作列
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
function isActionColumn(column: unknown): column is ActionColumn {
  if (typeof column === 'object' && column !== null && 'key' in column && 'render' in column) {
    // 同时具备 key 和 render 时才可能是普通渲染列。
    return column.key === 'actions'
  } else {
    // selection/group 等特殊列没有本测试需要的渲染函数。
    return false
  }
}

/**
 * 创建 LLM Markdown 配置行测试数据。
 *
 * @returns LLM Markdown 配置行
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
function row(): LlmMarkdownConfigRow {
  return {
    id: 'llm-config-1',
    name: '默认配置',
    apiType: 'openai',
    url: 'https://api.example.com/v1',
    model: 'gpt-4o-mini',
    credentialEnvVar: 'MODEL_API_KEY',
    credentialConfigured: true,
    usageType: 'MARKDOWN_POST_PROCESSING',
    priority: 100,
    maxContextTokens: 16000,
    maxConcurrency: 1,
    requestIntervalMillis: 1000,
    defaultConfig: false,
    enabled: true,
    healthy: true,
    healthMessage: '',
    lastHealthAt: '2026-07-02T10:00:00+08:00',
    statusLabel: '可用'
  }
}
