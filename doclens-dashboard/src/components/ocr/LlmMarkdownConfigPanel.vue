<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { Plus, RefreshCcw } from '@lucide/vue'
import {
  NAlert,
  NButton,
  NDataTable,
  NIcon,
  useMessage
} from 'naive-ui'

import { useLlmMarkdownConfig } from '@/composables/useLlmMarkdownConfig'
import { createLlmMarkdownConfigColumns } from '@/components/ocr/LlmMarkdownConfigTableColumns'
import LlmMarkdownConfigDrawer from '@/components/ocr/LlmMarkdownConfigDrawer.vue'
import { formatDateTime } from '@/utils/formatters'

const message = useMessage()
const llmConfig = useLlmMarkdownConfig(message)
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
        <NButton
          size="small"
          :loading="llmConfig.isLoading.value"
          :disabled="llmConfig.isActionLocked.value"
          @click="llmConfig.loadConfig"
        >
          <template #icon>
            <NIcon :component="RefreshCcw" />
          </template>
          刷新
        </NButton>
        <NButton
          size="small"
          type="primary"
          secondary
          :disabled="llmConfig.isLoading.value || llmConfig.isActionLocked.value"
          @click="llmConfig.openCreateDrawer"
        >
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
    </div>

    <LlmMarkdownConfigDrawer
      :visible="llmConfig.isDrawerVisible.value"
      :title="llmConfig.editingTitle.value"
      :form="llmConfig.form"
      :saving="llmConfig.isSaving.value"
      :testing="llmConfig.isTesting.value"
      :can-submit="llmConfig.canSubmit.value"
      :capability-hints="llmConfig.capabilityHints.value"
      @close="llmConfig.closeDrawer"
      @save="llmConfig.saveConfig"
      @test="llmConfig.testConfig"
    />
  </section>
</template>

<style scoped>
.llm-config-panel {
  overflow: hidden;
}

.llm-config-panel__header {
  align-items: flex-start;
}

.llm-config-panel__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.llm-config-panel__alert {
  margin-bottom: 12px;
}

.llm-config-panel__body {
  display: block;
}

.llm-config-panel__table {
  min-width: 0;
}

@media (max-width: 640px) {
  .llm-config-panel__header,
  .llm-config-panel__actions,
  .llm-config-panel__body {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
