import { computed, reactive, shallowRef } from 'vue'
import { defineStore } from 'pinia'

import {
  fetchBatchDetail,
  fetchBatchList,
  fetchDashboardSummary,
  fetchOcrHealth
} from '@/api/dashboard'
import type {
  BatchDetailResponse,
  BatchListResponse,
  DashboardSummary,
  OcrHealthResponse
} from '@/types/dashboard'

interface RequestState {
  loading: boolean
  error: string
  lastUpdated: string
}

function emptyRequestState(): RequestState {
  return {
    loading: false,
    error: '',
    lastUpdated: ''
  }
}

export const useDashboardStore = defineStore('dashboard', () => {
  const summary = shallowRef<DashboardSummary | null>(null)
  const batches = shallowRef<BatchListResponse>({ items: [], total: 0 })
  const selectedBatch = shallowRef<BatchDetailResponse | null>(null)
  const ocrHealth = shallowRef<OcrHealthResponse | null>(null)

  const summaryState = reactive(emptyRequestState())
  const batchesState = reactive(emptyRequestState())
  const detailState = reactive(emptyRequestState())
  const ocrHealthState = reactive(emptyRequestState())

  const activeDocuments = computed(() => summary.value?.overview.processing_documents ?? 0)
  const hasFailures = computed(() => (summary.value?.overview.failed_documents ?? 0) > 0)

  async function runRequest<T>(
    state: RequestState,
    request: () => Promise<T>,
    applyResult: (result: T) => void
  ): Promise<void> {
    state.loading = true
    state.error = ''
    try {
      const result = await request()
      applyResult(result)
      state.lastUpdated = new Date().toISOString()
    } catch (error) {
      state.error = error instanceof Error ? error.message : '请求失败'
    } finally {
      state.loading = false
    }
  }

  function loadSummary(): Promise<void> {
    return runRequest(summaryState, fetchDashboardSummary, (result) => {
      summary.value = result
    })
  }

  function loadBatches(): Promise<void> {
    return runRequest(batchesState, fetchBatchList, (result) => {
      batches.value = result
    })
  }

  function loadBatchDetail(batchId: string): Promise<void> {
    return runRequest(detailState, () => fetchBatchDetail(batchId), (result) => {
      selectedBatch.value = result
    })
  }

  function loadOcrHealth(): Promise<void> {
    return runRequest(ocrHealthState, fetchOcrHealth, (result) => {
      ocrHealth.value = result
    })
  }

  return {
    summary,
    batches,
    selectedBatch,
    ocrHealth,
    summaryState,
    batchesState,
    detailState,
    ocrHealthState,
    activeDocuments,
    hasFailures,
    loadSummary,
    loadBatches,
    loadBatchDetail,
    loadOcrHealth
  }
})
