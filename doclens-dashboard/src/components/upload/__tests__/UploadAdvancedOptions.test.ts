import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'

import UploadAdvancedOptions from '../UploadAdvancedOptions.vue'
import type { UploadAdvancedOptionsValue } from '@/types/upload'

/**
 * 创建上传高级选项表单值。
 *
 * @returns 上传高级选项表单值
 * @author lvdaxianerplus
 * @date 2026-06-16
 */
function uploadAdvancedOptions(): UploadAdvancedOptionsValue {
  return {
    metadata: '{}',
    callbackUrl: '',
    idempotencyKey: ''
  }
}

describe('UploadAdvancedOptions', () => {
  it('describes idempotency key as a third-party pass-through value', () => {
    const wrapper = mount(UploadAdvancedOptions, {
      props: {
        modelValue: uploadAdvancedOptions(),
        'onUpdate:modelValue': () => {}
      }
    })

    expect(wrapper.text()).toContain('第三方')
    expect(wrapper.text()).toContain('透传')
    expect(wrapper.text()).not.toContain('重复提交')
  })
})
