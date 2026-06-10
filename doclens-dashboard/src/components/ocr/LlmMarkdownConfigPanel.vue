<script setup lang="ts">
import { computed, onMounted, useTemplateRef } from 'vue'
import { FlaskConical, RefreshCcw, Save } from '@lucide/vue'
import { NAlert, NButton, NForm, NFormItem, NIcon, NInput, NSelect, NTag, useMessage } from 'naive-ui'
import type { FormInst } from 'naive-ui'

import { useLlmMarkdownConfig } from '@/composables/useLlmMarkdownConfig'
import { formatDateTime } from '@/utils/formatters'
import { createLlmMarkdownConfigFormRules } from '@/utils/llmMarkdownConfigRules'

const message = useMessage()
const llmConfig = useLlmMarkdownConfig(message)
const formRef = useTemplateRef<FormInst>('formRef')
const formRules = computed(() => createLlmMarkdownConfigFormRules(llmConfig.form))
const apiTypeOptions = [
  { label: 'OpenAI compatible', value: 'openai' },
  { label: 'Anthropic', value: 'anthropic' }
]

/**
 * 校验并保存 LLM Markdown 配置。
 *
 * @returns 保存完成信号
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
async function saveConfig(): Promise<void> {
  try {
    await formRef.value?.validate()
    await llmConfig.saveConfig()
  } catch {
    // 表单校验未通过时由 Naive UI 展示字段错误，同时给出全局提示。
    message.warning('请先修正 LLM Markdown 配置表单')
  }
}

onMounted(llmConfig.loadConfig)
</script>

<template>
  <section class="panel llm-config-panel">
    <div class="panel__header llm-config-panel__header">
      <div>
        <h2 class="panel__title">LLM Markdown 后处理</h2>
        <span class="panel__hint">最后刷新：{{ formatDateTime(llmConfig.lastLoadedAt.value) }}</span>
      </div>
      <div class="llm-config-panel__actions">
        <NTag size="small" :type="llmConfig.isConfigured.value ? 'success' : 'default'">
          {{ llmConfig.isConfigured.value ? '已配置' : '未配置' }}
        </NTag>
        <NTag v-if="llmConfig.isConfigured.value" size="small" :type="llmConfig.form.healthy ? 'success' : 'error'">
          {{ llmConfig.form.healthy ? '心跳正常' : '心跳不可用' }}
        </NTag>
        <NButton size="small" :loading="llmConfig.isLoading.value" @click="llmConfig.loadConfig">
          <template #icon>
            <NIcon :component="RefreshCcw" />
          </template>
          刷新
        </NButton>
      </div>
    </div>

    <NAlert v-if="llmConfig.errorMessage.value" class="llm-config-panel__alert" type="error" :title="llmConfig.errorMessage.value" />
    <NAlert
      class="llm-config-panel__alert"
      type="info"
      :title="llmConfig.capabilityHints.value.protocolHint"
    >
      推荐 endpoint：{{ llmConfig.capabilityHints.value.endpointExample }}
    </NAlert>
    <NAlert
      v-if="llmConfig.isConfigured.value && !llmConfig.form.healthy"
      class="llm-config-panel__alert"
      type="error"
      :title="llmConfig.form.healthMessage || 'LLM Markdown 心跳不可用'"
    >
      最近心跳：{{ formatDateTime(llmConfig.form.lastHealthAt) }}
    </NAlert>

    <NForm ref="formRef" class="llm-config-panel__form" label-placement="top" :model="llmConfig.form" :rules="formRules">
      <NFormItem label="协议" path="apiType">
        <NSelect v-model:value="llmConfig.form.apiType" :options="apiTypeOptions" />
      </NFormItem>
      <NFormItem label="URL" path="url">
        <NInput v-model:value="llmConfig.form.url" :placeholder="llmConfig.capabilityHints.value.endpointExample" />
      </NFormItem>
      <NFormItem label="模型名称" path="model">
        <NInput v-model:value="llmConfig.form.model" placeholder="markdown-model" />
      </NFormItem>
      <NFormItem label="API Key" path="apiKey">
        <NInput
          v-model:value="llmConfig.form.apiKey"
          type="password"
          :placeholder="llmConfig.form.credentialConfigured ? '已配置，留空则沿用旧密钥' : '可选，保存后不再回显'"
        />
      </NFormItem>
    </NForm>

    <div class="llm-config-panel__footer">
      <span>
        API Key：{{ llmConfig.form.credentialConfigured ? '已配置' : '未配置' }}
        <template v-if="llmConfig.form.lastHealthAt">
          · 最近心跳：{{ formatDateTime(llmConfig.form.lastHealthAt) }}
        </template>
      </span>
      <NButton
        secondary
        :loading="llmConfig.isTesting.value"
        :disabled="!llmConfig.capabilityHints.value.canTest"
        @click="llmConfig.testConfig"
      >
        <template #icon>
          <NIcon :component="FlaskConical" />
        </template>
        测试配置
      </NButton>
      <NButton type="primary" :loading="llmConfig.isSaving.value" @click="saveConfig">
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
  grid-template-columns: minmax(150px, 0.7fr) minmax(260px, 1.5fr) minmax(180px, 1fr) minmax(220px, 1fr);
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
