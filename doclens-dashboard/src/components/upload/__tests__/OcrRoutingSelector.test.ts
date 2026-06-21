import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

import OcrRoutingSelector from '../OcrRoutingSelector.vue'
import type { OcrModel } from '@/types/ocrResources'

const PADDLE_MODEL_KEY = 'paddle_ocr'
const OLLAMA_MODEL_KEY = 'ollama'

vi.mock('@/api/ocrResources', () => ({
  fetchOcrModels: vi.fn(async () => ({
    items: [
      model(OLLAMA_MODEL_KEY, 'Ollama'),
      model(PADDLE_MODEL_KEY, 'PaddleOCR')
    ]
  })),
  fetchOcrNodes: vi.fn(async () => ({ items: [] }))
}))

/**
 * OCR 路由选择器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
describe('OcrRoutingSelector', () => {
  it('keeps global load balance selected after models load', async () => {
    const wrapper = mount(OcrRoutingSelector)

    await flushPromises()

    const emittedValues = wrapper.emitted('change')?.flat() ?? []
    expect(emittedValues.at(-1)).toMatchObject({
      ocrRoutingMode: 'GLOBAL_LOAD_BALANCE',
      ocrModelKey: '',
      ocrNodeId: '',
      ocrLoadBalanceStrategy: 'weighted-idle'
    })
  })
})

/**
 * 创建 OCR 模型测试数据。
 *
 * @param modelKey - 模型标识
 * @param name - 模型名称
 * @returns OCR 模型
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
function model(modelKey: string, name: string): OcrModel {
  return {
    model_key: modelKey,
    name,
    description: '',
    supported_inputs: ['image'],
    ocr_path: '/ocr',
    health_path: '/ocr',
    node_count: 1,
    healthy_node_count: 1,
    enabled_node_count: 1
  }
}
