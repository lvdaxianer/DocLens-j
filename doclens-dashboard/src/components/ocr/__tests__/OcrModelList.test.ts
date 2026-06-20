import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'

import OcrModelList from '../OcrModelList.vue'
import type { OcrModel } from '@/types/ocrResources'

/**
 * OCR 模型列表测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
describe('OcrModelList', () => {
  it('shows model concurrency capacity and global capacity', () => {
    const wrapper = mount(OcrModelList, {
      props: {
        models: [model()],
        selectedModelKey: 'paddle_ocr'
      }
    })

    expect(wrapper.text()).toContain('并发 3 / 30')
    expect(wrapper.text()).toContain('启用容量 30')
    expect(wrapper.text()).toContain('全局容量 20')
  })
})

/**
 * 创建 OCR 模型。
 *
 * @returns OCR 模型
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
function model(): OcrModel {
  return {
    model_key: 'paddle_ocr',
    name: 'PaddleOCR',
    description: 'PaddleOCR native-compatible HTTP API',
    supported_inputs: ['image'],
    ocr_path: '/ocr',
    health_path: '/ocr',
    node_count: 4,
    healthy_node_count: 3,
    enabled_node_count: 3,
    inflight_images: 3,
    max_concurrency: 30,
    enabled_max_concurrency: 30,
    global_max_concurrency: 20
  }
}
