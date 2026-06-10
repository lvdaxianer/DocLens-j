import { computed, reactive, shallowRef } from 'vue'
import type { MessageApi } from 'naive-ui'

import { fetchLlmMarkdownConfig, testLlmMarkdownConfig, updateLlmMarkdownConfig } from '@/api/llmMarkdownConfig'
import {
  createDefaultLlmMarkdownConfigForm,
  createLlmMarkdownConfigPayload,
  fillLlmMarkdownConfigFormFromResponse,
  llmConfigCapabilityHints,
  isLlmMarkdownConfigFormSubmittable,
  sanitizeLlmMarkdownConfigErrorMessage
} from '@/utils/llmMarkdownConfigRules'

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
  const isLoading = shallowRef(false)
  const isSaving = shallowRef(false)
  const isTesting = shallowRef(false)
  const lastLoadedAt = shallowRef('')
  const errorMessage = shallowRef('')
  const canSubmit = computed(() => isLlmMarkdownConfigFormSubmittable(form))
  const isConfigured = computed(() => form.url.trim() !== '' && form.model.trim() !== '')
  const capabilityHints = computed(() => llmConfigCapabilityHints(form))

  /**
   * 将未知异常转换为错误消息。
   *
   * @param error - 捕获到的异常
   * @returns 错误消息
   * @author lvdaxianerplus
   * @date 2026-06-09
   */
  function toErrorMessage(error: unknown): string {
    return sanitizeLlmMarkdownConfigErrorMessage(error instanceof Error ? error.message : String(error))
  }

  /**
   * 刷新 LLM Markdown 配置。
   *
   * @returns 刷新完成信号
   * @author lvdaxianerplus
   * @date 2026-06-09
   */
  async function loadConfig(): Promise<void> {
    isLoading.value = true
    errorMessage.value = ''
    try {
      Object.assign(form, fillLlmMarkdownConfigFormFromResponse(await fetchLlmMarkdownConfig()))
      lastLoadedAt.value = new Date().toISOString()
    } catch (error) {
      errorMessage.value = toErrorMessage(error)
    } finally {
      isLoading.value = false
    }
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
      Object.assign(form, fillLlmMarkdownConfigFormFromResponse(
        await updateLlmMarkdownConfig(createLlmMarkdownConfigPayload(form))
      ))
      lastLoadedAt.value = new Date().toISOString()
      message.success('LLM Markdown 配置已保存')
    } catch (error) {
      message.error(toErrorMessage(error))
    } finally {
      isSaving.value = false
    }
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
      if (response.healthy) {
        message.success(response.message)
      } else {
        message.warning(response.message)
      }
    } catch (error) {
      message.error(toErrorMessage(error))
    } finally {
      isTesting.value = false
    }
  }

  return {
    form,
    isLoading,
    isSaving,
    isTesting,
    lastLoadedAt,
    errorMessage,
    canSubmit,
    isConfigured,
    capabilityHints,
    loadConfig,
    saveConfig,
    testConfig
  }
}
