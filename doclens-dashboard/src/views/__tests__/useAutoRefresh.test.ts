import { afterEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, nextTick } from 'vue'
import { mount } from '@vue/test-utils'

import { useAutoRefresh } from '@/composables/useAutoRefresh'

/**
 * 自动刷新组合式函数测试。
 *
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
describe('useAutoRefresh', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('exposes stale state after refresh failure and clears it after manual success', async () => {
    vi.useFakeTimers()
    const refresh = vi.fn()
      .mockRejectedValueOnce(new Error('网关连接失败'))
      .mockResolvedValueOnce(undefined)
    const wrapper = mount(host(refresh))

    await flushAsyncActions()

    expect(wrapper.text()).toContain('自动刷新失败：网关连接失败')
    await (wrapper.vm as unknown as { refreshNow: () => Promise<void> }).refreshNow()
    await nextTick()
    expect(wrapper.text()).toContain('自动刷新正常')
  })
})

/**
 * 创建自动刷新测试宿主组件。
 *
 * @param refresh - 刷新函数
 * @returns 测试宿主组件
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
function host(refresh: () => Promise<void>) {
  return defineComponent({
    name: 'AutoRefreshHost',
    setup() {
      const autoRefresh = useAutoRefresh(refresh, { intervalMs: 1000 })
      return {
        refreshNow: autoRefresh.refreshNow,
        render: () => h('span', autoRefresh.isStale.value
          ? `自动刷新失败：${autoRefresh.lastErrorMessage.value}`
          : '自动刷新正常')
      }
    },
    render() {
      return this.render()
    }
  })
}

/**
 * 等待异步刷新任务完成。
 *
 * @returns 等待完成信号
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
async function flushAsyncActions(): Promise<void> {
  await Promise.resolve()
  await Promise.resolve()
  await nextTick()
}
