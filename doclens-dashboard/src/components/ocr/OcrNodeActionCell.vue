<script setup lang="ts">
import { computed } from 'vue'
import { Eye, FlaskConical, Link2, Pencil, Power, Trash2 } from '@lucide/vue'
import { NButton, NIcon, NPopconfirm } from 'naive-ui'

import type { OcrNode, OcrNodeActionKind } from '@/types/ocrResources'
import { ocrNodeDeleteConfirmMessage } from '@/utils/destructiveConfirmMessages'
import { summarizeOcrNode } from '@/utils/ocrDisplayRules'

// OcrNodeActionCell 只负责单行操作按钮和动作中状态。
// 维护边界：
// - 详情按钮保持可用，方便用户在等待时查看节点。
// - 编辑、测试、连接、启停和删除会被行级动作锁定。
// - 删除确认仍由 Popconfirm 负责，避免误删。
// - 行级动作文案靠近按钮展示，避免用户误以为整页加载。
// - actionKind 由父级状态传入，本组件不直接发起远程请求。
// - 事件继续向父级冒泡，保持表格的数据流清晰。
// - loading 只绑定到当前动作按钮，减少视觉误导。
// - canReconnect 仍复用节点治理摘要，避免重复业务判断。
// - 文案映射集中在常量里，后续新增动作时只扩展一处。
const props = defineProps<{
  node: OcrNode
  actionKind?: OcrNodeActionKind
}>()

const emit = defineEmits<{
  edit: []
  delete: []
  detail: []
  test: []
  reconnect: []
  toggleEnabled: []
}>()

const NODE_ACTION_LABELS: Record<OcrNodeActionKind, string> = {
  delete: '删除中',
  test: '测试中',
  reconnect: '连接中',
  toggle: '切换中'
}

const governanceSummary = computed(() => summarizeOcrNode(props.node))
const deleteConfirmMessage = computed(() => ocrNodeDeleteConfirmMessage(props.node.name, props.node.id))

/**
 * 判断节点是否正在执行行级动作。
 *
 * @returns 是否忙碌
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
function isNodeBusy(): boolean {
  return Boolean(props.actionKind)
}

/**
 * 判断节点是否正在执行指定动作。
 *
 * @param actionKind - 节点动作类型
 * @returns 是否执行指定动作
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
function isNodeAction(actionKind: OcrNodeActionKind): boolean {
  return props.actionKind === actionKind
}

/**
 * 获取节点当前动作中文标签。
 *
 * @returns 当前动作中文标签
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
function nodeActionLabel(): string {
  if (props.actionKind) {
    // 存在行级动作时展示靠近操作按钮的状态文案。
    return NODE_ACTION_LABELS[props.actionKind]
  } else {
    // 无行级动作时返回空文本，避免占用额外视觉信息。
    return ''
  }
}
</script>

<template>
  <div class="ocr-node-action-cell">
    <NButton quaternary circle size="small" title="详情" @click="emit('detail')">
      <template #icon>
        <NIcon :component="Eye" />
      </template>
    </NButton>
    <span v-if="nodeActionLabel()" class="ocr-node-action-cell__status">
      {{ nodeActionLabel() }}
    </span>
    <NButton quaternary circle size="small" title="编辑" :disabled="isNodeBusy()" @click="emit('edit')">
      <template #icon>
        <NIcon :component="Pencil" />
      </template>
    </NButton>
    <NButton
      quaternary
      circle
      size="small"
      title="测试"
      :disabled="isNodeBusy()"
      :loading="isNodeAction('test')"
      @click="emit('test')"
    >
      <template #icon>
        <NIcon :component="FlaskConical" />
      </template>
    </NButton>
    <NButton
      v-if="governanceSummary.canReconnect"
      quaternary
      circle
      size="small"
      title="手动连接"
      :disabled="isNodeBusy()"
      :loading="isNodeAction('reconnect')"
      @click="emit('reconnect')"
    >
      <template #icon>
        <NIcon :component="Link2" />
      </template>
    </NButton>
    <NButton
      quaternary
      circle
      size="small"
      title="切换启用"
      :disabled="isNodeBusy()"
      :loading="isNodeAction('toggle')"
      @click="emit('toggleEnabled')"
    >
      <template #icon>
        <NIcon :component="Power" />
      </template>
    </NButton>
    <NPopconfirm @positive-click="emit('delete')">
      <template #trigger>
        <NButton
          quaternary
          circle
          size="small"
          title="删除"
          :disabled="isNodeBusy()"
          :loading="isNodeAction('delete')"
        >
          <template #icon>
            <NIcon :component="Trash2" />
          </template>
        </NButton>
      </template>
      {{ deleteConfirmMessage }}
    </NPopconfirm>
  </div>
</template>

<style scoped>
.ocr-node-action-cell {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--ocr-node-action-gap);
}

.ocr-node-action-cell__status {
  min-width: 42px;
  color: var(--ink-muted);
  font-size: 12px;
  white-space: nowrap;
}
</style>
