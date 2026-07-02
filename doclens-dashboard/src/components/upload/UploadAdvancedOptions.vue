<script setup lang="ts">
import { computed } from 'vue'
import { NForm, NFormItem, NInput } from 'naive-ui'

import type { UploadAdvancedOptionsValue } from '@/types/upload'
import { callbackContractHints, createUploadAdvancedOptionsRules } from '@/utils/uploadFormRules'

const model = defineModel<UploadAdvancedOptionsValue>({ required: true })
const props = withDefaults(defineProps<{
  /** 上传请求进行中时禁用高级参数输入。 */
  disabled?: boolean
}>(), {
  disabled: false
})
const callbackHints = callbackContractHints()
const formRules = computed(createUploadAdvancedOptionsRules)
</script>

<template>
  <!-- 上传中禁用所有可编辑字段，防止请求 payload 与屏幕内容不一致。 -->
  <NForm class="upload-advanced-options" label-placement="top" :model="model" :rules="formRules">
    <NFormItem label="元数据 JSON" path="metadata">
      <NInput
        id="upload-metadata"
        v-model:value="model.metadata"
        type="textarea"
        :autosize="{ minRows: 3, maxRows: 5 }"
        placeholder="metadata JSON，例如 {}"
        class="upload-advanced-options__json-editor"
        :disabled="props.disabled"
      />
    </NFormItem>
    <div class="upload-advanced-options__callback">
      <p class="upload-advanced-options__hint">回调将在解析完成后以 {{ callbackHints.method }} 方式发送。</p>
      <pre class="upload-advanced-options__contract">{{ JSON.stringify(callbackHints.body, null, 2) }}</pre>
      <ul class="upload-advanced-options__contract-list">
        <li><code>meta</code> 为上传元数据</li>
        <li><code>text</code> {{ callbackHints.textSourceHint }}</li>
        <li><code>idempotency_key</code> 为第三方系统使用的透传键，DocLens 不做重复拦截</li>
      </ul>
    </div>
    <div class="upload-advanced-options__grid">
      <NFormItem label="回调地址" path="callbackUrl">
        <NInput v-model:value="model.callbackUrl" placeholder="callback_url，可选" :disabled="props.disabled" />
      </NFormItem>
      <NFormItem label="第三方透传键" path="idempotencyKey">
        <NInput v-model:value="model.idempotencyKey" placeholder="idempotency_key，可选，原样进入回调" :disabled="props.disabled" />
      </NFormItem>
    </div>
  </NForm>
</template>

<style scoped>
.upload-advanced-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.upload-advanced-options__hint {
  margin: -2px 0 0;
  color: var(--ink-muted);
  font-size: 12px;
}

.upload-advanced-options__callback {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.upload-advanced-options__contract {
  margin: 0;
  padding: 10px 12px;
  border: 1px solid var(--rail-border);
  border-radius: 8px;
  overflow-x: auto;
  background: var(--surface-inset);
  color: var(--ink-strong);
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", monospace;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.upload-advanced-options__contract-list {
  display: grid;
  gap: 4px;
  margin: 0;
  padding-left: 18px;
  color: var(--ink-muted);
  font-size: 12px;
}

.upload-advanced-options__json-editor :deep(textarea) {
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", monospace;
}

.upload-advanced-options__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

@media (max-width: 760px) {
  .upload-advanced-options__grid {
    grid-template-columns: 1fr;
  }
}
</style>
