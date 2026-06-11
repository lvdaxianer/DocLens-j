package io.github.lvdaxianer.doclens.j.autoconfigure;

import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.processing.application.StaleDocumentRecoveryDependencies;
import io.github.lvdaxianer.doclens.j.processing.application.StaleDocumentRecoveryService;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.StaleDocumentRecoveryScheduler;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.NamedThreadPoolFactory;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * 卡死文档恢复自动配置。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
@AutoConfiguration(after = DocLensProcessingAutoConfiguration.class)
@ConditionalOnBean({
        DocumentJobRepository.class,
        BatchRepository.class,
        OcrEventRepository.class,
        OcrEventFactory.class
})
public class DocLensStaleDocumentRecoveryAutoConfiguration {

    private static final int STALE_DOCUMENT_SCAN_INTERVAL_SECONDS = 60;
    private static final Duration STALE_DOCUMENT_THRESHOLD = Duration.ofMinutes(5);

    /**
     * 创建卡死文档恢复服务。
     *
     * @param dependencies 恢复依赖
     * @param transactionRunner 事务执行器
     * @return 卡死文档恢复服务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean
    @ConditionalOnMissingBean
    StaleDocumentRecoveryService staleDocumentRecoveryService(
            StaleDocumentRecoveryDependencies dependencies,
            TransactionRunner transactionRunner
    ) {
        return new StaleDocumentRecoveryService(dependencies, transactionRunner, STALE_DOCUMENT_THRESHOLD);
    }

    /**
     * 创建卡死文档恢复服务依赖持有对象。
     *
     * @param context Spring 上下文
     * @return 恢复依赖
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean
    @ConditionalOnMissingBean
    StaleDocumentRecoveryDependencies staleDocumentRecoveryDependencies(ApplicationContext context) {
        return new StaleDocumentRecoveryDependencies(context.getBean(DocumentJobRepository.class),
                context.getBean(BatchRepository.class), context.getBean(OcrEventRepository.class),
                context.getBean(OcrEventFactory.class));
    }

    /**
     * 创建卡死文档恢复调度线程池。
     *
     * @return 恢复调度线程池
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "doclensStaleDocumentRecoverySchedulerExecutor")
    ScheduledExecutorService doclensStaleDocumentRecoverySchedulerExecutor() {
        return Executors.newSingleThreadScheduledExecutor(
                new NamedThreadPoolFactory("doclens-stale-document-recovery-"));
    }

    /**
     * 创建卡死文档恢复周期调度器。
     *
     * @param recoveryService 恢复服务
     * @param schedulerExecutor 恢复调度线程池
     * @return 卡死文档恢复调度器
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean
    @ConditionalOnMissingBean
    StaleDocumentRecoveryScheduler staleDocumentRecoveryScheduler(
            StaleDocumentRecoveryService recoveryService,
            @Qualifier("doclensStaleDocumentRecoverySchedulerExecutor") ScheduledExecutorService schedulerExecutor
    ) {
        return new StaleDocumentRecoveryScheduler(recoveryService, schedulerExecutor,
                STALE_DOCUMENT_SCAN_INTERVAL_SECONDS);
    }

    /**
     * 应用启动完成后启动卡死文档恢复调度。
     *
     * @param scheduler 卡死文档恢复调度器
     * @return 启动任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean
    @ConditionalOnMissingBean(name = "staleDocumentRecoverySchedulerRunner")
    ApplicationRunner staleDocumentRecoverySchedulerRunner(StaleDocumentRecoveryScheduler scheduler) {
        return args -> scheduler.start();
    }
}
