import { computed, shallowRef } from 'vue'
import type { MessageApi } from 'naive-ui'

import {
  createOcrNode,
  deleteOcrNode,
  fetchOcrModels,
  fetchOcrNodeCalls,
  fetchOcrNodes,
  testOcrNode,
  updateOcrNode,
  updateOcrNodeEnabled
} from '@/api/ocrResources'
import type { OcrModel, OcrNode, OcrNodeCall, OcrNodeSubmitPayload } from '@/types/ocrResources'

/**
 * 创建 OCR 资源页状态和操作。
 *
 * @param message - Naive UI 消息 API
 * @returns OCR 资源页状态和操作
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function useOcrResources(message: MessageApi) {
  const models = shallowRef<OcrModel[]>([])
  const nodes = shallowRef<OcrNode[]>([])
  const selectedModelKey = shallowRef('')
  const selectedNode = shallowRef<OcrNode | null>(null)
  const selectedNodeCalls = shallowRef<OcrNodeCall[]>([])
  const editingNode = shallowRef<OcrNode | null>(null)
  const isFormVisible = shallowRef(false)
  const isDetailVisible = shallowRef(false)
  const isLoadingModels = shallowRef(false)
  const isLoadingNodes = shallowRef(false)
  const isSavingNode = shallowRef(false)
  const lastUpdated = shallowRef('')
  const errorMessage = shallowRef('')

  const selectedModel = computed(() => models.value.find((model) => model.model_key === selectedModelKey.value))
  const hasModels = computed(() => models.value.length > 0)

  /**
   * 将未知异常转换为错误消息。
   *
   * @param error - 捕获到的异常
   * @returns 错误消息
   * @author lvdaxianerplus
   * @date 2026-06-09
   */
  function toErrorMessage(error: unknown): string {
    return error instanceof Error ? error.message : String(error)
  }

  /**
   * 确保存在当前选中的 OCR 模型。
   *
   * @returns 处理完成信号
   * @author lvdaxianerplus
   * @date 2026-06-09
   */
  function ensureSelectedModel(): void {
    const exists = models.value.some((model) => model.model_key === selectedModelKey.value)
    if (exists) {
      // 已选择模型仍然存在时保留当前选择。
    } else {
      selectedModelKey.value = models.value[0]?.model_key ?? ''
    }
  }

  /**
   * 刷新 OCR 模型列表。
   *
   * @returns 刷新完成信号
   * @author lvdaxianerplus
   * @date 2026-06-09
   */
  async function loadModels(): Promise<void> {
    isLoadingModels.value = true
    errorMessage.value = ''
    try {
      const response = await fetchOcrModels()
      models.value = response.items
      ensureSelectedModel()
      await loadNodes()
    } catch (error) {
      errorMessage.value = toErrorMessage(error)
    } finally {
      isLoadingModels.value = false
    }
  }

  /**
   * 刷新当前模型节点列表。
   *
   * @returns 刷新完成信号
   * @author lvdaxianerplus
   * @date 2026-06-09
   */
  async function loadNodes(): Promise<void> {
    if (!selectedModelKey.value) {
      nodes.value = []
      return
    }
    isLoadingNodes.value = true
    errorMessage.value = ''
    try {
      const response = await fetchOcrNodes(selectedModelKey.value)
      nodes.value = response.items
      lastUpdated.value = new Date().toISOString()
    } catch (error) {
      errorMessage.value = toErrorMessage(error)
    } finally {
      isLoadingNodes.value = false
    }
  }

  /**
   * 切换当前 OCR 模型。
   *
   * @param modelKey - OCR 模型标识
   * @returns 切换完成信号
   * @author lvdaxianerplus
   * @date 2026-06-09
   */
  async function selectModel(modelKey: string): Promise<void> {
    selectedModelKey.value = modelKey
    await loadNodes()
  }

  /**
   * 打开新增节点抽屉。
   *
   * @returns 打开完成信号
   * @author lvdaxianerplus
   * @date 2026-06-09
   */
  function openCreateDrawer(): void {
    editingNode.value = null
    isFormVisible.value = true
  }

  /**
   * 打开编辑节点抽屉。
   *
   * @param node - OCR 节点
   * @returns 打开完成信号
   * @author lvdaxianerplus
   * @date 2026-06-09
   */
  function openEditDrawer(node: OcrNode): void {
    editingNode.value = node
    isFormVisible.value = true
  }

  /**
   * 打开节点详情抽屉。
   *
   * @param node - OCR 节点
   * @returns 打开完成信号
   * @author lvdaxianerplus
   * @date 2026-06-09
   */
  async function openDetailDrawer(node: OcrNode): Promise<void> {
    selectedNode.value = node
    isDetailVisible.value = true
    selectedNodeCalls.value = []
    try {
      const response = await fetchOcrNodeCalls(node.id)
      selectedNodeCalls.value = response.items
    } catch (error) {
      message.error(toErrorMessage(error))
    }
  }

  /**
   * 关闭节点表单抽屉。
   *
   * @returns 关闭完成信号
   * @author lvdaxianerplus
   * @date 2026-06-09
   */
  function closeFormDrawer(): void {
    isFormVisible.value = false
  }

  /**
   * 关闭节点详情抽屉。
   *
   * @returns 关闭完成信号
   * @author lvdaxianerplus
   * @date 2026-06-09
   */
  function closeDetailDrawer(): void {
    isDetailVisible.value = false
    selectedNode.value = null
    selectedNodeCalls.value = []
  }

  /**
   * 发送节点保存请求。
   *
   * @param payload - 节点提交载荷
   * @returns 保存后的节点
   * @author lvdaxianerplus
   * @date 2026-06-09
   */
  function saveNodeRequest(payload: OcrNodeSubmitPayload): Promise<OcrNode> {
    if (editingNode.value) {
      return updateOcrNode(editingNode.value.id, payload.node)
    } else {
      return createOcrNode(payload.modelKey, payload.node)
    }
  }

  /**
   * 保存 OCR 节点配置。
   *
   * @param payload - 节点提交载荷
   * @returns 保存完成信号
   * @author lvdaxianerplus
   * @date 2026-06-09
   */
  async function saveNode(payload: OcrNodeSubmitPayload): Promise<void> {
    isSavingNode.value = true
    try {
      await saveNodeRequest(payload)
      selectedModelKey.value = payload.modelKey
      await loadModels()
      isFormVisible.value = false
      message.success('OCR 节点已保存')
    } catch (error) {
      message.error(toErrorMessage(error))
    } finally {
      isSavingNode.value = false
    }
  }

  /**
   * 删除 OCR 节点。
   *
   * @param node - OCR 节点
   * @returns 删除完成信号
   * @author lvdaxianerplus
   * @date 2026-06-09
   */
  async function removeNode(node: OcrNode): Promise<void> {
    try {
      await deleteOcrNode(node.id)
      await loadModels()
      message.success('OCR 节点已删除')
    } catch (error) {
      message.error(toErrorMessage(error))
    }
  }

  /**
   * 测试 OCR 节点健康。
   *
   * @param node - OCR 节点
   * @returns 测试完成信号
   * @author lvdaxianerplus
   * @date 2026-06-09
   */
  async function testNode(node: OcrNode): Promise<void> {
    try {
      const response = await testOcrNode(node.id)
      if (response.healthy) {
        message.success(response.message)
      } else {
        message.warning(response.message)
      }
    } catch (error) {
      message.error(toErrorMessage(error))
    }
  }

  /**
   * 切换 OCR 节点启用状态。
   *
   * @param node - OCR 节点
   * @param enabled - 是否启用
   * @returns 切换完成信号
   * @author lvdaxianerplus
   * @date 2026-06-09
   */
  async function toggleNodeEnabled(node: OcrNode, enabled: boolean): Promise<void> {
    try {
      await updateOcrNodeEnabled(node.id, enabled)
      await loadNodes()
      message.success(enabled ? 'OCR 节点已启用' : 'OCR 节点已停用')
    } catch (error) {
      message.error(toErrorMessage(error))
    }
  }

  return {
    models,
    nodes,
    selectedModelKey,
    selectedNode,
    selectedNodeCalls,
    editingNode,
    isFormVisible,
    isDetailVisible,
    isLoadingModels,
    isLoadingNodes,
    isSavingNode,
    lastUpdated,
    errorMessage,
    selectedModel,
    hasModels,
    loadModels,
    selectModel,
    openCreateDrawer,
    openEditDrawer,
    openDetailDrawer,
    closeFormDrawer,
    closeDetailDrawer,
    saveNode,
    removeNode,
    testNode,
    toggleNodeEnabled
  }
}
