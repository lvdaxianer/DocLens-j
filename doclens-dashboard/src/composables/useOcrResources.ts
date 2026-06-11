import { computed, shallowRef } from 'vue'
import type { MessageApi } from 'naive-ui'

import {
  fetchOcrModels,
  fetchOcrNodeCalls,
  fetchOcrNodes
} from '@/api/ocrResources'
import { useOcrNodeActions } from '@/composables/useOcrNodeActions'
import type { OcrModel, OcrNode, OcrNodeCall } from '@/types/ocrResources'

// useOcrResources 是 OCR 资源页的状态编排 composable。
// 维护边界：
// - 模型列表、节点列表和抽屉状态留在这里。
// - 节点保存、删除、测试、恢复和启停动作下沉到 useOcrNodeActions。
// - 本文件只直接调用列表查询和详情调用记录查询。
// - 错误消息转换保留在这里，供子动作 composable 复用。
// - selectedModelKey 是节点列表查询的主筛选条件。
// - selectedNode 是详情抽屉当前节点快照。
// - selectedNodeCalls 是详情抽屉内的调用记录。
// - editingNode 是表单抽屉当前编辑对象。
// - isFormVisible 和 isDetailVisible 分别控制两个抽屉。
// - isLoadingModels 和 isLoadingNodes 分离，避免页面整体误转圈。
// - isSavingNode 由动作 composable 修改，但状态归属仍在这里。
// - lastUpdated 只在节点列表刷新成功后更新。
// - errorMessage 只承载列表级错误，节点动作错误用 message 展示。
// - selectedModel 是派生数据，不额外存储副本。
// - hasModels 是模板便利状态，不作为业务判断来源。
// - loadModels 会联动 loadNodes，保持模型切换后的列表一致。
// - loadNodes 在没有模型 key 时清空节点列表，避免展示旧数据。
// - openDetailDrawer 会先清空调用记录，避免旧记录闪现。
// - closeDetailDrawer 清空节点和调用记录，释放详情上下文。
// - openCreateDrawer 会清空 editingNode，避免误带编辑数据。
// - openEditDrawer 只设置编辑目标，不做远程请求。
// - 动作 composable 通过上下文回调刷新列表，避免循环 import。
// - 返回对象保持原 API 名称，确保视图组件无需改动。
// - 不在这里做轮询，OCR 资源页由用户手动刷新。
// - 不在这里做排序，后端返回顺序作为当前展示顺序。
// - 不在这里做节点健康策略判断，后端负责状态语义。
// - 不在这里缓存调用记录，详情每次打开都拉取最新数据。
// - 新增节点动作时优先放入 useOcrNodeActions。
// - 新增页面状态时才扩展本 composable。
// - 这个拆分让本文件保持“状态 + 查询 + 抽屉”的单一职责。
/**
 * 创建 OCR 资源页状态和操作。
 *
 * @param message - Naive UI 消息 API
 * @returns OCR 资源页状态和操作
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function useOcrResources(message: MessageApi) {
  // 模型和节点数组使用 shallowRef，避免深度代理后端返回的大对象。
  const models = shallowRef<OcrModel[]>([])
  const nodes = shallowRef<OcrNode[]>([])
  // 当前模型 key 是节点列表查询和新增节点默认归属的依据。
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
  // hasModels 只服务模板展示，不替代 selectedModelKey 的业务判断。
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

  const {
    saveNode,
    removeNode,
    testNode,
    reconnectNode,
    toggleNodeEnabled
  } = useOcrNodeActions({
    // 动作 composable 需要这些状态引用来完成远程动作后的同步。
    message,
    nodes,
    selectedNode,
    selectedModelKey,
    editingNode,
    isFormVisible,
    isSavingNode,
    loadModels,
    loadNodes,
    toErrorMessage
  })

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
    reconnectNode,
    toggleNodeEnabled
  }
}
