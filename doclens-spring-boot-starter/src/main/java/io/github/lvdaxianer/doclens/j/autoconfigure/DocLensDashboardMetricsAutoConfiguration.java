package io.github.lvdaxianer.doclens.j.autoconfigure;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrBatchHitTracker;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRunningPageTaskTracker;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrModelRegistry;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodeMetricsViewReader;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrRuntimeNodePool;
import io.github.lvdaxianer.doclens.j.query.application.DashboardOcrMetricsProvider;
import io.github.lvdaxianer.doclens.j.query.application.EmptyDashboardOcrMetricsProvider;
import io.github.lvdaxianer.doclens.j.query.infrastructure.DashboardCoreThreadPools;
import io.github.lvdaxianer.doclens.j.query.infrastructure.DashboardPageTaskWorkerSettings;
import io.github.lvdaxianer.doclens.j.query.infrastructure.DashboardThreadPools;
import io.github.lvdaxianer.doclens.j.query.infrastructure.OcrDashboardAttributionSources;
import io.github.lvdaxianer.doclens.j.query.infrastructure.OcrDashboardDataSources;
import io.github.lvdaxianer.doclens.j.query.infrastructure.OcrDashboardMetricsProvider;
import io.github.lvdaxianer.doclens.j.query.infrastructure.OcrDashboardMetricsProviderDependencies;
import io.github.lvdaxianer.doclens.j.query.infrastructure.OcrDashboardRuntimeSources;
import io.github.lvdaxianer.doclens.j.query.infrastructure.OcrNodeMetricsAggregator;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
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
        DocLensOcrNodeManagementAutoConfiguration.class,
        DocLensOcrResourceAutoConfiguration.class
})
public class DocLensDashboardMetricsAutoConfiguration {

    /**
     * 创建 Dashboard OCR 指标提供器。
     *
     * @param dependencies Dashboard OCR 指标提供器依赖
     * @return Dashboard OCR 指标提供器
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Bean
    @ConditionalOnBean({
            OcrNodeRepository.class,
            OcrNodeCallRepository.class,
            OcrRuntimeNodePool.class,
            OcrModelRegistry.class
    })
    @ConditionalOnMissingBean
    DashboardOcrMetricsProvider dashboardOcrMetricsProvider(OcrDashboardMetricsProviderDependencies dependencies) {
        return new OcrDashboardMetricsProvider(dependencies);
    }

    /**
     * 创建 Dashboard OCR 指标提供器依赖。
     *
     * @param dataSources 查询侧仓储集合
     * @param runtimeSources 运行态依赖集合
     * @param attributionSources 路由归因依赖集合
     * @return Dashboard OCR 指标提供器依赖
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Bean
    @ConditionalOnMissingBean
    OcrDashboardMetricsProviderDependencies dashboardOcrMetricsProviderDependencies(
            OcrDashboardDataSources dataSources,
            OcrDashboardRuntimeSources runtimeSources,
            OcrDashboardAttributionSources attributionSources
    ) {
        return new OcrDashboardMetricsProviderDependencies(dataSources, runtimeSources, attributionSources);
    }

    /**
     * 创建 Dashboard OCR 查询侧仓储集合。
     *
     * @param nodeRepository OCR 节点仓储
     * @param callRepository OCR 调用记录仓储
     * @return Dashboard OCR 查询侧仓储集合
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Bean
    @ConditionalOnMissingBean
    OcrDashboardDataSources dashboardOcrDataSources(
            OcrNodeRepository nodeRepository,
            OcrNodeCallRepository callRepository
    ) {
        return new OcrDashboardDataSources(nodeRepository, callRepository);
    }

    /**
     * 创建 Dashboard OCR 运行态依赖集合。
     *
     * @param nodePool OCR 运行时节点池
     * @param threadPools Dashboard 线程池集合
     * @return Dashboard OCR 运行态依赖集合
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Bean
    @ConditionalOnMissingBean
    OcrDashboardRuntimeSources dashboardOcrRuntimeSources(
            OcrRuntimeNodePool nodePool,
            DashboardThreadPools threadPools
    ) {
        return new OcrDashboardRuntimeSources(nodePool, threadPools);
    }

    /**
     * 创建 Dashboard OCR 路由归因依赖集合。
     *
     * @param batchHitTracker 批次运行时命中跟踪器
     * @param runningPageTaskTracker 运行中图片页任务追踪器
     * @param modelRegistry OCR 模型注册表
     * @return Dashboard OCR 路由归因依赖集合
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Bean
    @ConditionalOnMissingBean
    OcrDashboardAttributionSources dashboardOcrAttributionSources(
            OcrBatchHitTracker batchHitTracker,
            OcrRunningPageTaskTracker runningPageTaskTracker,
            OcrModelRegistry modelRegistry
    ) {
        return new OcrDashboardAttributionSources(batchHitTracker, runningPageTaskTracker, modelRegistry);
    }

    /**
     * 创建 OCR 节点指标读取器。
     *
     * @param callRepository OCR 调用记录仓储
     * @return OCR 节点指标读取器
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Bean
    @ConditionalOnBean({OcrNodeCallRepository.class, OcrRuntimeNodePool.class})
    @ConditionalOnMissingBean
    OcrNodeMetricsViewReader ocrNodeMetricsViewReader(
            OcrNodeCallRepository callRepository,
            OcrRuntimeNodePool nodePool
    ) {
        return new OcrNodeMetricsAggregator(callRepository, nodePool);
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
     * @param context Spring 上下文
     * @param pageTaskWorkerSettingsProvider 页任务 worker 生效配置
     * @return Dashboard 线程池集合
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Bean
    @ConditionalOnMissingBean
    DashboardThreadPools dashboardThreadPools(
            ApplicationContext context,
            ObjectProvider<DocLensPageTaskWorkerAutoConfiguration.PageTaskWorkerRuntimeSettings>
                    pageTaskWorkerSettingsProvider
    ) {
        return new DashboardThreadPools(coreThreadPools(context), pageTaskExecutor(context),
                pageTaskWorkerSettings(pageTaskWorkerSettingsProvider.getIfAvailable()));
    }

    /**
     * 创建 Dashboard 核心线程池集合。
     *
     * @param context Spring 上下文
     * @return Dashboard 核心线程池集合
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private DashboardCoreThreadPools coreThreadPools(ApplicationContext context) {
        return new DashboardCoreThreadPools(context.getBean("doclensDocumentProcessingExecutor", ExecutorService.class),
                context.getBean("doclensOcrRequestExecutor", ExecutorService.class),
                context.getBean("doclensOcrHealthExecutor", ExecutorService.class),
                context.getBean("doclensCallbackExecutor", ExecutorService.class),
                context.getBean("doclensLlmMarkdownChunkExecutor", ExecutorService.class));
    }

    /**
     * 获取页任务 OCR 执行线程池。
     *
     * @param context Spring 上下文
     * @return 页任务 OCR 执行线程池
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private Optional<ExecutorService> pageTaskExecutor(ApplicationContext context) {
        if (context.containsBean("doclensPageTaskExecutor")) {
            // 页任务 worker 已启用时，读取实际执行线程池用于运行态指标。
            return Optional.of(context.getBean("doclensPageTaskExecutor", ExecutorService.class));
        } else {
            // 页任务 worker 未启用时，Dashboard 使用空线程池指标保持响应稳定。
            return Optional.empty();
        }
    }

    /**
     * 映射页任务 worker 生效配置。
     *
     * @param settings 页任务 worker 生效配置
     * @return Dashboard 页任务 worker 配置
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private Optional<DashboardPageTaskWorkerSettings> pageTaskWorkerSettings(
            DocLensPageTaskWorkerAutoConfiguration.PageTaskWorkerRuntimeSettings settings
    ) {
        if (settings == null) {
            // 页任务 worker 未启用时，Dashboard 仍保留线程池空指标。
            return Optional.empty();
        } else {
            // 页任务 worker 已启用时，暴露最终生效配置给前端展示容量来源。
            return Optional.of(new DashboardPageTaskWorkerSettings(
                    new DashboardPageTaskWorkerSettings.Execution(settings.batchSize(), settings.lockSeconds(),
                            settings.poolSize(), settings.queueCapacity()),
                    new DashboardPageTaskWorkerSettings.Recovery(settings.recoveryLimit(),
                            settings.intervalMillis())));
        }
    }
}
