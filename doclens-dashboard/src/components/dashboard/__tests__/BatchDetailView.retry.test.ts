import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, ref, type VNodeChild } from 'vue'
import { mount } from '@vue/test-utils'
import { NConfigProvider } from 'naive-ui'

const { retryDocument } = vi.hoisted(() => ({
  retryDocument: vi.fn(() => Promise.resolve())
}))

const loadBatchDetail = vi.fn(() => Promise.resolve())

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { batchId: 'batch-test' } }),
  useRouter: () => ({ back: vi.fn() })
}))

vi.mock('pinia', () => ({
  storeToRefs: () => ({
    selectedBatch: ref({
      batch: {
        batch_id: 'batch-test',
        status: 'partial_failed',
        total_files: 2,
        completed_files: 1,
        failed_files: 1,
        progress_percent: 50,
        success_rate: 50,
        failure_rate: 50,
        average_duration_ms: 1000,
        created_at: '2026-06-10T10:00:00+08:00',
        updated_at: '2026-06-10T10:05:00+08:00'
      },
      documents: [
        {
          document_id: 'doc-failed',
          batch_id: 'batch-test',
          file_name: 'failed.pdf',
          file_type: 'pdf',
          status: 'failed',
          stage: 'ocr_failed',
          progress_percent: 100,
          current_page: 2,
          total_pages: 2,
          duration_ms: 1000,
          llm_chunk_count: 0,
          track: [],
          ocr_running_hit_nodes: [],
          ocr_final_hit_nodes: [],
          error_code: 'OCR_FAILED',
          error_message: 'ocr failed',
          updated_at: '2026-06-10T10:04:00+08:00'
        },
        {
          document_id: 'doc-completed',
          batch_id: 'batch-test',
          file_name: 'completed.pdf',
          file_type: 'pdf',
          status: 'completed',
          stage: 'completed',
          progress_percent: 100,
          current_page: 2,
          total_pages: 2,
          duration_ms: 1000,
          llm_chunk_count: 4,
          track: [],
          ocr_running_hit_nodes: [],
          ocr_final_hit_nodes: [],
          error_code: '',
          error_message: '',
          updated_at: '2026-06-10T10:03:00+08:00'
        }
      ],
      events: [],
      ocr_route_policy: {
        routing_mode: 'GLOBAL_LOAD_BALANCE',
        model_key: 'paddle_ocr',
        node_id: '',
        load_balance_strategy: 'least-inflight'
      },
      batch_dispatch_hit_nodes: [],
      failure_summary: {}
    }),
    detailState: ref({
      loading: false,
      error: '',
      lastUpdated: '2026-06-10T10:05:00+08:00'
    })
  })
}))

vi.mock('@/stores/dashboard', () => ({
  useDashboardStore: () => ({
    loadBatchDetail
  })
}))

vi.mock('@/composables/useAutoRefresh', () => ({
  DEFAULT_REFRESH_INTERVAL_SECONDS: 5,
  useAutoRefresh: (refresh: () => Promise<void>) => ({
    isStale: { value: false },
    lastErrorMessage: { value: '' },
    refreshNow: refresh
  })
}))

vi.mock('@/composables/useDocumentResultDrawer', () => ({
  useDocumentResultDrawer: () => ({
    resultDrawerOpen: ref(false),
    selectedResultDocument: ref(null),
    selectedDocumentResult: ref(null),
    resultState: { loading: false, error: '' },
    openDocumentResult: vi.fn(),
    retryDocumentResult: vi.fn()
  })
}))

vi.mock('@/api/dashboard', async () => {
  const actual = await vi.importActual<typeof import('@/api/dashboard')>('@/api/dashboard')
  return {
    ...actual,
    retryDocument
  }
})

import BatchDetailView from '@/views/BatchDetailView.vue'

const DataTableStub = defineComponent({
  name: 'NDataTable',
  props: {
    columns: {
      type: Array,
      required: true
    },
    data: {
      type: Array,
      required: true
    }
  },
  setup(props) {
    return () => h('div', { class: 'data-table-stub' }, (props.data as Array<Record<string, unknown>>).flatMap((row) =>
      (props.columns as Array<Record<string, unknown>>)
        .filter((column) => typeof column.render === 'function')
        .map((column, index) => h('div', { key: `${String(row.document_id)}-${index}` }, [
          (column.render as (item: Record<string, unknown>) => VNodeChild)(row)
        ]))
    ))
  }
})

const MountHost = defineComponent({
  name: 'MountHost',
  setup() {
    return () => h(NConfigProvider, null, {
      default: () => h(BatchDetailView)
    })
  }
})

describe('BatchDetailView document retry', () => {
  beforeEach(() => {
    loadBatchDetail.mockClear()
    retryDocument.mockClear()
  })

  it('shows retry button for failed documents and refreshes detail after retry', async () => {
    const wrapper = mount(MountHost, {
      global: {
        stubs: {
          NAlert: true,
          NButton: false,
          NProgress: true,
          NDataTable: DataTableStub,
          BatchOcrRoutePanel: true,
          DocumentResultDrawer: true,
          DocumentTrackCards: true,
          StatusTag: true
        }
      }
    })

    const retryButton = wrapper.findAll('button').find((candidate) => candidate.text() === '重试')

    expect(retryButton).toBeDefined()
    await retryButton?.trigger('click')
    expect(retryDocument).toHaveBeenCalledWith('doc-failed')
    expect(loadBatchDetail).toHaveBeenCalledWith('batch-test')
  })
})
