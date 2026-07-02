<script setup lang="ts">
import { NSwitch } from 'naive-ui'

import type { OcrNodeActionKind } from '@/types/ocrResources'

// OcrNodeEnabledSwitch 只负责节点启停开关的动作中表现。
// 维护边界：
// - value 由父组件传入，组件不持有节点状态副本。
// - actionKind 只用于 disabled/loading，不改变开关值。
// - update:value 继续上抛给父组件，遵守 props down/events up。
// - toggle 动作执行中展示 loading，其他动作执行中只禁用。
// - 组件不访问远程接口，重复请求保护由 composable 兜底。
const props = defineProps<{
  value: boolean
  actionKind?: OcrNodeActionKind
}>()

const emit = defineEmits<{
  'update:value': [value: boolean]
}>()

/**
 * 判断节点是否已有行级动作执行中。
 *
 * @returns 是否忙碌
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
function isNodeBusy(): boolean {
  return Boolean(props.actionKind)
}

/**
 * 判断当前动作是否为启停切换。
 *
 * @returns 是否正在切换
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
function isToggleRunning(): boolean {
  return props.actionKind === 'toggle'
}
</script>

<template>
  <NSwitch
    size="small"
    :value="value"
    :disabled="isNodeBusy()"
    :loading="isToggleRunning()"
    @update:value="emit('update:value', $event)"
  >
    <template #checked>
      开
    </template>
    <template #unchecked>
      关
    </template>
  </NSwitch>
</template>
