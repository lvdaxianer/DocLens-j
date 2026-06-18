import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'
import { mount } from '@vue/test-utils'

import OcrNodeFormDrawer from '../OcrNodeFormDrawer.vue'
import type { OcrModel } from '@/types/ocrResources'

vi.mock('naive-ui', () => ({
  NButton: passthrough('button', 'n-button'),
  NDrawer: passthrough('aside', 'n-drawer'),
  NDrawerContent: passthrough('section', 'n-drawer-content'),
  NForm: passthrough('form', 'n-form'),
  NFormItem: passthrough('div', 'n-form-item'),
  NInput: passthrough('input', 'n-input'),
  NInputNumber: passthrough('input', 'n-input-number'),
  NRadioButton: passthrough('button', 'n-radio-button'),
  NRadioGroup: passthrough('div', 'n-radio-group'),
  NSelect: passthrough('div', 'n-select'),
  NSwitch: passthrough('button', 'n-switch')
}))

/**
 * OCR 节点表单抽屉组件测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-18
 */
describe('OcrNodeFormDrawer', () => {
  /**
   * Ollama family 节点应显示自由输入提示。
   *
   * @author lvdaxianerplus
   * @date 2026-06-18
   */
  it('shows an Ollama free-input hint for real family keys', () => {
    const wrapper = mount(OcrNodeFormDrawer, {
      props: {
        visible: true,
        selectedModelKey: 'ollama_deepseek_ocr',
        models: [model('ollama_deepseek_ocr'), model('paddle_ocr')],
        loading: false
      }
    })

    expect(wrapper.text()).toContain('Ollama family 节点可自由填写真实 provider model')
    expect(wrapper.text()).toContain('deepseek-ocr:latest')
  })

  /**
   * PaddleOCR 节点不应显示 Ollama 自由输入提示。
   *
   * @author lvdaxianerplus
   * @date 2026-06-18
   */
  it('does not show the Ollama hint for paddle ocr nodes', () => {
    const wrapper = mount(OcrNodeFormDrawer, {
      props: {
        visible: true,
        selectedModelKey: 'paddle_ocr',
        models: [model('paddle_ocr')],
        loading: false
      }
    })

    expect(wrapper.text()).not.toContain('Ollama family 节点可自由填写真实 provider model')
  })
})

/**
 * 创建模型测试数据。
 *
 * @param modelKey - OCR 模型标识
 * @returns OCR 模型
 * @author lvdaxianerplus
 * @date 2026-06-18
 */
function model(modelKey: string): OcrModel {
  return {
    model_key: modelKey,
    name: modelKey,
    description: '',
    supported_inputs: ['image'],
    ocr_path: '/ocr',
    health_path: '/health',
    node_count: 0,
    healthy_node_count: 0,
    enabled_node_count: 0
  }
}

/**
 * 创建透传型测试组件。
 *
 * @param tag - 标签名
 * @param className - class 名称
 * @returns Vue 测试组件
 * @author lvdaxianerplus
 * @date 2026-06-18
 */
function passthrough(tag: string, className: string) {
  return defineComponent({
    setup(_, { slots, attrs }) {
      return () => h(tag, { ...attrs, class: className }, slots.default?.())
    }
  })
}
