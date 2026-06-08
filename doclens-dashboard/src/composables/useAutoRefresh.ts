import { onMounted, onUnmounted } from 'vue'

/**
 * Dashboard 自动刷新组合式模块。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
interface UseAutoRefreshOptions {
  intervalMs?: number
  immediate?: boolean
}

export const DEFAULT_REFRESH_INTERVAL_MS = 5000
const MILLIS_PER_SECOND = 1000
export const DEFAULT_REFRESH_INTERVAL_SECONDS = DEFAULT_REFRESH_INTERVAL_MS / MILLIS_PER_SECOND

/**
 * 按固定间隔局部刷新页面数据，并避免请求重叠。
 *
 * @param refresh - 页面数据刷新函数
 * @param options - 刷新间隔和首次加载配置
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
export function useAutoRefresh(
  refresh: () => Promise<void> | void,
  options: UseAutoRefreshOptions = {}
): void {
  const intervalMs = options.intervalMs ?? DEFAULT_REFRESH_INTERVAL_MS
  const immediate = options.immediate ?? true
  let timerId: number | undefined = undefined
  let isRefreshing = false

  /**
   * 执行单次刷新，已有请求时跳过本次轮询。
   *
   * @returns 刷新完成信号
   * @author lvdaxianerplus
   * @date 2026-06-08
   */
  async function runRefresh(): Promise<void> {
    if (isRefreshing) {
      // 已有请求在进行中，跳过本次轮询。
    } else {
      isRefreshing = true
      try {
        await refresh()
      } finally {
        isRefreshing = false
      }
    }
  }

  onMounted(() => {
    if (immediate) {
      void runRefresh()
    } else {
      // 调用方选择自行触发首次刷新。
    }
    timerId = window.setInterval(() => {
      void runRefresh()
    }, intervalMs)
  })

  onUnmounted(() => {
    if (timerId !== undefined) {
      window.clearInterval(timerId)
    } else {
      // 未创建定时器时无需清理。
    }
  })
}
