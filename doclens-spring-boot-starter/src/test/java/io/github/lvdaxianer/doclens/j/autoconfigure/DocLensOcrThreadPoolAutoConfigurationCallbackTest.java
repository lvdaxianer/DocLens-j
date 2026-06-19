package io.github.lvdaxianer.doclens.j.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.Test;

/**
 * 回调投递线程池自动配置测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-16
 */
class DocLensOcrThreadPoolAutoConfigurationCallbackTest {

    private static final int EXPECTED_CALLBACK_CORE_SIZE = 3;

    /**
     * 回调投递线程池应使用 3 个核心线程并支持延迟调度。
     *
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    @Test
    void callbackExecutorUsesThreeCoreScheduledPool() {
        ExecutorService executor = new DocLensOcrThreadPoolAutoConfiguration()
                .doclensCallbackExecutor(defaultProperties());

        try {
            assertThat(executor).isInstanceOf(ScheduledThreadPoolExecutor.class);
            assertThat(((ScheduledThreadPoolExecutor) executor).getCorePoolSize())
                    .isEqualTo(EXPECTED_CALLBACK_CORE_SIZE);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 默认 OCR 请求线程池应按启用节点最大并发扩容。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Test
    void ocrRequestExecutorDefaultsToBootstrapNodeConcurrency() {
        ExecutorService executor = new DocLensOcrThreadPoolAutoConfiguration()
                .doclensOcrRequestExecutor(defaultProperties());

        try {
            ThreadPoolExecutor threadPool = (ThreadPoolExecutor) executor;
            assertThat(threadPool.getCorePoolSize()).isEqualTo(10);
            assertThat(threadPool.getMaximumPoolSize()).isEqualTo(10);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 创建默认 DocLens Spring 配置。
     *
     * @return 默认 DocLens Spring 配置
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    private DocLensSpringProperties defaultProperties() {
        return new DocLensSpringProperties(null, true, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }
}
