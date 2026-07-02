import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, ref } from 'vue'
import { mount } from '@vue/test-utils'

const { deleteBatch, loadSummary, warningMessage } = vi.hoisted(() => ({
  deleteBatch: vi.fn(() => Promise.reject(new Error(
    '请求失败：400 batch contains non-deletable document doc-1 status queued'
  ))),
  loadSummary: vi.fn(() => Promise.resolve()),
  warningMessage: vi.fn()
}))

vi.mock('naive-ui', async () => {
  const actual = await vi.importActual<typeof import('naive-ui')>('naive-ui')
  return {
    ...actual,
    useMessage: () => ({ warning: warningMessage, error: vi.fn(), success: vi.fn() })
  }
})

vi.mock('pinia', () => ({
  storeToRefs: () => ({
      summary: ref({
        overview: null,
        stage_status_counts: [],
        image_progress: null,
        ocr_resources: {
          healthy_node_count: 2,
          down_node_count: 0,
          recovering_node_count: 0,
          global_inflight_images: 1,
          nodes: [],
          busiest_node: {},
          thread_pools: {
            ocr_request: { active_count: 0, queue_size: 0 },
            ocr_health: { active_count: 0, queue_size: 0 },
            callback: { active_count: 0, queue_size: 0 },
            llm_markdown_chunk: { active_count: 3, queue_size: 5 }
          }
        },
        recent_batches: [
          {
            batch_id: 'batch-test',
          status: 'queued',
          total_files: 2,
          completed_files: 0,
          failed_files: 0,
          progress_percent: 0,
          success_rate: 0,
          failure_rate: 0,
          average_duration_ms: 0,
          created_at: '2026-06-11T10:00:00+08:00',
          updated_at: '2026-06-11T10:00:00+08:00'
        }
      ],
      recent_failures: []
    }),
    summaryState: ref({ loading: false, error: '', lastUpdated: '2026-06-11T10:00:00+08:00' })
  })
}))

vi.mock('@/stores/dashboard', () => ({
  useDashboardStore: () => ({ loadSummary })
}))

vi.mock('@/api/dashboard', async () => {
  const actual = await vi.importActual<typeof import('@/api/dashboard')>('@/api/dashboard')
  return {
    ...actual,
    deleteBatch
  }
})

vi.mock('@/composables/useAutoRefresh', () => ({
  DEFAULT_REFRESH_INTERVAL_SECONDS: 5,
  useAutoRefresh: (refresh: () => Promise<void>) => ({
    isStale: { value: false },
    lastErrorMessage: { value: '' },
    refreshNow: refresh
  })
}))

import OverviewView from '@/views/OverviewView.vue'

const BatchTableStub = defineComponent({
  name: 'BatchTable',
  emits: ['delete-batch'],
  /**
   * 渲染只保留删除事件的批次表格测试替身。
   *
   * @param _ 未使用的 props
   * @param context 组件上下文
   * @returns 渲染函数
   * @author lvdaxianerplus
   * @date 2026-06-11
   */
  setup(_, { emit }) {
    return () => h('button', {
      onClick: () => emit('delete-batch', 'batch-test')
    }, '删除')
  }
})

/**
 * 等待 Vue 与异步 Promise 队列完成。
 *
 * @returns 异步动作完成信号
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
async function flushAsyncActions(): Promise<void> {
  await Promise.resolve()
  await Promise.resolve()
}

describe('OverviewView batch delete', () => {
  beforeEach(() => {
    warningMessage.mockClear()
    deleteBatch.mockClear()
    loadSummary.mockClear()
  })

  it('shows friendly warning when queued documents prevent batch delete', async () => {
    const wrapper = mount(OverviewView, {
      global: {
        stubs: {
          BatchTable: BatchTableStub,
          FailureList: true,
          LatencyChart: true,
          MetricStrip: true,
          OcrResourceMetricCards: true,
          StageStatusBoard: true,
          NAlert: true,
          NButton: true,
          NSpin: true
        }
      }
    })

    const deleteButton = wrapper.findAll('button').find((candidate) => candidate.text() === '删除')

    expect(deleteButton).toBeDefined()
    await deleteButton?.trigger('click')
    await flushAsyncActions()

    expect(deleteBatch).toHaveBeenCalledWith('batch-test')
    expect(warningMessage).toHaveBeenCalledWith('批次中还有等待或处理中的任务，暂时不能删除')
  })
})
