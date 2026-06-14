import { describe, expect, it } from 'vitest'

import { DASHBOARD_THEME, createDashboardThemeOverrides } from '@/theme/dashboardTheme'

describe('dashboard theme palette', () => {
  it('uses the orange brand palette for primary accents', () => {
    expect(DASHBOARD_THEME).toMatchObject({
      primaryColor: '#f97316',
      primaryColorHover: '#fb923c',
      primaryColorPressed: '#ea580c',
      primaryColorSuppl: '#fff1e6',
      activeColor: '#f97316',
      activeStrongColor: '#c2410c',
      activeMutedColor: '#fff0e5',
      chartAccentColors: ['#f97316', '#f59e0b', '#c75146']
    })
  })

  it('maps the orange palette into Naive UI theme overrides', () => {
    const overrides = createDashboardThemeOverrides()

    expect(overrides.common).toMatchObject({
      primaryColor: '#f97316',
      primaryColorHover: '#fb923c',
      primaryColorPressed: '#ea580c',
      primaryColorSuppl: '#fff1e6'
    })
  })
})
