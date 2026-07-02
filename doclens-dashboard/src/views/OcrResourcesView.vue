<script setup lang="ts">
import { onMounted } from 'vue'
import { Plus, RefreshCcw } from '@lucide/vue'
import { NAlert, NButton, NIcon, useMessage } from 'naive-ui'

import LlmMarkdownConfigPanel from '@/components/ocr/LlmMarkdownConfigPanel.vue'
import OcrGovernanceConfigPanel from '@/components/ocr/OcrGovernanceConfigPanel.vue'
import OcrModelList from '@/components/ocr/OcrModelList.vue'
import OcrNodeDetailDrawer from '@/components/ocr/OcrNodeDetailDrawer.vue'
import OcrNodeFormDrawer from '@/components/ocr/OcrNodeFormDrawer.vue'
import OcrNodeTable from '@/components/ocr/OcrNodeTable.vue'
import { useOcrResources } from '@/composables/useOcrResources'
import { formatDateTime } from '@/utils/formatters'

const message = useMessage()
const ocrResources = useOcrResources(message)
const {
  models,
  nodes,
  selectedModelKey,
  selectedNode,
  selectedNodeCalls,
  editingNode,
  isFormVisible,
  isDetailVisible,
  isLoadingModels,
  isLoadingNodes,
  isSavingNode,
  nodeActionById,
  lastUpdated,
  errorMessage,
  selectedModel,
  hasModels,
  loadModels,
  selectModel,
  openCreateDrawer,
  openEditDrawer,
  openDetailDrawer,
  closeFormDrawer,
  closeDetailDrawer,
  saveNode,
  removeNode,
  testNode,
  reconnectNode,
  toggleNodeEnabled
} = ocrResources

onMounted(loadModels)
</script>

<template>
  <div class="view-stack">
    <NAlert v-if="errorMessage" type="error" :title="errorMessage" />

    <section class="ocr-resource-toolbar">
      <div>
        <span>当前模型</span>
        <strong>{{ selectedModel?.name ?? '未选择' }}</strong>
      </div>
      <div class="ocr-resource-toolbar__actions">
        <span>最后刷新：{{ formatDateTime(lastUpdated) }}</span>
        <NButton size="small" :loading="isLoadingModels || isLoadingNodes" @click="loadModels">
          <template #icon>
            <NIcon :component="RefreshCcw" />
          </template>
          刷新
        </NButton>
        <NButton size="small" type="primary" :disabled="!hasModels" @click="openCreateDrawer">
          <template #icon>
            <NIcon :component="Plus" />
          </template>
          新增节点
        </NButton>
      </div>
    </section>

    <OcrModelList :models="models" :selected-model-key="selectedModelKey" @select="selectModel" />

    <LlmMarkdownConfigPanel />
    <OcrGovernanceConfigPanel />

    <section class="panel">
      <div class="panel__header">
        <div>
          <h2 class="panel__title">OCR 节点</h2>
          <span class="panel__hint">展示当前解析图片数、成功失败、耗时和最近错误</span>
        </div>
      </div>
      <OcrNodeTable
        :nodes="nodes"
        :loading="isLoadingNodes"
        :node-action-by-id="nodeActionById"
        @edit="openEditDrawer"
        @delete="removeNode"
        @detail="openDetailDrawer"
        @test="testNode"
        @reconnect="reconnectNode"
        @toggle-enabled="toggleNodeEnabled"
      />
    </section>

    <OcrNodeFormDrawer
      :visible="isFormVisible"
      :models="models"
      :selected-model-key="selectedModelKey"
      :node="editingNode"
      :loading="isSavingNode"
      @close="closeFormDrawer"
      @submit="saveNode"
    />

    <OcrNodeDetailDrawer
      :visible="isDetailVisible"
      :node="selectedNode"
      :calls="selectedNodeCalls"
      @close="closeDetailDrawer"
    />
  </div>
</template>

<style scoped>
.ocr-resource-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 16px;
  border: 1px solid var(--rail-border);
  border-radius: 8px;
  background: var(--surface-raised);
  box-shadow: var(--shadow-card);
}

.ocr-resource-toolbar > div:first-child {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.ocr-resource-toolbar span {
  color: var(--ink-muted);
  font-size: 12px;
}

.ocr-resource-toolbar strong {
  overflow-wrap: anywhere;
  color: var(--ink-strong);
  font-size: 16px;
}

.ocr-resource-toolbar__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 780px) {
  .ocr-resource-toolbar,
  .ocr-resource-toolbar__actions {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
