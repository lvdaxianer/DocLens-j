import { describe, expect, it } from 'vitest'
import { defineComponent, h } from 'vue'
import { mount } from '@vue/test-utils'
import { NConfigProvider } from 'naive-ui'

import BatchTable from '@/components/dashboard/BatchTable.vue'
import type { BatchRow } from '@/types/dashboard'

const batch: BatchRow = {
  batch_id: 'batch-test',
  status: 'completed',
  total_files: 2,
  completed_files: 2,
  failed_files: 0,
  progress_percent: 100,
  success_rate: 100,
  failure_rate: 0,
  average_duration_ms: 1200,
  created_at: '2026-06-10T10:00:00+08:00',
  updated_at: '2026-06-10T10:05:00+08:00'
}

const MountHost = defineComponent({
  name: 'MountHost',
  setup() {
    return () => h(NConfigProvider, null, {
      default: () => h(BatchTable, { batches: [batch] })
    })
  }
})

async function flushAsyncActions(): Promise<void> {
  await Promise.resolve()
  await Promise.resolve()
}

describe('BatchTable batch delete', () => {
  it('emits delete-batch after confirmed delete', async () => {
    const wrapper = mount(MountHost, {
      global: {
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a><slot /></a>'
          }
        }
      }
    })

    const deleteButton = wrapper.findAll('button').find((candidate) => candidate.text() === '删除')

    expect(deleteButton).toBeDefined()
    await deleteButton?.trigger('click')
    expect(document.body.textContent).toContain('批次 batch-test')
    expect(document.body.textContent).toContain('不可恢复')
    const confirmButtons = Array.from(document.body.querySelectorAll('button'))
      .filter((candidate) => candidate.textContent?.trim() === '确认删除')
    expect(confirmButtons.length).toBeGreaterThan(0)
    ;(confirmButtons[0] as HTMLButtonElement | undefined)?.click()
    await flushAsyncActions()
    const table = wrapper.findComponent(BatchTable)
    expect(table.emitted('delete-batch')).toEqual([[batch.batch_id]])
  })
})
