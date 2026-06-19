import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import UploadChunkStrategySelector from '@/components/upload/UploadChunkStrategySelector.vue'

describe('UploadChunkStrategySelector', () => {
  it('shows the default preset and the four article presets', () => {
    const wrapper = mount(UploadChunkStrategySelector)

    expect(wrapper.text()).toContain('通用')
    expect(wrapper.text()).toContain('新闻类')
    expect(wrapper.text()).toContain('技术类')
    expect(wrapper.text()).toContain('论文类')
  })
})
