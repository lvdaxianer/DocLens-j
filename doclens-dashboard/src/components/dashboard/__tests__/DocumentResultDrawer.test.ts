import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'

import DocumentResultDrawer from '../DocumentResultDrawer.vue'
import type { DocumentResultResponse, DocumentRow } from '@/types/dashboard'

vi.mock('naive-ui', () => ({
  NAlert: passthrough('section', 'n-alert'),
  NButton: passthrough('button', 'n-button'),
  NDescriptions: passthrough('dl', 'n-descriptions'),
  NDescriptionsItem: passthrough('div', 'n-descriptions-item'),
  NDrawer: passthrough('aside', 'n-drawer'),
  NDrawerContent: passthrough('div', 'n-drawer-content'),
  NSpin: passthrough('div', 'n-spin'),
  NTabPane: defineComponent({
    props: { name: { type: String, required: true }, tab: { type: String, required: true } },
    setup(props, { slots }) {
      return () => h('section', { class: 'n-tab-pane', 'data-tab-name': props.name }, slots.default?.())
    }
  }),
  NTabs: defineComponent({
    props: { value: { type: String, required: true } },
    emits: ['update:value'],
    setup(props, { slots, emit }) {
      return () => h('div', { class: 'n-tabs' }, [
        h('button', {
          class: 'n-tab',
          type: 'button',
          onClick: () => emit('update:value', 'ocr-original')
        }, 'OCR 原内容'),
        h('div', { 'data-active-tab': props.value }, slots.default?.())
      ])
    }
  }),
  NTag: passthrough('span', 'n-tag'),
  useMessage: () => ({ error: vi.fn(), success: vi.fn(), warning: vi.fn() })
}))

/**
 * 文档结果抽屉组件测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
describe('DocumentResultDrawer', () => {
  /**
   * 结果内容应左右 Tab 切换，避免 Markdown 和 OCR 原内容上下堆叠。
   *
   * @author lvdaxianerplus
   * @date 2026-06-12
   */
  it('renders markdown and ocr original content as switchable tabs', async () => {
    const wrapper = mount(DocumentResultDrawer, {
      props: {
        show: true,
        document: documentRow(),
        result: documentResult(),
        loading: false,
        error: ''
      }
    })

    expect(wrapper.find('.document-result__tabs').exists()).toBe(true)
    expect(wrapper.findAll('.document-result__text')).toHaveLength(1)
    expect(wrapper.text()).toContain('Markdown 内容')
    expect(wrapper.text()).toContain('# Markdown 结果')
    expect(wrapper.text()).not.toContain('原始 OCR 文本')

    await wrapper.find('.n-tab').trigger('click')

    expect(wrapper.findAll('.document-result__text')).toHaveLength(1)
    expect(wrapper.text()).toContain('OCR 原内容')
    expect(wrapper.text()).toContain('原始 OCR 文本')
    expect(wrapper.text()).not.toContain('# Markdown 结果')
  })
})

/**
 * 创建透传型测试组件。
 *
 * @param tag 标签名
 * @param className class 名称
 * @returns Vue 测试组件
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
function passthrough(tag: string, className: string) {
  return defineComponent({
    setup(_, { slots, attrs }) {
      return () => h(tag, { ...attrs, class: className }, slots.default?.())
    }
  })
}

/**
 * 创建文档行测试数据。
 *
 * @returns 文档行
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
function documentRow(): DocumentRow {
  return {
    document_id: 'doc-1',
    batch_id: 'batch-1',
    file_name: 'invoice.pdf',
    file_type: 'pdf',
    status: 'COMPLETED',
    stage: 'DONE',
    progress_percent: 100,
    current_page: 1,
    total_pages: 1,
    duration_ms: 1000,
    track: [],
    ocr_final_hit_nodes: [],
    error_code: '',
    error_message: '',
    updated_at: '2026-06-12 10:00:00'
  }
}

/**
 * 创建文档结果测试数据。
 *
 * @returns 文档结果
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
function documentResult(): DocumentResultResponse {
  return {
    document_id: 'doc-1',
    result_id: 'result-1',
    result: {
      finalText: '# Markdown 结果',
      llm_markdown_applied: true,
      rawVendorOutput: { ocr_text: '原始 OCR 文本' },
      markdownStorageUri: 'file:///tmp/result.md',
      pages: [],
      confidence: 0.98,
      warnings: [],
      summary: {
        pageCount: 1,
        blockCount: 1,
        tableCount: 0,
        confidence: 0.98
      }
    }
  }
}
