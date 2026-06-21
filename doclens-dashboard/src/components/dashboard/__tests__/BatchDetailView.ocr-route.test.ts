import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, ref } from 'vue'
import { mount } from '@vue/test-utils'
import { NConfigProvider } from 'naive-ui'

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
        status: 'processing',
        total_files: 1,
        completed_files: 0,
        failed_files: 0,
        progress_percent: 40,
        success_rate: 0,
        failure_rate: 0,
        average_duration_ms: 1000,
        created_at: '2026-06-21T10:00:00+08:00',
        updated_at: '2026-06-21T10:05:00+08:00'
      },
      documents: [
        {
          document_id: 'doc-running',
          batch_id: 'batch-test',
          file_name: 'parallel.pdf',
          file_type: 'pdf',
          status: 'processing',
          stage: 'ocr_processing',
          progress_percent: 40,
          current_page: 2,
          total_pages: 5,
          duration_ms: 1000,
          llm_chunk_count: 0,
          track: [],
          ocr_running_hit_nodes: [],
          ocr_final_hit_nodes: [],
          error_code: '',
          error_message: '',
          updated_at: '2026-06-21T10:05:00+08:00'
        }
      ],
      events: [],
      callback_jobs: [],
      ocr_route_policy: {
        routing_mode: 'GLOBAL_LOAD_BALANCE',
        model_key: 'paddle_ocr',
        node_id: '',
        load_balance_strategy: 'least-inflight'
      },
      batch_dispatch_hit_nodes: [],
      ocr_running_page_tasks: [
        {
          task_id: 'task-1',
          batch_id: 'batch-test',
          document_id: 'doc-running',
          page_no: 1,
          worker_id: 'worker-a',
          thread_name: 'doclens-page-task-ocr-1',
          started_at: '2026-06-21T10:04:00+08:00',
          running_ms: 1200,
          model_key: 'paddle_ocr',
          node_id: 'node-1',
          node_name: '节点一'
        }
      ],
      failure_summary: {}
    }),
    detailState: ref({
      loading: false,
      error: '',
      lastUpdated: '2026-06-21T10:05:00+08:00'
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
    deleteDocument: vi.fn(() => Promise.resolve()),
    retryCallbackJob: vi.fn(() => Promise.resolve()),
    retryDocument: vi.fn(() => Promise.resolve())
  }
})

import BatchDetailView from '@/views/BatchDetailView.vue'

const OcrRoutePanelStub = defineComponent({
  name: 'BatchOcrRoutePanel',
  props: {
    runningPageTasks: {
      type: Array,
      default: () => []
    }
  },
  setup(props) {
    return () => h('div', { class: 'ocr-route-panel-stub' },
      (props.runningPageTasks as Array<{ thread_name: string }>).map((task) => task.thread_name).join(',')
    )
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

describe('BatchDetailView OCR route visibility', () => {
  beforeEach(() => {
    loadBatchDetail.mockClear()
  })

  it('passes live OCR page tasks to the route panel', () => {
    const wrapper = mount(MountHost, {
      global: {
        stubs: {
          NAlert: true,
          NButton: true,
          NProgress: true,
          NDataTable: true,
          BatchOcrRoutePanel: OcrRoutePanelStub,
          DocumentResultDrawer: true,
          DocumentTrackCards: true,
          StatusTag: true
        }
      }
    })

    expect(wrapper.text()).toContain('doclens-page-task-ocr-1')
  })
})
