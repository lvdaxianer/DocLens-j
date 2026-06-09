import { computed, reactive, shallowRef } from 'vue'
import type { MessageApi } from 'naive-ui'

import { fetchOcrGovernanceConfig, updateOcrGovernanceConfig } from '@/api/ocrGovernanceConfig'
import {
  createDefaultOcrGovernanceConfigForm,
  createOcrGovernanceConfigPayload,
  fillOcrGovernanceConfigFormFromResponse,
  isOcrGovernanceConfigFormSubmittable
} from '@/utils/ocrGovernanceConfigRules'

/**
 * 创建 OCR 全局治理配置面板状态和操作。
 *
 * @param message - Naive UI 消息 API
 * @returns OCR 全局治理配置状态和操作
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
export function useOcrGovernanceConfig(message: MessageApi) {
  const form = reactive(createDefaultOcrGovernanceConfigForm())
  const isLoading = shallowRef(false)
  const isSaving = shallowRef(false)
  const lastLoadedAt = shallowRef('')
  const errorMessage = shallowRef('')
  const canSubmit = computed(() => isOcrGovernanceConfigFormSubmittable(form))

  /**
   * 将未知异常转换为错误消息。
   *
   * @param error - 捕获到的异常
   * @returns 错误消息
   * @author lvdaxianerplus
   * @date 2026-06-10
   */
  function toErrorMessage(error: unknown): string {
    return error instanceof Error ? error.message : String(error)
  }

  /**
   * 刷新 OCR 全局治理配置。
   *
   * @returns 刷新完成信号
   * @author lvdaxianerplus
   * @date 2026-06-10
   */
  async function loadConfig(): Promise<void> {
    isLoading.value = true
    errorMessage.value = ''
    try {
      Object.assign(form, fillOcrGovernanceConfigFormFromResponse(await fetchOcrGovernanceConfig()))
      lastLoadedAt.value = new Date().toISOString()
    } catch (error) {
      errorMessage.value = toErrorMessage(error)
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 保存 OCR 全局治理配置。
   *
   * @returns 保存完成信号
   * @author lvdaxianerplus
   * @date 2026-06-10
   */
  async function saveConfig(): Promise<void> {
    if (!canSubmit.value) {
      message.warning('请填写大于 0 的治理参数')
      return
    }
    isSaving.value = true
    try {
      Object.assign(form, fillOcrGovernanceConfigFormFromResponse(
        await updateOcrGovernanceConfig(createOcrGovernanceConfigPayload(form))
      ))
      lastLoadedAt.value = new Date().toISOString()
      message.success('OCR 全局治理配置已保存')
    } catch (error) {
      message.error(toErrorMessage(error))
    } finally {
      isSaving.value = false
    }
  }

  return {
    form,
    isLoading,
    isSaving,
    lastLoadedAt,
    errorMessage,
    canSubmit,
    loadConfig,
    saveConfig
  }
}
