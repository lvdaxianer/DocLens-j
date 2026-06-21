import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, ref, type VNodeChild } from 'vue'
import { mount } from '@vue/test-utils'
import { NConfigProvider } from 'naive-ui'

const openDocumentResult = vi.fn(() => Promise.resolve())

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { batchId: 'batch-test' } }),
  useRouter: () => ({ back: vi.fn() })
}))

vi.mock('pinia', () => ({
  storeToRefs: () => ({
    selectedBatch: ref({
      batch: {
        batch_id: 'batch-test',
        status: 'completed',
        total_files: 1,
        completed_files: 1,
        failed_files: 0,
        progress_percent: 100,
        success_rate: 100,
        failure_rate: 0,
        average_duration_ms: 1000,
        created_at: '2026-06-10T10:00:00+08:00',
        updated_at: '2026-06-10T10:05:00+08:00'
      },
      documents: [
        {
          document_id: 'doc-completed',
          batch_id: 'batch-test',
          file_name: 'completed.docx',
          file_type: 'docx',
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
    loadBatchDetail: vi.fn(() => Promise.resolve())
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
    openDocumentResult,
    retryDocumentResult: vi.fn()
  })
}))

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

describe('BatchDetailView document result drawer', () => {
  beforeEach(() => {
    openDocumentResult.mockClear()
  })

  it('opens document result drawer from completed document action', async () => {
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

    const resultButton = wrapper.findAll('button').find((candidate) => candidate.text() === '查看文本')

    expect(resultButton).toBeDefined()
    await resultButton?.trigger('click')
    expect(openDocumentResult).toHaveBeenCalledWith(expect.objectContaining({
      document_id: 'doc-completed'
    }))
  })

  it('shows document chunk count in the batch detail track summary', () => {
    const wrapper = mount(MountHost, {
      global: {
        stubs: {
          NAlert: true,
          NButton: true,
          NProgress: true,
          NDataTable: DataTableStub,
          BatchOcrRoutePanel: true,
          DocumentResultDrawer: true,
          DocumentTrackCards: false,
          StatusTag: true
        }
      }
    })

    expect(wrapper.text()).toContain('分块：4 个')
  })
})
