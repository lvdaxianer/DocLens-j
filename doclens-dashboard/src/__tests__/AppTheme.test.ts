import { defineComponent, h } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'

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
})
