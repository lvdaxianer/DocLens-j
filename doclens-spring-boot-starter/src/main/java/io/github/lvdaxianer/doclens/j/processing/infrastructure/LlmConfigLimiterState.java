package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import java.util.Arrays;

/**
 * 单个 LLM 配置的限流状态。
 *
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
final class LlmConfigLimiterState {

    private int maxConcurrency;
    private int intervalMillis;
    private boolean[] availableSlots;
    private long[] lastStartMillis;

    /**
     * 创建限流状态。
     *
     * @param maxConcurrency 最大并发
     * @param intervalMillis 请求间隔
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    LlmConfigLimiterState(int maxConcurrency, int intervalMillis) {
        this.maxConcurrency = maxConcurrency;
        this.intervalMillis = intervalMillis;
        this.availableSlots = new boolean[maxConcurrency];
        this.lastStartMillis = new long[maxConcurrency];
        Arrays.fill(this.availableSlots, true);
    }

    /**
     * 按最新配置刷新槽位数量与请求间隔。
     *
     * @param maxConcurrency 最大并发
     * @param intervalMillis 请求间隔
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    synchronized void refresh(int maxConcurrency, int intervalMillis) {
        if (this.maxConcurrency != maxConcurrency) {
            resizeSlots(maxConcurrency);
        } else {
            // 并发未变化时保留当前槽位占用状态。
        }
        this.intervalMillis = intervalMillis;
    }

    /**
     * 获取一个可用并发槽位。
     *
     * @return 槽位编号
     * @throws InterruptedException 等待槽位被中断时抛出
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    synchronized int acquireSlot() throws InterruptedException {
        int slot = firstAvailableSlot();
        while (slot == LlmConfigRateLimiter.UNASSIGNED_SLOT) {
            wait();
            slot = firstAvailableSlot();
        }
        availableSlots[slot] = false;
        return slot;
    }

    /**
     * 释放并发槽位。
     *
     * @param slot 槽位编号
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    synchronized void releaseSlot(int slot) {
        if (slot < maxConcurrency) {
            // 槽位仍在当前配置范围内时归还给等待队列。
            availableSlots[slot] = true;
            notifyAll();
        } else {
            // 配置缩容后旧槽位不再归还，避免超过新的并发上限。
        }
    }

    /**
     * 读取可用槽位数量。
     *
     * @return 可用槽位数量
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    synchronized int availableSlots() {
        int count = 0;
        for (boolean available : availableSlots) {
            if (available) {
                count++;
            } else {
                // 已占用槽位不计入可用数量。
            }
        }
        return count;
    }

    /**
     * 读取槽位最近启动时间。
     *
     * @param slot 槽位编号
     * @return 最近启动时间戳
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    synchronized long lastStartMillis(int slot) {
        return lastStartMillis[slot];
    }

    /**
     * 预定槽位启动时间。
     *
     * @param slot 槽位编号
     * @param expected 预期旧值
     * @param updated 新启动时间
     * @return 是否预定成功
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    synchronized boolean reserveSlotStart(int slot, long expected, long updated) {
        if (lastStartMillis[slot] == expected) {
            lastStartMillis[slot] = updated;
            return true;
        } else {
            return false;
        }
    }

    /**
     * 读取请求间隔。
     *
     * @return 请求间隔毫秒数
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    synchronized int intervalMillis() {
        return intervalMillis;
    }

    /**
     * 调整槽位数组大小。
     *
     * @param newMaxConcurrency 新最大并发
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private void resizeSlots(int newMaxConcurrency) {
        int previousMax = this.maxConcurrency;
        availableSlots = Arrays.copyOf(availableSlots, newMaxConcurrency);
        lastStartMillis = Arrays.copyOf(lastStartMillis, newMaxConcurrency);
        this.maxConcurrency = newMaxConcurrency;
        fillNewSlots(previousMax, newMaxConcurrency);
    }

    /**
     * 将扩容新增槽位标记为可用。
     *
     * @param previousMax 旧最大并发
     * @param newMaxConcurrency 新最大并发
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private void fillNewSlots(int previousMax, int newMaxConcurrency) {
        if (newMaxConcurrency > previousMax) {
            Arrays.fill(availableSlots, previousMax, newMaxConcurrency, true);
            notifyAll();
        } else {
            // 缩容时不新增槽位，无需唤醒等待线程。
        }
    }

    /**
     * 查找第一个可用槽位。
     *
     * @return 槽位编号，不存在时返回未分配
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private int firstAvailableSlot() {
        for (int slot = 0; slot < maxConcurrency; slot++) {
            if (availableSlots[slot]) {
                return slot;
            } else {
                // 当前槽位已占用，继续查找后续槽位。
            }
        }
        return LlmConfigRateLimiter.UNASSIGNED_SLOT;
    }
}
