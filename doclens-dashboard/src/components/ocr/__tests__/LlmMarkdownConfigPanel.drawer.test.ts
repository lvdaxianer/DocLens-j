import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, ref } from 'vue'
import { mount } from '@vue/test-utils'

import LlmMarkdownConfigPanel from '../LlmMarkdownConfigPanel.vue'

const loadConfig = vi.fn()
const openCreateDrawer = vi.fn()
const closeDrawer = vi.fn()
const resetForm = vi.fn()
const editConfig = vi.fn()
const saveConfig = vi.fn()
const toggleEnabled = vi.fn()
const makeDefault = vi.fn()
const removeConfig = vi.fn()
const testConfig = vi.fn()
const drawerVisible = ref(false)
const editingTitle = ref('新增配置')

vi.mock('naive-ui', () => ({
  NAlert: passthrough('div', 'n-alert'),
  NButton: passthrough('button', 'n-button'),
  NDataTable: defineComponent({
    props: {
      columns: {
        type: Array,
        required: true
      },
      data: {
        type: Array,
        required: true
      }
    },
    setup(_, { attrs }) {
      return () => h('div', { ...attrs, class: 'n-data-table' }, [
        h('button', {
          class: 'n-data-table__edit',
          type: 'button',
          onClick: () => {
            editingTitle.value = '默认 LLM 配置'
            drawerVisible.value = true
            editConfig()
          }
        }, '编辑')
      ])
    }
  }),
  NDrawer: defineComponent({
    props: {
      show: {
        type: Boolean,
        default: false
      }
    },
    setup(props, { slots }) {
      return () => props.show ? h('aside', { class: 'n-drawer' }, slots.default?.()) : null
    }
  }),
  NDrawerContent: defineComponent({
    props: {
      title: {
        type: String,
        default: ''
      }
    },
    setup(props, { slots }) {
      return () => h('section', { class: 'n-drawer-content' }, [
        h('h2', { class: 'n-drawer-content__title' }, props.title),
        slots.default?.()
      ])
    }
  }),
  NForm: passthrough('form', 'n-form'),
  NFormItem: passthrough('div', 'n-form-item'),
  NIcon: passthrough('span', 'n-icon'),
  NInput: passthrough('input', 'n-input'),
  NInputNumber: passthrough('input', 'n-input-number'),
  NSelect: passthrough('div', 'n-select'),
  NSwitch: passthrough('button', 'n-switch'),
  useMessage: () => ({ warning: vi.fn(), error: vi.fn(), success: vi.fn() })
}))

vi.mock('@/composables/useLlmMarkdownConfig', () => ({
  useLlmMarkdownConfig: () => ({
    form: ref({
      id: '',
      name: '',
      apiType: 'openai',
      url: '',
      model: '',
      credentialEnvVar: '',
      credentialConfigured: false,
      usageType: 'MARKDOWN_POST_PROCESSING',
      priority: 100,
      maxContextTokens: 16000,
      maxConcurrency: 1,
      requestIntervalMillis: 1000,
      defaultConfig: true,
      enabled: true,
      healthy: false,
      healthMessage: '',
      lastHealthAt: ''
    }),
    rows: ref([
      {
        id: 'llm-config-1',
        name: '默认 LLM 配置',
        apiType: 'openai',
        url: 'https://api.example.com/v1',
        model: 'gpt-4o-mini',
        credentialEnvVar: 'MODEL_API_KEY',
        credentialConfigured: true,
        usageType: 'MARKDOWN_POST_PROCESSING',
        priority: 100,
        maxContextTokens: 16000,
        maxConcurrency: 1,
        requestIntervalMillis: 1000,
        defaultConfig: true,
        enabled: true,
        healthy: true,
        healthMessage: '',
        lastHealthAt: '2026-06-19T10:00:00+08:00',
        statusLabel: '可用'
      }
    ]),
    isDrawerVisible: drawerVisible,
    isLoading: ref(false),
    isSaving: ref(false),
    isTesting: ref(false),
    actingId: ref(''),
    lastLoadedAt: ref('2026-06-19T10:00:00+08:00'),
    errorMessage: ref(''),
    editingTitle,
    canSubmit: ref(true),
    hasConfigs: ref(true),
    emptyStatus: ref('暂无 LLM 配置，OCR 可正常运行。'),
    capabilityHints: ref({ urlPlaceholder: '请输入完整接口地址', canTest: true }),
    loadConfig,
    openCreateDrawer: () => {
      drawerVisible.value = true
      editingTitle.value = '新增配置'
      openCreateDrawer()
    },
    closeDrawer: () => {
      drawerVisible.value = false
      closeDrawer()
    },
    resetForm,
    editConfig,
    saveConfig,
    toggleEnabled,
    makeDefault,
    removeConfig,
    testConfig
  })
}))

describe('LlmMarkdownConfigPanel drawer editing', () => {
  beforeEach(() => {
    drawerVisible.value = false
    editingTitle.value = '新增配置'
    openCreateDrawer.mockClear()
    closeDrawer.mockClear()
    editConfig.mockClear()
    resetForm.mockClear()
    loadConfig.mockClear()
    saveConfig.mockClear()
    toggleEnabled.mockClear()
    makeDefault.mockClear()
    removeConfig.mockClear()
    testConfig.mockClear()
  })

  it('opens the drawer from新增配置 instead of keeping the inline editor beside the table', async () => {
    const wrapper = mount(LlmMarkdownConfigPanel)

    await wrapper.findAll('button').find((button) => button.text() === '新增配置')?.trigger('click')

    expect(openCreateDrawer).toHaveBeenCalledTimes(1)
    expect(wrapper.find('.n-drawer').exists()).toBe(true)
    expect(wrapper.find('.llm-config-drawer__form').exists()).toBe(true)
    expect(wrapper.find('.llm-config-panel__editor').exists()).toBe(false)
  })

  it('opens the drawer from row edit and updates the drawer title', async () => {
    const wrapper = mount(LlmMarkdownConfigPanel)

    await wrapper.findAll('button').find((button) => button.text() === '编辑')?.trigger('click')

    expect(editConfig).toHaveBeenCalledTimes(1)
    expect(wrapper.find('.n-drawer').exists()).toBe(true)
    expect(wrapper.text()).toContain('默认 LLM 配置')
  })

  /**
   * API Key 字段应说明保存的是环境变量名，不是真实 Key。
   *
   * @author lvdaxianerplus
   * @date 2026-06-21
   */
  it('explains that api key credential field stores an environment variable name', async () => {
    const wrapper = mount(LlmMarkdownConfigPanel)

    await wrapper.findAll('button').find((button) => button.text() === '新增配置')?.trigger('click')

    expect(wrapper.text()).toContain('这里只填写服务进程可读取的环境变量名')
    expect(wrapper.text()).toContain('真实 Key 需在启动服务前写入该环境变量')
  })
})

function passthrough(tag: string, className: string) {
  return defineComponent({
    setup(_, { slots, attrs }) {
      return () => h(tag, { ...attrs, class: className }, slots.default?.())
    }
  })
}
