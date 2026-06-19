<script setup lang="ts">
import { computed } from 'vue'
import { NForm, NFormItem, NRadio, NRadioGroup } from 'naive-ui'

import type { UploadChunkStrategyOptions } from '@/types/upload'
import { createDefaultUploadChunkStrategy } from '@/utils/uploadFormRules'

const model = defineModel<UploadChunkStrategyOptions>({
  default: () => createDefaultUploadChunkStrategy()
})
const defaultChunkStrategy = createDefaultUploadChunkStrategy()
const strategies = computed(() => [
  { label: '通用', value: 'GENERAL' },
  { label: '新闻类', value: 'NEWS' },
  { label: '技术类', value: 'TECHNICAL' },
  { label: '论文类', value: 'ACADEMIC' }
])

if (!model.value || !model.value.chunkStrategy) {
  model.value = defaultChunkStrategy
}
</script>

<template>
  <NForm class="upload-chunk-strategy" label-placement="top">
    <NFormItem label="文章分块策略">
      <NRadioGroup v-model:value="model.chunkStrategy">
        <NRadio v-for="item in strategies" :key="item.value" :value="item.value">
          {{ item.label }}
        </NRadio>
      </NRadioGroup>
    </NFormItem>
  </NForm>
</template>

<style scoped>
.upload-chunk-strategy {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
</style>
