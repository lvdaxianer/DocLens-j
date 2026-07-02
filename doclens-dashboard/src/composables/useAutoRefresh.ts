import { onMounted, onUnmounted, shallowRef, type ShallowRef } from 'vue'

/**
 * Dashboard 自动刷新组合式模块。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
interface UseAutoRefreshOptions {
  intervalMs?: number
  immediate?: boolean
  failureMessage?: () => string
}

export interface AutoRefreshState {
  isStale: ShallowRef<boolean>
  lastErrorMessage: ShallowRef<string>
  refreshNow: () => Promise<void>
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
): AutoRefreshState {
  const intervalMs = options.intervalMs ?? DEFAULT_REFRESH_INTERVAL_MS
  const immediate = options.immediate ?? true
  const isStale = shallowRef(false)
  const lastErrorMessage = shallowRef('')
  let timerId: number | undefined = undefined
  let isRefreshing = false

  /**
   * 读取调用方提供的页面错误。
   *
   * @returns 页面错误文案
   * @author lvdaxianer@yeah.net
   * @date 2026-07-02
   */
  function configuredFailureMessage(): string {
    return options.failureMessage?.().trim() ?? ''
  }

  /**
   * 读取刷新失败文案。
   *
   * @param error - 刷新异常
   * @returns 刷新失败文案
   * @author lvdaxianer@yeah.net
   * @date 2026-07-02
   */
  function refreshErrorMessage(error?: unknown): string {
    const failureMessage = configuredFailureMessage()
    if (failureMessage) {
      // 调用方提供页面级错误时优先展示用户可理解文案。
      return failureMessage
    } else if (error instanceof Error) {
      // 异常对象保留原始 message，方便用户反馈截图定位。
      return error.message
    } else {
      // 未知失败保持稳定兜底文案。
      return '自动刷新失败'
    }
  }

  /**
   * 标记自动刷新恢复正常。
   *
   * @returns 标记完成信号
   * @author lvdaxianer@yeah.net
   * @date 2026-07-02
   */
  function markFresh(): void {
    isStale.value = false
    lastErrorMessage.value = ''
  }

  /**
   * 标记自动刷新进入失效提示态。
   *
   * @param error - 刷新异常
   * @returns 标记完成信号
   * @author lvdaxianer@yeah.net
   * @date 2026-07-02
   */
  function markStale(error?: unknown, message?: string): void {
    isStale.value = true
    lastErrorMessage.value = message ?? refreshErrorMessage(error)
  }

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
        const failureMessage = configuredFailureMessage()
        if (failureMessage) {
          // 刷新函数吞掉异常但留下页面错误时，提示当前数据可能已失效。
          markStale(undefined, failureMessage)
        } else {
          // 无异常且调用方没有失败文案，说明本次刷新成功。
          markFresh()
        }
      } catch (error) {
        markStale(error)
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

  return {
    isStale,
    lastErrorMessage,
    refreshNow: runRefresh
  }
}
