import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'

import UploadDropzone from '../UploadDropzone.vue'

const warningMessage = vi.hoisted(() => vi.fn())

vi.mock('naive-ui', () => ({
  NButton: defineComponent({
    props: {
      disabled: Boolean,
      loading: Boolean,
      type: String
    },
    setup(props, { slots, attrs }) {
      return () => h('button', {
        ...attrs,
        disabled: props.disabled || props.loading,
        type: 'button'
      }, slots.default?.())
    }
  }),
  useMessage: () => ({ warning: warningMessage })
}))

vi.mock('@/components/dashboard/UploadFilePicker.vue', () => ({
  default: defineComponent({
    name: 'UploadFilePicker',
    emits: ['change'],
    setup(_, { emit }) {
      return () => h('button', {
        class: 'file-picker',
        type: 'button',
        onClick: () => emit('change', selectedFileList())
      }, '选择文件')
    }
  })
}))

vi.mock('@/components/dashboard/UploadFileList.vue', () => ({
  default: defineComponent({
    name: 'UploadFileList',
    props: { files: { type: Array, required: true } },
    setup(props) {
      return () => h('div', { class: 'file-list' }, String(props.files.length))
    }
  })
}))

vi.mock('@/components/upload/UploadAdvancedOptions.vue', () => ({
  default: defineComponent({
    name: 'UploadAdvancedOptions',
    setup() {
      return () => h('div', { class: 'advanced-options' })
    }
  })
}))

vi.mock('@/components/upload/UploadChunkStrategySelector.vue', () => ({
  default: defineComponent({
    name: 'UploadChunkStrategySelector',
    setup() {
      return () => h('div', { class: 'chunk-strategy' })
    }
  })
}))

vi.mock('@/components/upload/OcrRoutingSelector.vue', () => ({
  default: defineComponent({
    name: 'OcrRoutingSelector',
    emits: ['change'],
    setup(_, { expose }) {
      expose({
        validateRouting: () => '',
        reset: () => undefined
      })
      return () => h('div', { class: 'ocr-routing' })
    }
  })
}))

let selectedFiles: File[] = []

/**
 * 创建文件列表替身。
 *
 * @returns 文件列表替身
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
function selectedFileList(): FileList {
  return {
    length: selectedFiles.length,
    item: (index: number) => selectedFiles[index] ?? null,
    ...selectedFiles
  } as FileList
}

/**
 * 创建指定大小的文件。
 *
 * @param name - 文件名
 * @param size - 文件字节数
 * @returns 文件对象
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
function fileWithSize(name: string, size: number): File {
  const file = new File(['demo'], name, { type: 'text/plain' })
  Object.defineProperty(file, 'size', { value: size })
  return file
}

/**
 * 挂载上传表单并选择当前文件。
 *
 * @returns 上传表单包装器
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
async function mountAndSelectFiles() {
  const wrapper = mount(UploadDropzone)
  await wrapper.find('.file-picker').trigger('click')
  return wrapper
}

describe('UploadDropzone preflight limits', () => {
  beforeEach(() => {
    selectedFiles = []
    warningMessage.mockClear()
  })

  it('blocks submit when more than thirty files are selected', async () => {
    selectedFiles = Array.from({ length: 31 }, (_, index) => new File(['demo'], `demo-${index}.txt`))

    const wrapper = await mountAndSelectFiles()
    await wrapper.findAll('button').find((button) => button.text() === '上传并解析')?.trigger('click')

    expect(warningMessage).toHaveBeenCalledWith('单个批次最多上传 30 个文件，请拆分后再上传')
    expect(wrapper.emitted('submit')).toBeUndefined()
  })

  it('blocks submit when selected files exceed five hundred megabytes total', async () => {
    selectedFiles = [fileWithSize('large.txt', 501 * 1024 * 1024)]

    const wrapper = await mountAndSelectFiles()
    await wrapper.findAll('button').find((button) => button.text() === '上传并解析')?.trigger('click')

    expect(warningMessage).toHaveBeenCalledWith('单个批次最多上传 500 MB，请拆分后再上传')
    expect(wrapper.emitted('submit')).toBeUndefined()
  })
})
