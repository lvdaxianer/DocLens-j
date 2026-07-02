import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'

import OcrNodeTable from '../OcrNodeTable.vue'
import type { OcrNode } from '@/types/ocrResources'

/**
 * OCR 节点表测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
describe('OcrNodeTable', () => {
  it('shows each node max concurrency next to runtime load', () => {
    const wrapper = mount(OcrNodeTable, {
      props: {
        nodes: [node()]
      }
    })

    expect(wrapper.text()).toContain('最大并发')
    expect(wrapper.text()).toContain('10')
  })

  it('shows node action status while an operation is running', () => {
    const wrapper = mount(OcrNodeTable, {
      props: {
        nodes: [node()],
        nodeActionById: {
          ocr_node_a: 'test'
        }
      }
    })

    expect(wrapper.text()).toContain('测试中')
  })
})

/**
 * 创建 OCR 节点。
 *
 * @returns OCR 节点
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
function node(): OcrNode {
  return {
    id: 'ocr_node_a',
    model_key: 'paddle_ocr',
    deployment_type: 'OFFLINE',
    name: 'Paddle 节点 A',
    host: '10.0.0.1',
    port: 18081,
    channel_key: '',
    provider_model: '',
    credential_configured: false,
    enabled: true,
    participate_global: true,
    weight: 100,
    max_concurrency: 10,
    status: 'UP',
    inflight_images: 2,
    queued_images: 0,
    processed_images_today: 20,
    success_images: 20,
    failed_images: 0,
    avg_latency_ms: 100,
    p95_latency_ms: 150,
    failure_count: 0,
    recovery_success_count: 0,
    last_health_at: '2026-06-21T10:00:00+08:00',
    circuit_open_until: '',
    last_manual_recovery_at: '',
    last_error: ''
  }
}
