<script setup lang="ts">
import { computed } from 'vue'
import { NTag } from 'naive-ui'

import { statusLabel } from '@/utils/formatters'

const props = defineProps<{
  status: string
}>()

const tagType = computed(() => {
  const normalized = props.status.toLowerCase()
  if (normalized === 'completed') {
    return 'success'
  }
  if (normalized === 'failed') {
    return 'error'
  }
  if (normalized === 'stalled') {
    return 'warning'
  }
  if (normalized === 'processing') {
    return 'warning'
  }
  return 'default'
})
</script>

<template>
  <NTag :type="tagType" round class="status-tag">
    {{ statusLabel(status) }}
  </NTag>
</template>

<style scoped>
.status-tag :deep(.n-tag) {
  position: relative;
  overflow: hidden;
}

.status-tag :deep(.n-tag)::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.12), transparent);
  background-size: 200% 100%;
  animation: shimmer 2.5s infinite linear;
  pointer-events: none;
}
</style>
