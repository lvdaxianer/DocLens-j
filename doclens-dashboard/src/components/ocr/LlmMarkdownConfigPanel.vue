<script setup lang="ts">
import { computed, onMounted } from 'vue'
import {
  FlaskConical,
  Plus,
  RefreshCcw,
  Save
} from '@lucide/vue'
import {
  NAlert,
  NButton,
  NDataTable,
  NForm,
  NFormItem,
  NIcon,
  NInput,
  NInputNumber,
  NSelect,
  useMessage
} from 'naive-ui'

import { useLlmMarkdownConfig } from '@/composables/useLlmMarkdownConfig'
import { createLlmMarkdownConfigColumns } from '@/components/ocr/LlmMarkdownConfigTableColumns'
import { formatDateTime } from '@/utils/formatters'

const message = useMessage()
const llmConfig = useLlmMarkdownConfig(message)
const apiTypeOptions = [
  { label: 'OpenAI compatible', value: 'openai' },
  { label: 'Anthropic', value: 'anthropic' }
]
const columns = computed(() => createLlmMarkdownConfigColumns({
  actingId: llmConfig.actingId.value,
  editConfig: llmConfig.editConfig,
  toggleEnabled: llmConfig.toggleEnabled,
  makeDefault: llmConfig.makeDefault,
  removeConfig: llmConfig.removeConfig
}))

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
        <NButton size="small" :loading="llmConfig.isLoading.value" @click="llmConfig.loadConfig">
          <template #icon>
            <NIcon :component="RefreshCcw" />
          </template>
          刷新
        </NButton>
        <NButton size="small" type="primary" secondary @click="llmConfig.resetForm">
          <template #icon>
            <NIcon :component="Plus" />
          </template>
          新增配置
        </NButton>
      </div>
    </div>

    <NAlert
      v-if="llmConfig.errorMessage.value"
      class="llm-config-panel__alert"
      type="error"
      :title="llmConfig.errorMessage.value"
    />
    <NAlert
      v-if="!llmConfig.hasConfigs.value && !llmConfig.isLoading.value"
      class="llm-config-panel__alert"
      type="info"
      :title="llmConfig.emptyStatus.value"
    />

    <div class="llm-config-panel__body">
      <NDataTable
        class="llm-config-panel__table"
        :columns="columns"
        :data="llmConfig.rows.value"
        :loading="llmConfig.isLoading.value"
        :bordered="false"
        size="small"
      />

      <section class="llm-config-panel__editor">
        <div class="llm-config-panel__editor-head">
          <strong>{{ llmConfig.editingTitle.value }}</strong>
          <span>用户只需输入完整接口地址。</span>
        </div>

        <NForm class="llm-config-panel__form" label-placement="top">
          <NFormItem label="配置名称">
            <NInput v-model:value="llmConfig.form.name" placeholder="例如：主配置" />
          </NFormItem>
          <NFormItem label="协议">
            <NSelect v-model:value="llmConfig.form.apiType" :options="apiTypeOptions" />
          </NFormItem>
          <NFormItem label="完整地址">
            <NInput v-model:value="llmConfig.form.url" :placeholder="llmConfig.capabilityHints.value.urlPlaceholder" />
          </NFormItem>
          <NFormItem label="模型名称">
            <NInput v-model:value="llmConfig.form.model" placeholder="markdown-model" />
          </NFormItem>
          <NFormItem label="API Key 环境变量名">
            <NInput
              v-model:value="llmConfig.form.credentialEnvVar"
              placeholder="例如：MINIMAX_API_KEY"
            />
          </NFormItem>
          <NFormItem label="最大上下文 Token 数">
            <NInputNumber
              v-model:value="llmConfig.form.maxContextTokens"
              :min="1000"
              :precision="0"
              placeholder="例如：16000"
            />
          </NFormItem>
          <NFormItem label="最大并发数">
            <NInputNumber
              v-model:value="llmConfig.form.maxConcurrency"
              :min="1"
              :precision="0"
              placeholder="例如：1"
            />
          </NFormItem>
          <NFormItem label="请求间隔（毫秒）">
            <NInputNumber
              v-model:value="llmConfig.form.requestIntervalMillis"
              :min="0"
              :precision="0"
              placeholder="例如：1000"
            />
          </NFormItem>
          <NFormItem label="优先级">
            <NInputNumber v-model:value="llmConfig.form.priority" :min="0" :precision="0" />
          </NFormItem>
        </NForm>

        <div class="llm-config-panel__toggles">
          <label><input v-model="llmConfig.form.enabled" type="checkbox" /> 启用</label>
          <label><input v-model="llmConfig.form.defaultConfig" type="checkbox" /> 设为默认</label>
        </div>

        <div class="llm-config-panel__footer">
          <span>
            凭证：{{ llmConfig.form.credentialEnvVar || '未填写环境变量名' }}
            / {{ llmConfig.form.credentialConfigured ? '已配置' : '未配置' }}
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
          <NButton
            type="primary"
            :loading="llmConfig.isSaving.value"
            :disabled="!llmConfig.canSubmit.value"
            @click="llmConfig.saveConfig"
          >
            <template #icon>
              <NIcon :component="Save" />
            </template>
            保存
          </NButton>
        </div>
      </section>
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
.llm-config-panel__footer,
.llm-config-panel__toggles {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.llm-config-panel__alert {
  margin-bottom: 12px;
}

.llm-config-panel__body {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(320px, 0.8fr);
  gap: 14px;
}

.llm-config-panel__table {
  min-width: 0;
}

.llm-config-panel__editor {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--rail-border);
  border-radius: 8px;
  background: var(--surface-raised);
}

.llm-config-panel__editor-head {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.llm-config-panel__editor-head strong {
  color: var(--ink-strong);
}

.llm-config-panel__editor-head span,
.llm-config-panel__footer span {
  color: var(--ink-muted);
  font-size: 12px;
}

.llm-config-panel__form {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
}

.llm-config-panel__toggles {
  justify-content: flex-start;
}

.llm-config-panel__toggles label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--ink-soft);
  font-size: 12px;
  font-weight: 650;
}

.llm-config-panel__footer {
  margin-top: 2px;
}

@media (max-width: 1280px) {
  .llm-config-panel__body {
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
