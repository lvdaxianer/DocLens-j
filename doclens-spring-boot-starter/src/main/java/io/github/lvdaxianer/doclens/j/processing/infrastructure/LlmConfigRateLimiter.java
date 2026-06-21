package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 按 LLM 配置隔离的请求限流器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
public final class LlmConfigRateLimiter {

    private static final int MIN_CONCURRENCY = 1;
    private static final int MIN_INTERVAL_MILLIS = 0;
    static final int UNASSIGNED_SLOT = -1;

    private final ConcurrentMap<String, LlmConfigLimiterState> states = new ConcurrentHashMap<>();

    /**
     * 获取指定 LLM 配置的调用许可。
     *
     * @param config LLM 配置
     * @return 调用许可
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    public Permit acquire(LlmMarkdownConfig config) {
        LlmConfigLimiterState state = state(config);
        int slot = UNASSIGNED_SLOT;
        try {
            slot = state.acquireSlot();
            awaitRequestInterval(state, slot);
            return new Permit(state, slot);
        } catch (InterruptedException ex) {
            // 中断发生在获取许可之后时，必须释放许可避免同配置永久少一个并发槽位。
            if (slot != UNASSIGNED_SLOT) {
                state.releaseSlot(slot);
            } else {
                // 未获取许可时无需释放。
            }
            Thread.currentThread().interrupt();
            throw new IllegalStateException("llm config rate limiter interrupted", ex);
        }
    }

    /**
     * 获取或创建配置对应的限流状态。
     *
     * @param config LLM 配置
     * @return 限流状态
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private LlmConfigLimiterState state(LlmMarkdownConfig config) {
        int maxConcurrency = Math.max(MIN_CONCURRENCY, config.maxConcurrency());
        int intervalMillis = Math.max(MIN_INTERVAL_MILLIS, config.requestIntervalMillis());
        LlmConfigLimiterState state = states.computeIfAbsent(config.id(),
                ignored -> new LlmConfigLimiterState(maxConcurrency, intervalMillis));
        state.refresh(maxConcurrency, intervalMillis);
        return state;
    }

    /**
     * 读取测试场景下的剩余许可数量。
     *
     * @param config LLM 配置
     * @return 剩余许可数量
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    int availablePermitsForTesting(LlmMarkdownConfig config) {
        return state(config).availableSlots();
    }

    /**
     * 等待同一配置的请求启动间隔。
     *
     * @param state 限流状态
     * @param slot 并发槽位
     * @throws InterruptedException 等待被中断时抛出
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private void awaitRequestInterval(LlmConfigLimiterState state, int slot) throws InterruptedException {
        long waitMillis = reserveStartMillis(state, slot);
        // 预定启动时间后在锁外等待，避免阻塞其它配置的限流计算。
        if (waitMillis > MIN_INTERVAL_MILLIS) {
            TimeUnit.MILLISECONDS.sleep(waitMillis);
        } else {
            // 无需等待时立即发起请求。
        }
    }

    /**
     * 预定本次请求启动时间。
     *
     * @param state 限流状态
     * @param slot 并发槽位
     * @return 需要等待的毫秒数
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private long reserveStartMillis(LlmConfigLimiterState state, int slot) {
        while (true) {
            long previousStart = state.lastStartMillis(slot);
            long now = System.currentTimeMillis();
            long reservedStart = Math.max(now, previousStart + state.intervalMillis());
            if (state.reserveSlotStart(slot, previousStart, reservedStart)) {
                // 只返回当前线程需要睡眠的时间，实际睡眠在 CAS 之外完成。
                return reservedStart - now;
            } else {
                // 竞争失败说明其它线程已预定启动时间，继续重算。
            }
        }
    }

    /**
     * LLM 配置调用许可。
     *
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    public static final class Permit implements AutoCloseable {

        private final LlmConfigLimiterState state;
        private final int slot;
        private final AtomicBoolean closed = new AtomicBoolean();

        /**
         * 创建调用许可。
         *
         * @param state 限流状态
         * @param slot 并发槽位
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        private Permit(LlmConfigLimiterState state, int slot) {
            this.state = state;
            this.slot = slot;
        }

        /**
         * 释放调用许可。
         *
         * @author lvdaxianerplus
         * @date 2026-06-13
         */
        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                // 首次关闭时释放槽位，重复关闭不重复释放。
                state.releaseSlot(slot);
            } else {
                // 已释放的许可忽略重复关闭。
            }
        }
    }
}
