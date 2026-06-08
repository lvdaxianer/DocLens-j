package io.github.lvdaxianer.doclens.j.autoconfigure;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrRuntimeNodePool;
import io.github.lvdaxianer.doclens.j.query.application.DashboardOcrMetricsProvider;
import io.github.lvdaxianer.doclens.j.query.application.EmptyDashboardOcrMetricsProvider;
import io.github.lvdaxianer.doclens.j.query.infrastructure.OcrDashboardMetricsProvider;
import io.github.lvdaxianer.doclens.j.query.infrastructure.OcrDashboardMetricsProvider.DashboardThreadPools;
import java.util.concurrent.ExecutorService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Dashboard OCR 指标自动配置。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
@AutoConfiguration(after = {
        DocLensAutoConfiguration.class,
        DocLensOcrThreadPoolAutoConfiguration.class,
        DocLensOcrResourceAutoConfiguration.class
})
public class DocLensDashboardMetricsAutoConfiguration {

    /**
     * 创建 Dashboard OCR 指标提供器。
     *
     * @param nodeRepository OCR 节点仓储
     * @param callRepository OCR 调用记录仓储
     * @param nodePool OCR 运行时节点池
     * @param threadPools Dashboard 线程池集合
     * @return Dashboard OCR 指标提供器
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Bean
    @ConditionalOnBean({OcrNodeRepository.class, OcrNodeCallRepository.class, OcrRuntimeNodePool.class})
    @ConditionalOnMissingBean
    DashboardOcrMetricsProvider dashboardOcrMetricsProvider(
            OcrNodeRepository nodeRepository,
            OcrNodeCallRepository callRepository,
            OcrRuntimeNodePool nodePool,
            DashboardThreadPools threadPools
    ) {
        return new OcrDashboardMetricsProvider(nodeRepository, callRepository, nodePool, threadPools);
    }

    /**
     * 创建空 Dashboard OCR 指标提供器。
     *
     * @return 空 Dashboard OCR 指标提供器
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Bean
    @ConditionalOnMissingBean
    DashboardOcrMetricsProvider emptyDashboardOcrMetricsProvider() {
        return new EmptyDashboardOcrMetricsProvider();
    }

    /**
     * 创建 Dashboard 线程池集合。
     *
     * @param documentProcessingExecutor 文档处理线程池
     * @param ocrRequestExecutor OCR 请求线程池
     * @param ocrHealthExecutor OCR 健康检查线程池
     * @param callbackExecutor 回调线程池
     * @return Dashboard 线程池集合
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Bean
    @ConditionalOnMissingBean
    DashboardThreadPools dashboardThreadPools(
            @Qualifier("doclensDocumentProcessingExecutor") ExecutorService documentProcessingExecutor,
            @Qualifier("doclensOcrRequestExecutor") ExecutorService ocrRequestExecutor,
            @Qualifier("doclensOcrHealthExecutor") ExecutorService ocrHealthExecutor,
            @Qualifier("doclensCallbackExecutor") ExecutorService callbackExecutor
    ) {
        return new DashboardThreadPools(documentProcessingExecutor, ocrRequestExecutor, ocrHealthExecutor,
                callbackExecutor);
    }
}
