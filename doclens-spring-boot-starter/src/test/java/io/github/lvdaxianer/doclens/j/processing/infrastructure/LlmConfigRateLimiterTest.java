package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownApiType;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmUsageType;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * LLM 配置限流器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
class LlmConfigRateLimiterTest {

    private static final int SINGLE_CONCURRENCY = 1;
    private static final int DOUBLE_CONCURRENCY = 2;
    private static final int NO_INTERVAL_MILLIS = 0;
    private static final int ONE_SECOND_INTERVAL_MILLIS = 1000;
    private static final int INTERVAL_ASSERTION_MILLIS = 900;
    private static final int PARALLEL_ASSERTION_MILLIS = 250;
    private static final int FUTURE_TIMEOUT_SECONDS = 1;

    /**
     * 同一配置应限制并发请求数量。
     *
     * @throws Exception 等待异步结果失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Test
    void limitsConcurrentRequestsPerConfig() throws Exception {
        LlmConfigRateLimiter limiter = new LlmConfigRateLimiter();
        LlmMarkdownConfig config = config("llm-a", SINGLE_CONCURRENCY, NO_INTERVAL_MILLIS);
        ExecutorService executor = singleThreadExecutor();

        try (LlmConfigRateLimiter.Permit first = limiter.acquire(config)) {
            Future<Boolean> blocked = executor.submit(() -> {
                try (LlmConfigRateLimiter.Permit ignored = limiter.acquire(config)) {
                    return true;
                }
            });

            assertThat(blocked.isDone()).isFalse();
            first.close();
            assertThat(blocked.get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 同一配置相邻请求启动时间应遵守最小间隔。
     *
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Test
    void waitsBetweenRequestStartsForSameConfig() {
        LlmConfigRateLimiter limiter = new LlmConfigRateLimiter();
        LlmMarkdownConfig config = config("llm-a", SINGLE_CONCURRENCY, ONE_SECOND_INTERVAL_MILLIS);

        Instant start = Instant.now();
        try (LlmConfigRateLimiter.Permit ignored = limiter.acquire(config)) {
            // 首次请求立即获得许可。
        }
        try (LlmConfigRateLimiter.Permit ignored = limiter.acquire(config)) {
            assertThat(Duration.between(start, Instant.now()))
                    .isGreaterThanOrEqualTo(Duration.ofMillis(INTERVAL_ASSERTION_MILLIS));
        }
    }

    /**
     * 请求间隔应按并发槽位生效，不应串行化所有可用槽位。
     *
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Test
    void requestIntervalDoesNotSerializeAvailableConcurrencySlots() {
        LlmConfigRateLimiter limiter = new LlmConfigRateLimiter();
        LlmMarkdownConfig config = config("llm-a", DOUBLE_CONCURRENCY, ONE_SECOND_INTERVAL_MILLIS);

        Instant start = Instant.now();
        try (LlmConfigRateLimiter.Permit first = limiter.acquire(config);
                LlmConfigRateLimiter.Permit second = limiter.acquire(config)) {
            assertThat(Duration.between(start, Instant.now()))
                    .isLessThan(Duration.ofMillis(PARALLEL_ASSERTION_MILLIS));
        }
    }

    /**
     * 同一配置 ID 的并发配置变化后，限流器应使用新的并发值。
     *
     * @throws Exception 等待异步结果失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Test
    void refreshesConcurrencyWhenConfigLimitChangesForSameId() throws Exception {
        LlmConfigRateLimiter limiter = new LlmConfigRateLimiter();
        LlmMarkdownConfig singleSlot = config("llm-a", SINGLE_CONCURRENCY, NO_INTERVAL_MILLIS);
        LlmMarkdownConfig doubleSlot = config("llm-a", DOUBLE_CONCURRENCY, NO_INTERVAL_MILLIS);
        ExecutorService executor = singleThreadExecutor();
        CountDownLatch acquiredLatch = new CountDownLatch(1);
        CountDownLatch releaseLatch = new CountDownLatch(1);

        try (LlmConfigRateLimiter.Permit first = limiter.acquire(singleSlot)) {
            Future<Boolean> acquired = executor.submit(() -> {
                try (LlmConfigRateLimiter.Permit ignored = limiter.acquire(doubleSlot)) {
                    acquiredLatch.countDown();
                    releaseLatch.await(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    return true;
                }
            });

            assertThat(acquiredLatch.await(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
            assertThat(limiter.availablePermitsForTesting(doubleSlot)).isZero();
            releaseLatch.countDown();
            assertThat(acquired.get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 等待间隔时被中断应释放已获取的并发许可。
     *
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Test
    void releasesPermitWhenInterruptedDuringIntervalWait() {
        LlmConfigRateLimiter limiter = new LlmConfigRateLimiter();
        LlmMarkdownConfig config = config("llm-a", SINGLE_CONCURRENCY, ONE_SECOND_INTERVAL_MILLIS);
        try (LlmConfigRateLimiter.Permit ignored = limiter.acquire(config)) {
            // 首次请求预定下一次启动时间。
        }

        Thread.currentThread().interrupt();
        assertThatThrownBy(() -> limiter.acquire(config))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("llm config rate limiter interrupted");
        assertThat(Thread.interrupted()).isTrue();

        assertThat(limiter.availablePermitsForTesting(config)).isEqualTo(SINGLE_CONCURRENCY);
    }

    /**
     * 创建测试用 LLM 配置。
     *
     * @param id 配置 ID
     * @param maxConcurrency 最大并发数
     * @param requestIntervalMillis 请求启动最小间隔
     * @return LLM 配置
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private LlmMarkdownConfig config(String id, int maxConcurrency, int requestIntervalMillis) {
        return LlmMarkdownConfig.builder(id, "配置-" + id, LlmMarkdownApiType.OPENAI)
                .endpoint("https://llm.example.com/v1/chat/completions", "markdown-model")
                .credential("MINIMAX_API_KEY")
                .usage(LlmUsageType.MARKDOWN_POST_PROCESSING, 10)
                .runtimeLimits(LlmMarkdownConfig.DEFAULT_MAX_CONTEXT_TOKENS, maxConcurrency,
                        requestIntervalMillis)
                .defaultConfig(true)
                .enabled(true)
                .healthy(true)
                .build();
    }

    /**
     * 创建测试专用单线程池。
     *
     * @return 单线程池
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private ExecutorService singleThreadExecutor() {
        return Executors.newSingleThreadExecutor(new NamedThreadFactory());
    }

    /**
     * 测试线程工厂。
     *
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private static final class NamedThreadFactory implements ThreadFactory {

        private final AtomicInteger counter = new AtomicInteger();

        /**
         * 创建测试线程。
         *
         * @param runnable 线程任务
         * @return 测试线程
         * @author lvdaxianerplus
         * @date 2026-06-13
         */
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName("llm-rate-limiter-test-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
