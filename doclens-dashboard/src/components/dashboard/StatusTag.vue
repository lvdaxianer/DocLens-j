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
  if (normalized === 'processing') {
    return 'warning'
  }
  return 'default'
})
</script>

<template>
  <NTag :type="tagType" round>
    {{ statusLabel(status) }}
  </NTag>
</template>
