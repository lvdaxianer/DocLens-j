import { afterEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'

import DocumentResultDrawer from '@/components/dashboard/DocumentResultDrawer.vue'
import type { DocumentResultResponse, DocumentRow } from '@/types/dashboard'

const clipboardWriteText = vi.fn(() => Promise.resolve())

Object.defineProperty(navigator, 'clipboard', {
  configurable: true,
  value: {
    writeText: clipboardWriteText
  }
})

const documentRow: DocumentRow = {
  document_id: 'doc-1',
  batch_id: 'batch-1',
  file_name: 'report.md',
  file_type: 'md',
  status: 'completed',
  stage: 'completed',
  progress_percent: 100,
  current_page: 1,
  total_pages: 1,
  duration_ms: 1000,
  track: [],
  ocr_final_hit_nodes: [],
  error_code: '',
  error_message: '',
  updated_at: '2026-06-10T10:00:00+08:00'
}

const result: DocumentResultResponse = {
  document_id: 'doc-1',
  result_id: 'result-1',
  result: {
    finalText: '## 标题\n\n正文 **重点**',
    llm_markdown_applied: true,
    markdownStorageUri: 'local://result.md',
    pages: [],
    confidence: 0.92,
    warnings: [],
    summary: {
      pageCount: 1,
      blockCount: 2,
      tableCount: 0,
      confidence: 0.92
    }
  }
}

describe('DocumentResultDrawer', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('renders markdown preview and copies final text', async () => {
    clipboardWriteText.mockClear()

    const wrapper = mount(DocumentResultDrawer, {
      props: {
        show: true,
        document: documentRow,
        result,
        loading: false,
        error: ''
      },
      global: {
        stubs: {
          NDrawer: { template: '<div><slot /></div>' },
          NDrawerContent: { template: '<div><slot /><slot name="footer" /></div>' },
          NDescriptions: { template: '<dl><slot /></dl>' },
          NDescriptionsItem: { template: '<div><slot /></div>' },
          NSpin: { template: '<div><slot /></div>' },
          NTag: { template: '<span><slot /></span>' },
          NAlert: { template: '<div><slot /></div>' }
        }
      }
    })

    expect(document.body.innerHTML).toContain('<h2>标题</h2>')
    expect(document.body.innerHTML).toContain('<strong>重点</strong>')

    const copyButton = Array.from(document.body.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('复制 Markdown'))
    expect(copyButton).toBeDefined()
    copyButton?.click()
    await wrapper.vm.$nextTick()

    expect(clipboardWriteText).toHaveBeenCalledWith(result.result.finalText)
  })
})
