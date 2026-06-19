package io.github.lvdaxianer.doclens.j.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.Test;

/**
 * 页任务 worker 自动配置测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-19
 */
class DocLensPageTaskWorkerAutoConfigurationTest {

    /**
     * 默认页任务锁应覆盖 OCR 请求超时窗口。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Test
    void effectiveSettingsKeepPageTaskLockLongerThanOcrTimeout() {
        DocLensSpringProperties properties = defaultPropertiesWithPaddleTimeout(600);

        DocLensPageTaskWorkerAutoConfiguration.PageTaskWorkerRuntimeSettings settings =
                new DocLensPageTaskWorkerAutoConfiguration().pageTaskWorkerRuntimeSettings(properties);

        assertThat(settings.lockSeconds()).isGreaterThanOrEqualTo(630);
    }

    /**
     * 默认页任务并发应跟随启用节点的最大并发。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Test
    void effectiveSettingsUseBootstrapNodeConcurrencyWhenNotExplicitlyConfigured() {
        DocLensSpringProperties properties = defaultPropertiesWithPaddleTimeout(600);

        DocLensPageTaskWorkerAutoConfiguration.PageTaskWorkerRuntimeSettings settings =
                new DocLensPageTaskWorkerAutoConfiguration().pageTaskWorkerRuntimeSettings(properties);

        assertThat(settings.batchSize()).isEqualTo(10);
        assertThat(settings.poolSize()).isEqualTo(10);
    }

    /**
     * 页任务执行线程池应使用生效后的 pool size。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Test
    void pageTaskExecutorUsesEffectivePoolSize() {
        DocLensPageTaskWorkerAutoConfiguration configuration = new DocLensPageTaskWorkerAutoConfiguration();
        DocLensPageTaskWorkerAutoConfiguration.PageTaskWorkerRuntimeSettings settings =
                new DocLensPageTaskWorkerAutoConfiguration.PageTaskWorkerRuntimeSettings(10, 630, 10, 500, 64, 250);

        ThreadPoolExecutor executor = (ThreadPoolExecutor) configuration.doclensPageTaskExecutor(settings);

        try {
            assertThat(executor.getCorePoolSize()).isEqualTo(10);
            assertThat(executor.getMaximumPoolSize()).isEqualTo(10);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 创建指定 PaddleOCR 超时的默认配置。
     *
     * @param timeoutSeconds PaddleOCR 请求超时秒数
     * @return DocLens Spring 配置
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private DocLensSpringProperties defaultPropertiesWithPaddleTimeout(int timeoutSeconds) {
        return new DocLensSpringProperties(null, true, "local-worker", null, null, null, null,
                new DocLensSpringProperties.PaddleOcrProperties(true, "http://127.0.0.1:8080/ocr",
                        timeoutSeconds, false, java.util.List.of(DocLensSpringProperties.defaultPaddleNode())),
                null, null, null, null, null, null, null, null);
    }
}
