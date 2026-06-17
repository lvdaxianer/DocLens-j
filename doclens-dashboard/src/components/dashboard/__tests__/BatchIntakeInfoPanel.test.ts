import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'

import BatchIntakeInfoPanel from '../BatchIntakeInfoPanel.vue'
import type { BatchRow } from '@/types/dashboard'

const INTAKE_BATCH: BatchRow = {
  batch_id: 'batch-test',
  status: 'completed',
  total_files: 1,
  completed_files: 1,
  failed_files: 0,
  progress_percent: 100,
  success_rate: 100,
  failure_rate: 0,
  average_duration_ms: 1200,
  created_at: '2026-06-17T10:00:00+08:00',
  updated_at: '2026-06-17T10:05:00+08:00',
  callback_url: 'https://client.example.com/ocr-callback',
  idempotency_key: 'openwebui:file:file-123:hash:abc',
  metadata: {
    source: 'open-webui',
    openwebui_file_id: 'file-123'
  }
}

/**
 * 批次接入信息面板组件测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
describe('BatchIntakeInfoPanel', () => {
  /**
   * 应展示回调地址、幂等值和格式化后的 metadata。
   *
   * @author lvdaxianerplus
   * @date 2026-06-17
   */
  it('renders callback url, idempotency key, and formatted metadata', () => {
    const wrapper = mount(BatchIntakeInfoPanel, {
      props: {
        batch: INTAKE_BATCH
      }
    })

    expect(wrapper.text()).toContain('接入信息')
    expect(wrapper.text()).toContain('https://client.example.com/ocr-callback')
    expect(wrapper.text()).toContain('openwebui:file:file-123:hash:abc')
    expect(wrapper.text()).toContain('"source": "open-webui"')
    expect(wrapper.text()).toContain('"openwebui_file_id": "file-123"')
  })

  /**
   * 空接入字段应显示稳定占位。
   *
   * @author lvdaxianerplus
   * @date 2026-06-17
   */
  it('renders stable placeholders for missing intake fields', () => {
    const wrapper = mount(BatchIntakeInfoPanel, {
      props: {
        batch: {
          ...INTAKE_BATCH,
          callback_url: '',
          idempotency_key: '',
          metadata: {}
        }
      }
    })

    expect(wrapper.text()).toContain('未提供回调地址')
    expect(wrapper.text()).toContain('未提供幂等值')
    expect(wrapper.text()).toContain('无 meta 信息')
  })
})
