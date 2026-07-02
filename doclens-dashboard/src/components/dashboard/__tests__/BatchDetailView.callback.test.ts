import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, ref } from 'vue'
import { mount } from '@vue/test-utils'
import { NConfigProvider } from 'naive-ui'

const { retryCallbackJob } = vi.hoisted(() => ({
  retryCallbackJob: vi.fn(() => Promise.resolve())
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
        status: 'completed',
        total_files: 1,
        completed_files: 1,
        failed_files: 0,
        progress_percent: 100,
        success_rate: 100,
        failure_rate: 0,
        average_duration_ms: 1000,
        created_at: '2026-06-16T10:00:00+08:00',
        updated_at: '2026-06-16T10:05:00+08:00',
        callback_url: 'https://client.example.com/ocr-callback',
        idempotency_key: 'openwebui:file:file-123:hash:abc',
        metadata: {
          source: 'open-webui',
          openwebui_file_id: 'file-123'
        }
      },
      documents: [],
      events: [],
      callback_jobs: [
        {
          callback_job_id: 'callback-1',
          event_id: 'event-1',
          batch_id: 'batch-test',
          document_id: 'doc-1',
          callback_url: 'https://callback.example.test/done',
          status: 'failed',
          retry_count: 3,
          next_retry_at: '',
          failure_reason: 'http_status',
          failure_detail: 'HTTP 503',
          updated_at: '2026-06-16T10:04:00+08:00'
        }
      ],
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
      lastUpdated: '2026-06-16T10:05:00+08:00'
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
    retryCallbackJob
  }
})

import BatchDetailView from '@/views/BatchDetailView.vue'

const MountHost = defineComponent({
  name: 'MountHost',
  setup() {
    return () => h(NConfigProvider, null, {
      default: () => h(BatchDetailView)
    })
  }
})

describe('BatchDetailView callback visibility', () => {
  beforeEach(() => {
    loadBatchDetail.mockClear()
    retryCallbackJob.mockClear()
  })

  it('shows callback status and failure detail on the batch detail page', () => {
    const wrapper = mount(MountHost, {
      global: {
        stubs: {
          NAlert: true,
          NButton: true,
          NProgress: true,
          NDataTable: true,
          BatchOcrRoutePanel: true,
          DocumentResultDrawer: true,
          DocumentTrackCards: true,
          StatusTag: true
        }
      }
    })

    expect(wrapper.text()).toContain('回调投递结果')
    expect(wrapper.text()).toContain('失败')
    expect(wrapper.text()).toContain('http_status')
    expect(wrapper.text()).toContain('HTTP 503')
  })

  it('shows batch intake information on the batch detail page', () => {
    const wrapper = mount(MountHost, {
      global: {
        stubs: {
          NAlert: true,
          NButton: true,
          NProgress: true,
          NDataTable: true,
          BatchOcrRoutePanel: true,
          DocumentResultDrawer: true,
          DocumentTrackCards: true,
          StatusTag: true
        }
      }
    })

    expect(wrapper.text()).toContain('接入信息')
    expect(wrapper.text()).toContain('https://client.example.com/ocr-callback')
    expect(wrapper.text()).toContain('openwebui:file:file-123:hash:abc')
    expect(wrapper.text()).toContain('"source": "open-webui"')
  })

  it('retries failed callback job and refreshes batch detail', async () => {
    const wrapper = mount(MountHost, {
      global: {
        stubs: {
          NAlert: true,
          NButton: false,
          NProgress: true,
          NDataTable: true,
          BatchOcrRoutePanel: true,
          DocumentResultDrawer: true,
          DocumentTrackCards: true,
          StatusTag: true
        }
      }
    })

    const retryButton = wrapper.findAll('button').find((candidate) => candidate.text() === '重试回调')

    expect(retryButton).toBeDefined()
    await retryButton?.trigger('click')
    expect(retryCallbackJob).toHaveBeenCalledWith('callback-1')
    expect(loadBatchDetail).toHaveBeenCalledWith('batch-test')
  })
})
