import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'

import OcrResourceMetricCards from '../OcrResourceMetricCards.vue'
import type { DashboardOcrResources } from '@/types/dashboard'

const OCR_REQUEST_CONCURRENCY_LABEL = 'OCR 请求并发'
const OCR_REQUEST_CONCURRENCY_VALUE = '12 / 30'
const OCR_REQUEST_NOTE = '队列 4 · 当前线程 18'
const PAGE_TASK_CONCURRENCY_LABEL = '页任务并发'
const PAGE_TASK_CONCURRENCY_VALUE = '9 / 30'
const PAGE_TASK_NOTE = '批量 30 · 运行线程 11'
const HEALTHY_NODE_COUNT = 3
const DOWN_NODE_COUNT = 0
const RECOVERING_NODE_COUNT = 0
const GLOBAL_INFLIGHT_IMAGES = 12
const OCR_REQUEST_ACTIVE_COUNT = 12
const OCR_REQUEST_QUEUE_SIZE = 4
const OCR_REQUEST_POOL_SIZE = 18
const OCR_REQUEST_MAXIMUM_POOL_SIZE = 30
const PAGE_TASK_ACTIVE_COUNT = 9
const PAGE_TASK_QUEUE_SIZE = 0
const PAGE_TASK_POOL_SIZE = 30
const PAGE_TASK_RUNTIME_POOL_SIZE = 11
const PAGE_TASK_BATCH_SIZE = 30
const OCR_HEALTH_ACTIVE_COUNT = 1
const OCR_HEALTH_QUEUE_SIZE = 0
const LLM_CHUNK_ACTIVE_COUNT = 2
const LLM_CHUNK_QUEUE_SIZE = 1

/**
 * OCR 资源指标卡组件测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
describe('OcrResourceMetricCards', () => {
  it('shows active OCR work against configured capacity', () => {
    const wrapper = mount(OcrResourceMetricCards, {
      props: {
        metrics: metrics()
      }
    })

    expect(wrapper.text()).toContain(OCR_REQUEST_CONCURRENCY_LABEL)
    expect(wrapper.text()).toContain(OCR_REQUEST_CONCURRENCY_VALUE)
    expect(wrapper.text()).toContain(OCR_REQUEST_NOTE)
    expect(wrapper.text()).toContain(PAGE_TASK_CONCURRENCY_LABEL)
    expect(wrapper.text()).toContain(PAGE_TASK_CONCURRENCY_VALUE)
    expect(wrapper.text()).toContain(PAGE_TASK_NOTE)
  })
})

/**
 * 创建 OCR 资源指标。
 *
 * @returns OCR 资源指标
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
function metrics(): DashboardOcrResources {
  return {
    healthy_node_count: HEALTHY_NODE_COUNT,
    down_node_count: DOWN_NODE_COUNT,
    recovering_node_count: RECOVERING_NODE_COUNT,
    global_inflight_images: GLOBAL_INFLIGHT_IMAGES,
    nodes: [],
    busiest_node: {},
    thread_pools: {
      ocr_request: {
        active_count: OCR_REQUEST_ACTIVE_COUNT,
        queue_size: OCR_REQUEST_QUEUE_SIZE,
        pool_size: OCR_REQUEST_POOL_SIZE,
        core_pool_size: OCR_REQUEST_MAXIMUM_POOL_SIZE,
        maximum_pool_size: OCR_REQUEST_MAXIMUM_POOL_SIZE,
        largest_pool_size: OCR_REQUEST_POOL_SIZE
      },
      page_task_worker: {
        active_count: PAGE_TASK_ACTIVE_COUNT,
        queue_size: PAGE_TASK_QUEUE_SIZE,
        pool_size: PAGE_TASK_POOL_SIZE,
        runtime_pool_size: PAGE_TASK_RUNTIME_POOL_SIZE,
        batch_size: PAGE_TASK_BATCH_SIZE
      },
      ocr_health: {
        active_count: OCR_HEALTH_ACTIVE_COUNT,
        queue_size: OCR_HEALTH_QUEUE_SIZE
      },
      llm_markdown_chunk: {
        active_count: LLM_CHUNK_ACTIVE_COUNT,
        queue_size: LLM_CHUNK_QUEUE_SIZE
      }
    }
  }
}
