import { computed, reactive, shallowRef } from 'vue'
import type { MessageApi } from 'naive-ui'

import {
  createLlmMarkdownConfig,
  deleteLlmMarkdownConfig,
  fetchLlmMarkdownConfig,
  makeDefaultLlmMarkdownConfig,
  testLlmMarkdownConfig,
  updateLlmMarkdownConfigById,
  updateLlmMarkdownConfigEnabled
} from '@/api/llmMarkdownConfig'
import type { LlmMarkdownConfigResponse } from '@/types/llmMarkdownConfig'
import {
  createDefaultLlmMarkdownConfigForm,
  createLlmMarkdownConfigPayload,
  createLlmMarkdownConfigRow,
  fillLlmMarkdownConfigFormFromResponse,
  llmConfigCapabilityHints,
  isLlmMarkdownConfigFormSubmittable,
  sanitizeLlmMarkdownConfigErrorMessage,
  toggleLlmMarkdownConfigRowEnabled,
  type LlmMarkdownConfigRow
} from '@/utils/llmMarkdownConfigRules'

const NEW_CONFIG_TITLE = '新增配置'

/**
 * 创建 LLM Markdown 配置面板状态和操作。
 *
 * @param message - Naive UI 消息 API
 * @returns LLM Markdown 配置状态和操作
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function useLlmMarkdownConfig(message: MessageApi) {
  const form = reactive(createDefaultLlmMarkdownConfigForm())
  const rows = shallowRef<LlmMarkdownConfigRow[]>([])
  const isDrawerVisible = shallowRef(false)
  const isLoading = shallowRef(false)
  const isSaving = shallowRef(false)
  const isTesting = shallowRef(false)
  const actingId = shallowRef('')
  const lastLoadedAt = shallowRef('')
  const errorMessage = shallowRef('')
  const editingTitle = shallowRef(NEW_CONFIG_TITLE)
  const canSubmit = computed(() => isLlmMarkdownConfigFormSubmittable(form))
  const hasConfigs = computed(() => rows.value.length > 0)
  const emptyStatus = computed(() => createLlmMarkdownConfigRow(undefined).statusLabel)
  const capabilityHints = computed(() => llmConfigCapabilityHints(form))

  function toErrorMessage(error: unknown): string {
    return sanitizeLlmMarkdownConfigErrorMessage(error instanceof Error ? error.message : String(error))
  }

  /**
   * 刷新 LLM Markdown 配置列表。
   *
   * @returns 刷新完成信号
   * @author lvdaxianerplus
   * @date 2026-06-09
   */
  async function loadConfig(): Promise<void> {
    isLoading.value = true
    errorMessage.value = ''
    try {
      rows.value = (await fetchLlmMarkdownConfig()).map(createLlmMarkdownConfigRow)
      lastLoadedAt.value = new Date().toISOString()
      resetForm()
    } catch (error) {
      errorMessage.value = toErrorMessage(error)
    } finally {
      isLoading.value = false
    }
  }

  function resetForm(): void {
    Object.assign(form, createDefaultLlmMarkdownConfigForm())
    editingTitle.value = NEW_CONFIG_TITLE
  }

  function editConfig(row: LlmMarkdownConfigRow): void {
    Object.assign(form, fillLlmMarkdownConfigFormFromResponse(rowToResponse(row)))
    editingTitle.value = row.name
    isDrawerVisible.value = true
  }

  /**
   * 打开新增配置抽屉。
   *
   * @returns 打开完成信号
   * @author lvdaxianerplus
   * @date 2026-06-19
   */
  function openCreateDrawer(): void {
    resetForm()
    isDrawerVisible.value = true
  }

  /**
   * 关闭 LLM Markdown 配置抽屉。
   *
   * @returns 关闭完成信号
   * @author lvdaxianerplus
   * @date 2026-06-19
   */
  function closeDrawer(): void {
    isDrawerVisible.value = false
  }

  /**
   * 保存 LLM Markdown 配置。
   *
   * @returns 保存完成信号
   * @author lvdaxianerplus
   * @date 2026-06-09
   */
  async function saveConfig(): Promise<void> {
    if (!canSubmit.value) {
      message.warning('请填写有效的 HTTP URL 和模型名称')
      return
    }
    await submitConfig()
  }

  /**
   * 提交 LLM Markdown 配置。
   *
   * @returns 提交完成信号
   * @author lvdaxianerplus
   * @date 2026-06-09
   */
  async function submitConfig(): Promise<void> {
    isSaving.value = true
    try {
      const response = await saveCurrentForm()
      applySavedConfig(response)
      message.success('LLM Markdown 配置已保存')
    } catch (error) {
      message.error(toErrorMessage(error))
    } finally {
      isSaving.value = false
    }
  }

  function saveCurrentForm(): Promise<LlmMarkdownConfigResponse> {
    if (form.id) {
      // 已有 ID 时更新指定配置。
      return updateLlmMarkdownConfigById(form.id, createLlmMarkdownConfigPayload(form))
    } else {
      // 无 ID 时创建新的多配置。
      return createLlmMarkdownConfig(createLlmMarkdownConfigPayload(form))
    }
  }

  function applySavedConfig(response: LlmMarkdownConfigResponse): void {
    const savedRow = createLlmMarkdownConfigRow(response)
    rows.value = upsertRow(rows.value, savedRow)
    Object.assign(form, fillLlmMarkdownConfigFormFromResponse(response))
    editingTitle.value = savedRow.name
    lastLoadedAt.value = new Date().toISOString()
  }

  /**
   * 切换 LLM Markdown 配置启停状态。
   *
   * @param row - 配置行
   * @returns 切换完成信号
   * @author lvdaxianerplus
   * @date 2026-06-12
   */
  async function toggleEnabled(row: LlmMarkdownConfigRow): Promise<void> {
    await runRowAction(row.id, async () => {
      const nextEnabled = !row.enabled
      rows.value = toggleLlmMarkdownConfigRowEnabled(rows.value, row.id, nextEnabled)
      const response = await updateLlmMarkdownConfigEnabled(row.id, nextEnabled)
      rows.value = upsertRow(rows.value, createLlmMarkdownConfigRow(response))
      message.success(nextEnabled ? 'LLM Markdown 已恢复使用' : 'LLM Markdown 已暂停使用')
    })
  }

  /**
   * 设置默认配置。
   *
   * @param row - 配置行
   * @returns 设置完成信号
   * @author lvdaxianerplus
   * @date 2026-06-12
   */
  async function makeDefault(row: LlmMarkdownConfigRow): Promise<void> {
    await runRowAction(row.id, async () => {
      await makeDefaultLlmMarkdownConfig(row.id)
      await loadConfig()
      message.success('默认配置已更新')
    })
  }

  /**
   * 删除配置。
   *
   * @param row - 配置行
   * @returns 删除完成信号
   * @author lvdaxianerplus
   * @date 2026-06-12
   */
  async function removeConfig(row: LlmMarkdownConfigRow): Promise<void> {
    await runRowAction(row.id, async () => {
      await deleteLlmMarkdownConfig(row.id)
      rows.value = rows.value.filter((current) => current.id !== row.id)
      if (form.id === row.id) {
        resetForm()
      }
      message.success('配置已删除')
    })
  }

  /**
   * 测试当前 LLM Markdown 配置是否可连通。
   *
   * @returns 测试完成信号
   * @author lvdaxianerplus
   * @date 2026-06-09
   */
  async function testConfig(): Promise<void> {
    if (!capabilityHints.value.canTest) {
      message.warning('请先填写有效的 LLM URL 和模型名称')
      return
    }
    isTesting.value = true
    try {
      const response = await testLlmMarkdownConfig(createLlmMarkdownConfigPayload(form))
      showTestResult(response.healthy, response.message)
    } catch (error) {
      message.error(toErrorMessage(error))
    } finally {
      isTesting.value = false
    }
  }

  function showTestResult(healthy: boolean, resultMessage: string): void {
    if (healthy) {
      // 连通性成功时提示成功消息。
      message.success(resultMessage)
    } else {
      // 连通性失败时提示可展示失败原因。
      message.warning(resultMessage)
    }
  }

  async function runRowAction(id: string, action: () => Promise<void>): Promise<void> {
    actingId.value = id
    try {
      await action()
    } catch (error) {
      await loadConfig()
      message.error(toErrorMessage(error))
    } finally {
      actingId.value = ''
    }
  }

  return {
    form,
    rows,
    isDrawerVisible,
    isLoading,
    isSaving,
    isTesting,
    actingId,
    lastLoadedAt,
    errorMessage,
    editingTitle,
    canSubmit,
    hasConfigs,
    emptyStatus,
    capabilityHints,
    loadConfig,
    openCreateDrawer,
    closeDrawer,
    resetForm,
    editConfig,
    saveConfig,
    toggleEnabled,
    makeDefault,
    removeConfig,
    testConfig
  }
}

/**
 * 插入或替换配置行。
 *
 * @param rows - 配置行列表
 * @param nextRow - 新配置行
 * @returns 更新后的配置行列表
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
function upsertRow(rows: LlmMarkdownConfigRow[], nextRow: LlmMarkdownConfigRow): LlmMarkdownConfigRow[] {
  const exists = rows.some((row) => row.id === nextRow.id)
  if (exists) {
    // 已存在时替换目标行。
    return rows.map((row) => row.id === nextRow.id ? nextRow : row)
  } else {
    // 新增时追加到列表尾部。
    return [...rows, nextRow]
  }
}

/**
 * 将配置行转换为响应对象。
 *
 * @param row - 配置行
 * @returns LLM Markdown 配置响应
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
function rowToResponse(row: LlmMarkdownConfigRow): LlmMarkdownConfigResponse {
  return {
    id: row.id,
    name: row.name,
    api_type: row.apiType,
    url: row.url,
    model: row.model,
    credential_env_var: row.credentialEnvVar,
    credential_configured: row.credentialConfigured,
    usage_type: row.usageType,
    priority: row.priority,
    max_context_tokens: row.maxContextTokens,
    max_concurrency: row.maxConcurrency,
    request_interval_millis: row.requestIntervalMillis,
    is_default: row.defaultConfig,
    enabled: row.enabled,
    healthy: row.healthy,
    health_message: row.healthMessage,
    last_health_at: row.lastHealthAt
  }
}
