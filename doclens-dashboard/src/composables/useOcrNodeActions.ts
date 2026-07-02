import type { Ref } from 'vue'
import type { MessageApi } from 'naive-ui'

import {
  createOcrNode,
  deleteOcrNode,
  reconnectOcrNode,
  testOcrNode,
  updateOcrNode,
  updateOcrNodeEnabled
} from '@/api/ocrResources'
import type { OcrNode, OcrNodeActionById, OcrNodeActionKind, OcrNodeSubmitPayload } from '@/types/ocrResources'

// useOcrNodeActions 只封装 OCR 节点的远程变更动作。
// 维护边界：
// - 不声明页面状态，所有状态都从 context 传入。
// - 不加载模型列表以外的数据，刷新逻辑通过 context 回调。
// - 不管理抽屉打开动作，只在保存成功后关闭表单抽屉。
// - 不暴露 saveNodeRequest，它只是保存动作内部选择 create/update。
// - 不直接修改 nodes，除 reconnect 后同步 selectedNode 快照。
// - 不吞异常，所有 catch 都通过 message 显示错误。
// - 不记录敏感信息，错误消息只来自 Error/message 转换。
// - 不处理列表级 errorMessage，节点动作错误属于即时反馈。
// - 不决定 selectedModelKey 初始值，只在保存成功后切到 payload model。
// - 不修改 selectedNodeCalls，调用记录由详情抽屉加载逻辑维护。
// - reconnect 后调用 loadNodes，保证列表和详情状态一致。
// - delete/save 后调用 loadModels，保证模型节点统计同步刷新。
// - enable/disable 后只调用 loadNodes，模型列表无需整体重载。
// - testNode 只展示健康测试结果，不刷新列表，保持原行为。
// - showReconnectMessage 单独提取，避免 reconnectNode 继续膨胀。
// - syncSelectedNode 单独提取，避免详情同步逻辑散落在动作中。
// - context 使用 Ref 类型，便于直接操作父 composable 状态。
// - context 回调避免本文件 import useOcrResources 造成循环依赖。
// - 新增远程节点动作时优先放入这里。
// - 新增页面选择/抽屉状态时不要放入这里。
// - 这个文件的目标是“远程动作 + 成功/失败反馈”。
// - 这个文件不负责 UI 排版，也不被组件直接渲染。
// - 这个拆分让资源页状态 composable 更容易阅读和测试。
export interface OcrNodeActionContext {
  message: MessageApi
  nodes: Ref<OcrNode[]>
  selectedNode: Ref<OcrNode | null>
  selectedModelKey: Ref<string>
  editingNode: Ref<OcrNode | null>
  isFormVisible: Ref<boolean>
  isSavingNode: Ref<boolean>
  nodeActionById: Ref<OcrNodeActionById>
  loadModels: () => Promise<void>
  loadNodes: () => Promise<void>
  toErrorMessage: (error: unknown) => string
}

/**
 * 创建 OCR 节点远程操作集合。
 *
 * @param context OCR 资源页动作上下文
 * @returns OCR 节点动作集合
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
export function useOcrNodeActions(context: OcrNodeActionContext) {
  /**
   * 发送节点保存请求。
   *
   * @param payload 节点提交载荷
   * @returns 保存后的节点
   * @author lvdaxianerplus
   * @date 2026-06-11
   */
  function saveNodeRequest(payload: OcrNodeSubmitPayload): Promise<OcrNode> {
    if (context.editingNode.value) {
      // 编辑态存在时走更新接口。
      return updateOcrNode(context.editingNode.value.id, payload.node)
    } else {
      // 新增态没有 editingNode，使用当前 payload 的模型归属创建节点。
      return createOcrNode(payload.modelKey, payload.node)
    }
  }

  /**
   * 判断节点是否已有行级远程动作在执行。
   *
   * @param node OCR 节点
   * @returns 是否忙碌
   * @author lvdaxianer@yeah.net
   * @date 2026-07-02
   */
  function isNodeBusy(node: OcrNode): boolean {
    return Boolean(context.nodeActionById.value[node.id])
  }

  /**
   * 设置节点行级远程动作状态。
   *
   * @param node OCR 节点
   * @param actionKind 节点动作类型
   * @returns 设置完成信号
   * @author lvdaxianer@yeah.net
   * @date 2026-07-02
   */
  function setNodeAction(node: OcrNode, actionKind: OcrNodeActionKind): void {
    context.nodeActionById.value = {
      ...context.nodeActionById.value,
      [node.id]: actionKind
    }
  }

  /**
   * 清理节点行级远程动作状态。
   *
   * @param node OCR 节点
   * @returns 清理完成信号
   * @author lvdaxianer@yeah.net
   * @date 2026-07-02
   */
  function clearNodeAction(node: OcrNode): void {
    const { [node.id]: _currentAction, ...restActions } = context.nodeActionById.value
    context.nodeActionById.value = restActions
  }

  /**
   * 尝试进入节点行级远程动作。
   *
   * @param node OCR 节点
   * @param actionKind 节点动作类型
   * @returns 是否成功进入动作
   * @author lvdaxianer@yeah.net
   * @date 2026-07-02
   */
  function beginNodeAction(node: OcrNode, actionKind: OcrNodeActionKind): boolean {
    if (isNodeBusy(node)) {
      // 当前节点已有动作执行中，忽略重复远程请求。
      return false
    } else {
      // 当前节点空闲时记录本次动作，供 UI 展示 loading 状态。
      setNodeAction(node, actionKind)
      return true
    }
  }

  /**
   * 保存 OCR 节点配置。
   *
   * @param payload 节点提交载荷
   * @returns 保存完成信号
   * @author lvdaxianerplus
   * @date 2026-06-11
   */
  async function saveNode(payload: OcrNodeSubmitPayload): Promise<void> {
    context.isSavingNode.value = true
    try {
      // 保存接口内部会根据 editingNode 自动选择新增或更新。
      await saveNodeRequest(payload)
      context.selectedModelKey.value = payload.modelKey
      await context.loadModels()
      context.isFormVisible.value = false
      context.message.success('OCR 节点已保存')
    } catch (error) {
      context.message.error(context.toErrorMessage(error))
    } finally {
      context.isSavingNode.value = false
    }
  }

  /**
   * 删除 OCR 节点。
   *
   * @param node OCR 节点
   * @returns 删除完成信号
   * @author lvdaxianerplus
   * @date 2026-06-11
   */
  async function removeNode(node: OcrNode): Promise<void> {
    if (!beginNodeAction(node, 'delete')) {
      // 当前节点忙碌时删除动作已经被统一拦截。
      return
    } else {
      // 当前节点进入删除动作后继续调用后端。
    }
    try {
      await deleteOcrNode(node.id)
      await context.loadModels()
      context.message.success('OCR 节点已删除')
    } catch (error) {
      context.message.error(context.toErrorMessage(error))
    } finally {
      clearNodeAction(node)
    }
  }

  /**
   * 测试 OCR 节点健康。
   *
   * @param node OCR 节点
   * @returns 测试完成信号
   * @author lvdaxianerplus
   * @date 2026-06-11
   */
  async function testNode(node: OcrNode): Promise<void> {
    if (!beginNodeAction(node, 'test')) {
      // 当前节点忙碌时测试动作已经被统一拦截。
      return
    } else {
      // 当前节点进入测试动作后继续调用后端。
    }
    try {
      const response = await testOcrNode(node.id)
      if (response.healthy) {
        // 健康节点使用成功提示。
        context.message.success(response.message)
      } else {
        // 非健康但接口成功时使用警告提示。
        context.message.warning(response.message)
      }
    } catch (error) {
      context.message.error(context.toErrorMessage(error))
    } finally {
      clearNodeAction(node)
    }
  }

  /**
   * 触发 OCR 节点手动恢复。
   *
   * @param node OCR 节点
   * @returns 手动恢复完成信号
   * @author lvdaxianerplus
   * @date 2026-06-11
   */
  async function reconnectNode(node: OcrNode): Promise<void> {
    if (!beginNodeAction(node, 'reconnect')) {
      // 当前节点忙碌时连接动作已经被统一拦截。
      return
    } else {
      // 当前节点进入连接动作后继续调用后端。
    }
    try {
      const response = await reconnectOcrNode(node.id)
      await context.loadNodes()
      syncSelectedNode(node)
      showReconnectMessage(response.healthy, response.attempts, response.status)
    } catch (error) {
      context.message.error(context.toErrorMessage(error))
    } finally {
      clearNodeAction(node)
    }
  }

  /**
   * 同步当前详情抽屉中的节点快照。
   *
   * @param node 原始节点
   * @returns 同步完成信号
   * @author lvdaxianerplus
   * @date 2026-06-11
   */
  function syncSelectedNode(node: OcrNode): void {
    if (context.selectedNode.value?.id === node.id) {
      const refreshedNode = context.nodes.value.find((item) => item.id === node.id) ?? node
      context.selectedNode.value = refreshedNode
    } else {
      // 当前未打开该节点详情时无需同步抽屉内状态。
    }
  }

  /**
   * 展示手动恢复结果消息。
   *
   * @param healthy 节点是否健康
   * @param attempts 尝试次数
   * @param status 节点状态
   * @returns 消息展示完成信号
   * @author lvdaxianerplus
   * @date 2026-06-11
   */
  function showReconnectMessage(healthy: boolean, attempts: number, status: string): void {
    if (healthy) {
      // 恢复后健康时明确告诉用户连接已经成功。
      context.message.success(`手动连接成功，已尝试 ${attempts} 次`)
    } else {
      // 恢复后仍异常时保留后端状态，便于继续排查。
      context.message.warning(`手动连接已执行 ${attempts} 次，当前状态：${status}`)
    }
  }

  /**
   * 切换 OCR 节点启用状态。
   *
   * @param node OCR 节点
   * @param enabled 是否启用
   * @returns 切换完成信号
   * @author lvdaxianerplus
   * @date 2026-06-11
   */
  async function toggleNodeEnabled(node: OcrNode, enabled: boolean): Promise<void> {
    if (!beginNodeAction(node, 'toggle')) {
      // 当前节点忙碌时启停动作已经被统一拦截。
      return
    } else {
      // 当前节点进入启停动作后继续调用后端。
    }
    try {
      await updateOcrNodeEnabled(node.id, enabled)
      await context.loadNodes()
      context.message.success(enabled ? 'OCR 节点已启用' : 'OCR 节点已停用')
    } catch (error) {
      context.message.error(context.toErrorMessage(error))
    } finally {
      clearNodeAction(node)
    }
  }

  return {
    saveNode,
    removeNode,
    testNode,
    reconnectNode,
    toggleNodeEnabled
  }
}
