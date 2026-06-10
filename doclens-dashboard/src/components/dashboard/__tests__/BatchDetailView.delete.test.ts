import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, ref, type VNodeChild } from 'vue'
import { mount } from '@vue/test-utils'
import { NConfigProvider } from 'naive-ui'

const { deleteDocument } = vi.hoisted(() => ({
  deleteDocument: vi.fn(() => Promise.resolve())
}))

const loadBatchDetail = vi.fn(() => Promise.resolve())

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { batchId: 'batch-test' } })
}))

vi.mock('pinia', () => ({
  storeToRefs: () => ({
    selectedBatch: ref({
      batch: {
        batch_id: 'batch-test',
        status: 'partial_failed',
        total_files: 3,
        completed_files: 1,
        failed_files: 1,
        progress_percent: 66,
        success_rate: 33,
        failure_rate: 33,
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
          track: [],
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
          track: [],
          ocr_final_hit_nodes: [],
          error_code: '',
          error_message: '',
          updated_at: '2026-06-10T10:03:00+08:00'
        },
        {
          document_id: 'doc-processing',
          batch_id: 'batch-test',
          file_name: 'processing.pdf',
          file_type: 'pdf',
          status: 'processing',
          stage: 'ocr_images',
          progress_percent: 50,
          current_page: 1,
          total_pages: 2,
          duration_ms: 1000,
          track: [],
          ocr_final_hit_nodes: [],
          error_code: '',
          error_message: '',
          updated_at: '2026-06-10T10:05:00+08:00'
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
  useAutoRefresh: () => undefined
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
    deleteDocument
  }
})

import BatchDetailView from '@/views/BatchDetailView.vue'

async function flushAsyncActions(): Promise<void> {
  await Promise.resolve()
  await Promise.resolve()
}

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

describe('BatchDetailView document delete', () => {
  beforeEach(() => {
    loadBatchDetail.mockClear()
    deleteDocument.mockClear()
  })

  it('shows delete button for failed documents and refreshes detail after confirmed delete', async () => {
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

    const deleteButton = wrapper.findAll('button').find((candidate) => candidate.text() === '删除')

    expect(deleteButton).toBeDefined()
    await deleteButton?.trigger('click')
    const confirmButtons = Array.from(document.body.querySelectorAll('button'))
      .filter((candidate) => candidate.textContent?.trim() === '确认删除')
    expect(confirmButtons.length).toBeGreaterThan(0)
    ;(confirmButtons[0] as HTMLButtonElement | undefined)?.click()
    await flushAsyncActions()
    expect(deleteDocument).toHaveBeenCalledWith('doc-failed')
    expect(loadBatchDetail).toHaveBeenCalledWith('batch-test')
  })

  it('does not enable delete for processing documents', async () => {
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

    const deleteButtons = wrapper.findAll('button').filter((candidate) => candidate.text() === '删除')

    expect(deleteButtons.length).toBeGreaterThan(0)
    expect(deleteButtons.some((candidate) => candidate.attributes('disabled') !== undefined)).toBe(true)
  })
})
