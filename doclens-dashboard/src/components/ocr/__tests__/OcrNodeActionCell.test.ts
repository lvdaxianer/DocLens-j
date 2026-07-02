import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { NConfigProvider } from 'naive-ui'
import { defineComponent, h } from 'vue'

import OcrNodeActionCell from '../OcrNodeActionCell.vue'
import type { OcrNode } from '@/types/ocrResources'

const MountHost = defineComponent({
  name: 'MountHost',
  setup() {
    return () => h(NConfigProvider, null, {
      default: () => h(OcrNodeActionCell, { node: node() })
    })
  }
})

/**
 * OCR 节点行操作测试。
 *
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
describe('OcrNodeActionCell', () => {
  it('names the target and consequence in delete confirmation', async () => {
    const wrapper = mount(MountHost)
    const deleteButton = wrapper.findAll('button').find((candidate) => candidate.attributes('title') === '删除')

    expect(deleteButton).toBeDefined()
    await deleteButton?.trigger('click')
    expect(document.body.textContent).toContain('OCR 节点 Paddle 节点 A（ocr_node_a）')
    expect(document.body.textContent).toContain('不可恢复')
  })
})

/**
 * 创建 OCR 节点测试数据。
 *
 * @returns OCR 节点
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
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
