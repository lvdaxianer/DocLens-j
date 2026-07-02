<script setup lang="ts">
import { NAlert } from 'naive-ui'

/**
 * 自动刷新失效提示。
 *
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
defineProps<{
  /** 是否展示失效提示。 */
  show: boolean
  /** 当前可能失效的数据名称。 */
  subject: string
  /** 最近一次自动刷新失败原因。 */
  errorMessage: string
}>()
</script>

<template>
  <NAlert v-if="show" type="warning" title="自动刷新暂未成功">
    当前{{ subject }}可能不是最新：{{ errorMessage }}。请手动刷新。
  </NAlert>
</template>
