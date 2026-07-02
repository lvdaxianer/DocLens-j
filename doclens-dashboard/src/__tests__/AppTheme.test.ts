import { defineComponent, h } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { DASHBOARD_THEME } from '@/theme/dashboardTheme'

vi.mock('vue-router', () => ({
  RouterLink: defineComponent({
    name: 'RouterLinkStub',
    setup(_, { slots }) {
      return () => h('a', slots.default?.())
    }
  }),
  RouterView: defineComponent({
    name: 'RouterViewStub',
    setup() {
      return () => h('div')
    }
  }),
  useRoute: () => ({ meta: { title: '运行总览' } })
}))

vi.mock('naive-ui', () => ({
  NConfigProvider: defineComponent({
    name: 'NConfigProviderStub',
    props: {
      themeOverrides: {
        type: Object,
        required: true
      }
    },
    setup(props, { slots }) {
      return () => h('div', { 'data-theme': JSON.stringify(props.themeOverrides) }, slots.default?.())
    }
  }),
  NMessageProvider: defineComponent({
    name: 'NMessageProviderStub',
    setup(_, { slots }) {
      return () => h('div', slots.default?.())
    }
  }),
  NIcon: defineComponent({
    name: 'NIconStub',
    setup() {
      return () => h('i')
    }
  })
}))

import App from '@/App.vue'

describe('App theme wiring', () => {
  // 每个用例使用独立会话上下文，避免分区状态在测试间泄漏。
  beforeEach(() => {
    sessionStorage.clear()
  })

  it('feeds the orange palette into Naive UI', () => {
    const wrapper = mount(App)
    const themeNode = wrapper.get('[data-theme]')
    const themeOverrides = JSON.parse(themeNode.attributes('data-theme') as string) as {
      common: Record<string, string>
    }

    expect(themeOverrides.common.primaryColor).toBe(DASHBOARD_THEME.primaryColor)
    expect(themeOverrides.common.primaryColorHover).toBe(DASHBOARD_THEME.primaryColorHover)
    expect(themeOverrides.common.primaryColorPressed).toBe(DASHBOARD_THEME.primaryColorPressed)
    expect(themeOverrides.common.primaryColorSuppl).toBe(DASHBOARD_THEME.primaryColorSuppl)
  })

  it('shows current partition and trusted principal status', () => {
    /*
     * 顶栏状态测试覆盖 F1 的用户可见结果：
     * - 分区值来自当前 sessionStorage。
     * - principal 缺省时展示可信网关注入语义。
     * - App 只验证组件被挂载到应用壳。
     * - 具体读写规则由组件内部封装。
     */
    sessionStorage.setItem('X-Doclens-Key', 'tenant-alpha')

    const wrapper = mount(App)

    expect(wrapper.text()).toContain('分区：tenant-alpha')
    expect(wrapper.text()).toContain('Principal：可信网关注入')
  })
})
