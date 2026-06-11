<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { NAlert } from 'naive-ui'

import { deleteDocument, retryDocument } from '@/api/dashboard'
import BatchDocumentTable from '@/components/dashboard/BatchDocumentTable.vue'
import BatchOcrRoutePanel from '@/components/dashboard/BatchOcrRoutePanel.vue'
import BatchSummaryStrip from '@/components/dashboard/BatchSummaryStrip.vue'
import DocumentResultDrawer from '@/components/dashboard/DocumentResultDrawer.vue'
import DocumentTrackCards from '@/components/dashboard/DocumentTrackCards.vue'
import StatusTag from '@/components/dashboard/StatusTag.vue'
import { useDocumentResultDrawer } from '@/composables/useDocumentResultDrawer'
import { DEFAULT_REFRESH_INTERVAL_SECONDS, useAutoRefresh } from '@/composables/useAutoRefresh'
import { useDashboardStore } from '@/stores/dashboard'
import type { DocumentRow } from '@/types/dashboard'

// Vue route/store/composable 初始化集中在顶部，便于快速识别页面依赖。
const route = useRoute()
const router = useRouter()
const store = useDashboardStore()
// storeToRefs 保持 Pinia state 响应式，避免解构后丢失更新。
const { selectedBatch, detailState } = storeToRefs(store)

/**
 * 批次详情页是路由级组合层。
 *
 * 设计边界：
 * - 路由参数由本组件读取，避免子组件感知 URL。
 * - 批次详情由 store 管理，子组件只接收快照数据。
 * - 重试、删除、刷新属于远程副作用，集中留在父级。
 * - 文档结果抽屉状态来自 composable，避免表格持有弹层状态。
 * - Summary 与 Table 只负责展示和事件上抛。
 * - 删除最后一个文档后的回退逻辑只和路由层相关。
 * - 自动刷新使用统一 composable，避免多个子组件各自轮询。
 * - 当前路由命中文档是 OCR 路由面板的派生展示数据。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
const batchId = computed(() => String(route.params.batchId ?? ''))
// 行级 loading 用文档 ID 表示，避免操作一行时整张表误置灰。
const retryingDocumentId = ref('')
// 删除 loading 独立于重试 loading，支持用户区分当前动作。
const deletingDocumentId = ref('')
// OCR 路由面板优先展示正在处理文档，其次展示已有最终命中节点的文档。
const currentRouteDocument = computed(() => {
  // 当前正在处理的文档最能代表路由中的实时命中状态。
  const documents = selectedBatch.value?.documents ?? []
  // 找不到 processing 时，退回到已有最终命中节点的文档。
  return documents.find((document) => document.status === 'processing')
    // 最后兜底第一份文档，保证路由面板仍可展示批次上下文。
    ?? documents.find((document) => document.ocr_final_hit_nodes.length > 0)
    ?? documents[0]
    ?? null
})
const {
  // resultDrawerOpen 通过 v-model 绑定到抽屉展示状态。
  resultDrawerOpen,
  // selectedResultDocument 用于抽屉标题和行内 loading 定位。
  selectedResultDocument,
  // selectedDocumentResult 保存实际 OCR/LLM 结果。
  selectedDocumentResult,
  // resultState 统一承载结果加载和错误信息。
  resultState,
  openDocumentResult,
  retryDocumentResult
} = useDocumentResultDrawer()
// 抽屉当前文档 ID 单独派生，避免表格组件读取整个文档对象。
const selectedResultDocumentId = computed(() => selectedResultDocument.value?.document_id ?? '')

/**
 * 执行文档重试并刷新当前批次详情。
 *
 * @param documentId 文档 ID
 * @returns 重试完成信号
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
async function handleRetryDocument(documentId: string): Promise<void> {
  retryingDocumentId.value = documentId
  try {
    // 重试成功后立即刷新详情，确保状态从 failed/stalled 回到 queued/processing。
    await retryDocument(documentId)
    await refresh()
  } finally {
    // finally 中清理行级 loading，避免请求失败后按钮一直转圈。
    retryingDocumentId.value = ''
  }
}

/**
 * 执行文档删除并刷新当前批次详情。
 *
 * @param documentId 文档 ID
 * @returns 删除完成信号
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
async function handleDeleteDocument(documentId: string): Promise<void> {
  deletingDocumentId.value = documentId
  const isDeletingLastDocument = (selectedBatch.value?.documents.length ?? 0) <= 1
  try {
    // 删除请求只由父级发起，子组件不承担 API 副作用。
    await deleteDocument(documentId)
    if (isDeletingLastDocument) {
      // 删除最后一个文档后批次会被清理，避免刷新已不存在的详情。
      router.back()
    } else {
      // 批次仍有文档时刷新当前详情，保持列表状态同步。
      await refresh()
    }
  } finally {
    // 删除完成或失败都释放当前行按钮状态。
    deletingDocumentId.value = ''
  }
}

/**
 * 打开指定文档的 OCR 结果抽屉。
 *
 * @param document 文档行
 * @returns 打开动作完成信号
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function handleOpenDocumentResult(document: DocumentRow): void {
  // 抽屉打开动作委托给 composable，保持页面状态来源单一。
  void openDocumentResult(document)
}

/**
 * 刷新当前批次详情。
 *
 * @returns 刷新完成信号
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
function refresh(): Promise<void> {
  if (batchId.value) {
    // 有批次 ID 时刷新当前批次详情。
    // 刷新方法返回 Promise，便于重试/删除后串行等待详情同步。
    return store.loadBatchDetail(batchId.value)
  } else {
    // 路由参数为空时跳过请求。
    return Promise.resolve()
  }
}

watch(batchId, refresh)
// 自动刷新只注册一次，具体刷新逻辑仍复用 refresh 方法。
useAutoRefresh(refresh)
</script>

<template>
  <!-- view-stack 复用页面级纵向间距，避免详情页自己定义额外 layout。 -->
  <div class="view-stack">
    <!-- 错误提示放在页面顶部，确保刷新或删除失败时优先被用户看到。 -->
    <NAlert v-if="detailState.error" type="error" :title="detailState.error" />

    <!-- 顶部指标条只展示批次聚合数据，并通过 refresh 事件回到父级执行请求。 -->
    <!-- selectedBatch 不存在时不渲染摘要，避免展示空指标误导用户。 -->
    <!-- batch 使用 selectedBatch.batch，避免子组件接收整个详情对象。 -->
    <!-- loading 复用详情请求状态，避免额外定义刷新状态。 -->
    <!-- 刷新间隔从统一常量注入，展示和轮询策略保持一致。 -->
    <BatchSummaryStrip
      v-if="selectedBatch"
      :batch="selectedBatch.batch"
      :loading="detailState.loading"
      :refresh-interval-seconds="DEFAULT_REFRESH_INTERVAL_SECONDS"
      @refresh="refresh"
    />

    <!-- OCR 路由面板展示当前命中的节点，命中文档由父级 computed 保持稳定。 -->
    <!-- current-document-name 允许为空，子组件负责展示未命中文案。 -->
    <!-- current-document-final-hit-nodes 用空数组兜底，避免子组件处理 undefined。 -->
    <BatchOcrRoutePanel
      v-if="selectedBatch"
      :route-policy="selectedBatch.ocr_route_policy"
      :current-document-name="currentRouteDocument?.file_name"
      :current-document-final-hit-nodes="currentRouteDocument?.ocr_final_hit_nodes ?? []"
      :hit-nodes="selectedBatch.batch_dispatch_hit_nodes"
    />

    <!-- 文档轨道区域由轨道卡片、文档表格和结果抽屉三个独立子组件组成。 -->
    <section class="panel">
      <!-- panel__header 延续全局面板样式，标题和状态标签保持左右分布。 -->
      <div class="panel__header">
        <div>
          <!-- 标题固定为文档处理轨道，强调这里展示的是单批次内文档流转。 -->
          <h2 class="panel__title">文档处理轨道</h2>
          <!-- hint 显示批次 ID 和刷新节奏，方便排查用户截图时定位批次。 -->
          <span class="panel__hint">{{ batchId }} · 每 {{ DEFAULT_REFRESH_INTERVAL_SECONDS }} 秒自动刷新</span>
        </div>
        <!-- 批次状态标签只在详情存在时展示，避免空详情闪烁默认状态。 -->
        <StatusTag v-if="selectedBatch" :status="selectedBatch.batch.status" />
      </div>
      <!-- 轨道卡片用于快速观察文档状态分布，不承接任何远程副作用。 -->
      <DocumentTrackCards :documents="selectedBatch?.documents ?? []" />
      <!-- 表格只上抛查看、重试、删除事件，具体请求仍由路由级组件处理。 -->
      <!-- documents 使用空数组兜底，避免表格在初次加载时报空。 -->
      <!-- loading 同步给表格，用统一 Loading 态代替空白区域。 -->
      <!-- result-loading 只影响查看文本按钮，不影响重试/删除按钮。 -->
      <!-- selectedResultDocumentId 控制单个“查看文本”按钮 loading。 -->
      <!-- retrying/deleting ID 分离，避免同时操作时互相覆盖。 -->
      <BatchDocumentTable
        :documents="selectedBatch?.documents ?? []"
        :loading="detailState.loading"
        :result-loading="resultState.loading"
        :selected-result-document-id="selectedResultDocumentId"
        :retrying-document-id="retryingDocumentId"
        :deleting-document-id="deletingDocumentId"
        @open-result="handleOpenDocumentResult"
        @retry-document="handleRetryDocument"
        @delete-document="handleDeleteDocument"
      />
      <!-- 结果抽屉保留在父级，方便和文档结果 composable 的状态直接绑定。 -->
      <DocumentResultDrawer
        v-model:show="resultDrawerOpen"
        :document="selectedResultDocument"
        :result="selectedDocumentResult"
        :loading="resultState.loading"
        :error="resultState.error"
        @retry="retryDocumentResult"
      />
    </section>
  </div>
</template>
