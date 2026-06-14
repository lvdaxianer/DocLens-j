import type { GlobalThemeOverrides } from 'naive-ui'

export const DASHBOARD_THEME = {
  primaryColor: '#f97316',
  primaryColorHover: '#fb923c',
  primaryColorPressed: '#ea580c',
  primaryColorSuppl: '#fff1e6',
  activeColor: '#f97316',
  activeStrongColor: '#c2410c',
  activeMutedColor: '#fff0e5',
  focusRingColor: 'rgba(249, 115, 22, 0.18)',
  chartAccentColors: ['#f97316', '#f59e0b', '#c75146']
} as const

/**
 * 创建仪表盘的 Naive UI 全局主题覆盖配置。
 *
 * @returns 主题覆盖配置
 * @author lvdaxianerplus
 * @date 2026-06-14
 */
export function createDashboardThemeOverrides(): GlobalThemeOverrides {
  return {
    common: {
      primaryColor: DASHBOARD_THEME.primaryColor,
      primaryColorHover: DASHBOARD_THEME.primaryColorHover,
      primaryColorPressed: DASHBOARD_THEME.primaryColorPressed,
      primaryColorSuppl: DASHBOARD_THEME.primaryColorSuppl,
      borderRadius: '8px',
      fontFamily: 'Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
      fontSize: '13px',
      fontSizeSmall: '12px',
      fontSizeMedium: '13px',
      fontSizeLarge: '14px',
      heightSmall: '26px',
      heightMedium: '30px',
      heightLarge: '34px'
    }
  }
}
