<script setup lang="ts">
import { onMounted } from 'vue'
import { RefreshCcw, Save } from '@lucide/vue'
import { NAlert, NButton, NForm, NFormItem, NIcon, NInput, NTag, useMessage } from 'naive-ui'

import { useLlmMarkdownConfig } from '@/composables/useLlmMarkdownConfig'
import { formatDateTime } from '@/utils/formatters'

const message = useMessage()
const {
  form,
  isLoading,
  isSaving,
  lastLoadedAt,
  errorMessage,
  canSubmit,
  isConfigured,
  loadConfig,
  saveConfig
} = useLlmMarkdownConfig(message)

onMounted(loadConfig)
</script>

<template>
  <section class="panel llm-config-panel">
    <div class="panel__header llm-config-panel__header">
      <div>
        <h2 class="panel__title">LLM Markdown 后处理</h2>
        <span class="panel__hint">最后刷新：{{ formatDateTime(lastLoadedAt) }}</span>
      </div>
      <div class="llm-config-panel__actions">
        <NTag size="small" :type="isConfigured ? 'success' : 'default'">
          {{ isConfigured ? '已配置' : '未配置' }}
        </NTag>
        <NButton size="small" :loading="isLoading" @click="loadConfig">
          <template #icon>
            <NIcon :component="RefreshCcw" />
          </template>
          刷新
        </NButton>
      </div>
    </div>

    <NAlert v-if="errorMessage" class="llm-config-panel__alert" type="error" :title="errorMessage" />

    <NForm class="llm-config-panel__form" label-placement="top">
      <NFormItem label="URL">
        <NInput v-model:value="form.url" placeholder="https://llm.example.com/v1/chat/completions" />
      </NFormItem>
      <NFormItem label="模型名称">
        <NInput v-model:value="form.model" placeholder="markdown-model" />
      </NFormItem>
      <NFormItem label="API Key">
        <NInput
          v-model:value="form.apiKey"
          type="password"
          :placeholder="form.credentialConfigured ? '已配置，留空则沿用旧密钥' : '可选，保存后不再回显'"
        />
      </NFormItem>
    </NForm>

    <div class="llm-config-panel__footer">
      <span>API Key：{{ form.credentialConfigured ? '已配置' : '未配置' }}</span>
      <NButton type="primary" :loading="isSaving" :disabled="!canSubmit" @click="saveConfig">
        <template #icon>
          <NIcon :component="Save" />
        </template>
        保存
      </NButton>
    </div>
  </section>
</template>

<style scoped>
.llm-config-panel {
  overflow: hidden;
}

.llm-config-panel__header {
  align-items: flex-start;
}

.llm-config-panel__actions,
.llm-config-panel__footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.llm-config-panel__alert {
  margin-bottom: 12px;
}

.llm-config-panel__form {
  display: grid;
  min-width: 0;
  grid-template-columns: minmax(280px, 1.5fr) minmax(200px, 1fr) minmax(220px, 1fr);
  gap: 12px;
}

.llm-config-panel__footer {
  margin-top: 4px;
}

.llm-config-panel__footer span {
  min-width: 0;
  color: var(--ink-muted);
  font-size: 12px;
  overflow-wrap: anywhere;
}

@media (max-width: 960px) {
  .llm-config-panel__form {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .llm-config-panel__header,
  .llm-config-panel__actions,
  .llm-config-panel__footer {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
