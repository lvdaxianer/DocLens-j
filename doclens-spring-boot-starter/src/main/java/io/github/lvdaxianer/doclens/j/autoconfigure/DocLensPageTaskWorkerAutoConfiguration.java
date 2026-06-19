package io.github.lvdaxianer.doclens.j.autoconfigure;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingService;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentPageTaskAggregationDependencies;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentPageTaskAggregationService;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentPageTaskExecutionDependencies;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentPageTaskExecutionOptions;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentPageTaskExecutionService;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentPageTaskRecoveryDependencies;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentPageTaskRecoveryService;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResultRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.PageTaskWorkerScheduler;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.PageTaskWorkerScheduler.PageTaskWorkerSchedulerDependencies;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.NamedThreadPoolFactory;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * 文档页任务 OCR worker 自动配置。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
@AutoConfiguration(after = {DocLensProcessingAutoConfiguration.class, DocLensOcrResourceAutoConfiguration.class})
public class DocLensPageTaskWorkerAutoConfiguration {

    /** 默认 worker 每 500ms 扫描一批任务，平衡补位速度与空队列开销。 */
    private static final int PAGE_TASK_WORKER_INTERVAL_MILLIS = 500;
    /** 默认批量大小与执行线程数保持一致，单轮扫描即可填满本进程执行槽位。 */
    private static final int PAGE_TASK_WORKER_BATCH_SIZE = 8;
    /** 页任务抢占锁默认 60 秒，后续恢复任务会负责处理进程崩溃后的过期锁。 */
    private static final int PAGE_TASK_LOCK_SECONDS = 60;
    /** 单轮恢复最多扫描 32 个过期页任务，避免启动瞬间恢复任务压垮数据库。 */
    private static final int PAGE_TASK_RECOVERY_LIMIT = 32;
    /**
     * 页任务 OCR 执行线程数，独立于批次调度和心跳线程池，避免互相挤占。
     * 默认值和单轮扫描批量大小一致，确保调度器每次补位都能打满执行池。
     */
    private static final int PAGE_TASK_EXECUTOR_POOL_SIZE = 8;
    /** 页任务执行队列容量，避免 OCR 节点短时抖动导致任务提交无界堆积。 */
    private static final int PAGE_TASK_EXECUTOR_QUEUE_CAPACITY = 200;
    /** 页任务线程池空闲线程保活秒数，和项目内其他业务线程池保持一致。 */
    private static final int THREAD_KEEP_ALIVE_SECONDS = 60;
    /** OCR 请求超时之外预留的页任务锁缓冲，避免网络返回和落库窗口误触发恢复。 */
    private static final int PAGE_TASK_LOCK_SAFETY_BUFFER_SECONDS = 30;

    /**
     * 创建页任务 worker 生效运行时配置。
     *
     * @param properties DocLens 配置属性
     * @return 页任务 worker 生效运行时配置
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Bean
    @ConditionalOnBean(OcrRoutingService.class)
    @ConditionalOnMissingBean
    PageTaskWorkerRuntimeSettings pageTaskWorkerRuntimeSettings(DocLensSpringProperties properties) {
        DocLensSpringProperties.PageTaskWorkerProperties worker = properties.pageTaskWorker();
        int derivedConcurrency = derivedNodeConcurrency(properties);
        int poolSize = positiveOrDefault(worker.poolSize(), derivedConcurrency);
        int batchSize = positiveOrDefault(worker.batchSize(), poolSize);
        int lockSeconds = positiveOrDefault(worker.lockSeconds(), derivedLockSeconds(properties));
        int queueCapacity = positiveOrDefault(worker.queueCapacity(), PAGE_TASK_EXECUTOR_QUEUE_CAPACITY);
        int recoveryLimit = positiveOrDefault(worker.recoveryLimit(), PAGE_TASK_RECOVERY_LIMIT);
        int intervalMillis = positiveOrDefault(worker.intervalMillis(), PAGE_TASK_WORKER_INTERVAL_MILLIS);
        return new PageTaskWorkerRuntimeSettings(batchSize, lockSeconds, poolSize, queueCapacity, recoveryLimit,
                intervalMillis);
    }

    /**
     * 创建文档页任务执行服务。
     *
     * @param context Spring 上下文
     * @return 文档页任务执行服务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean
    @ConditionalOnBean(OcrRoutingService.class)
    @ConditionalOnMissingBean
    DocumentPageTaskExecutionService documentPageTaskExecutionService(ApplicationContext context) {
        DocumentPageTaskExecutionDependencies dependencies = context.getBean(DocumentPageTaskExecutionDependencies.class);
        TransactionRunner transactionRunner = context.getBean(TransactionRunner.class);
        DocLensSpringProperties properties = context.getBean(DocLensSpringProperties.class);
        PageTaskWorkerRuntimeSettings settings = context.getBean(PageTaskWorkerRuntimeSettings.class);
        ExecutorService pageTaskExecutor = context.getBean("doclensPageTaskExecutor", ExecutorService.class);
        // 页任务执行器只负责单页 OCR，成功后的文档收口交给聚合服务统一判断。
        // 这里通过监听器注入聚合入口，避免执行服务直接依赖结果落库细节。
        DocumentPageTaskAggregationService aggregationService =
                context.getBean(DocumentPageTaskAggregationService.class);
        DocumentPageTaskExecutionOptions options = new DocumentPageTaskExecutionOptions(properties.workerId(),
                settings.batchSize(), settings.lockSeconds(), pageTaskExecutor,
                aggregationService::recordSuccess);
        return new DocumentPageTaskExecutionService(dependencies, transactionRunner, options);
    }

    /**
     * 创建文档页任务聚合服务。
     *
     * @param dependencies 聚合依赖
     * @param transactionRunner 事务执行器
     * @return 文档页任务聚合服务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean
    @ConditionalOnBean(OcrRoutingService.class)
    @ConditionalOnMissingBean
    DocumentPageTaskAggregationService documentPageTaskAggregationService(
            DocumentPageTaskAggregationDependencies dependencies,
            TransactionRunner transactionRunner
    ) {
        return new DocumentPageTaskAggregationService(dependencies, transactionRunner);
    }

    /**
     * 创建文档页任务聚合依赖集合。
     *
     * @param context Spring 上下文
     * @return 文档页任务聚合依赖
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean
    @ConditionalOnBean(OcrRoutingService.class)
    @ConditionalOnMissingBean
    DocumentPageTaskAggregationDependencies documentPageTaskAggregationDependencies(ApplicationContext context) {
        return new DocumentPageTaskAggregationDependencies(context.getBean(DocumentJobRepository.class),
                context.getBean(BatchRepository.class), context.getBean(DocumentPageTaskRepository.class),
                context.getBean(DocumentPageResultRepository.class), context.getBean(OcrResultRepository.class),
                context.getBean(ObjectStorage.class), context.getBean(IdGenerator.class),
                context.getBean(OcrEventRepository.class), context.getBean(CallbackJobRepository.class),
                context.getBean(OcrEventFactory.class));
    }

    /**
     * 创建文档页任务恢复服务。
     *
     * @param dependencies 恢复依赖
     * @param transactionRunner 事务执行器
     * @param aggregationService 聚合服务
     * @return 文档页任务恢复服务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean
    @ConditionalOnBean(OcrRoutingService.class)
    @ConditionalOnMissingBean
    DocumentPageTaskRecoveryService documentPageTaskRecoveryService(
            DocumentPageTaskRecoveryDependencies dependencies,
            TransactionRunner transactionRunner,
            DocumentPageTaskAggregationService aggregationService
    ) {
        return new DocumentPageTaskRecoveryService(dependencies, transactionRunner, aggregationService::recordSuccess);
    }

    /**
     * 创建文档页任务恢复依赖集合。
     *
     * @param context Spring 上下文
     * @return 文档页任务恢复依赖
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean
    @ConditionalOnBean(OcrRoutingService.class)
    @ConditionalOnMissingBean
    DocumentPageTaskRecoveryDependencies documentPageTaskRecoveryDependencies(ApplicationContext context) {
        return new DocumentPageTaskRecoveryDependencies(context.getBean(DocumentPageTaskRepository.class),
                context.getBean(DocumentPageResultRepository.class));
    }

    /**
     * 创建文档页任务执行依赖集合。
     *
     * @param context Spring 上下文
     * @return 文档页任务执行依赖
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean
    @ConditionalOnBean(OcrRoutingService.class)
    @ConditionalOnMissingBean
    DocumentPageTaskExecutionDependencies documentPageTaskExecutionDependencies(ApplicationContext context) {
        return new DocumentPageTaskExecutionDependencies(context.getBean(DocumentPageTaskRepository.class),
                context.getBean(DocumentJobRepository.class), context.getBean(DocumentPageResultRepository.class),
                context.getBean(ObjectStorage.class), context.getBean(OcrRoutingService.class));
    }

    /**
     * 创建页任务 worker 调度线程池。
     *
     * @return 页任务 worker 调度线程池
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnBean(OcrRoutingService.class)
    @ConditionalOnMissingBean(name = "doclensPageTaskWorkerSchedulerExecutor")
    ScheduledExecutorService doclensPageTaskWorkerSchedulerExecutor() {
        return Executors.newSingleThreadScheduledExecutor(new NamedThreadPoolFactory("doclens-page-task-worker-"));
    }

    /**
     * 创建页任务 OCR 执行线程池。
     *
     * @return 页任务 OCR 执行线程池
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnBean(OcrRoutingService.class)
    @ConditionalOnMissingBean(name = "doclensPageTaskExecutor")
    ExecutorService doclensPageTaskExecutor(PageTaskWorkerRuntimeSettings settings) {
        return new ThreadPoolExecutor(settings.poolSize(), settings.poolSize(),
                THREAD_KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(settings.queueCapacity()),
                new NamedThreadPoolFactory("doclens-page-task-ocr-"), new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * 创建页任务 OCR worker 调度器。
     *
     * @param executionService 页任务执行服务
     * @param recoveryService 页任务恢复服务
     * @param schedulerExecutor 页任务调度线程池
     * @param settings 页任务 worker 运行时配置
     * @return 页任务 OCR worker 调度器
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean
    @ConditionalOnBean(DocumentPageTaskExecutionService.class)
    @ConditionalOnMissingBean
    PageTaskWorkerScheduler pageTaskWorkerScheduler(
            DocumentPageTaskExecutionService executionService,
            DocumentPageTaskRecoveryService recoveryService,
            @Qualifier("doclensPageTaskWorkerSchedulerExecutor") ScheduledExecutorService schedulerExecutor,
            PageTaskWorkerRuntimeSettings settings
    ) {
        PageTaskWorkerSchedulerDependencies dependencies =
                new PageTaskWorkerSchedulerDependencies(executionService, recoveryService, schedulerExecutor);
        return new PageTaskWorkerScheduler(dependencies, settings.intervalMillis(), settings.recoveryLimit());
    }

    /**
     * 应用启动完成后启动页任务 OCR worker 调度。
     *
     * @param scheduler 页任务 OCR worker 调度器
     * @return 启动任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean
    @ConditionalOnBean(PageTaskWorkerScheduler.class)
    @ConditionalOnMissingBean(name = "pageTaskWorkerSchedulerRunner")
    ApplicationRunner pageTaskWorkerSchedulerRunner(PageTaskWorkerScheduler scheduler) {
        return args -> scheduler.start();
    }

    /**
     * 根据启用的启动节点并发派生本地页任务并发。
     *
     * @param properties DocLens 配置属性
     * @return 派生并发数
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    static int derivedNodeConcurrency(DocLensSpringProperties properties) {
        return properties.paddleOcr().bootstrapNodes().stream()
                .filter(DocLensSpringProperties.PaddleOcrNodeProperties::enabled)
                .mapToInt(DocLensSpringProperties.PaddleOcrNodeProperties::maxConcurrency)
                .max()
                .orElse(PAGE_TASK_EXECUTOR_POOL_SIZE);
    }

    /**
     * 根据 OCR 请求超时派生页任务锁时长。
     *
     * @param properties DocLens 配置属性
     * @return 派生锁秒数
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private int derivedLockSeconds(DocLensSpringProperties properties) {
        int timeoutSeconds = Math.max(1, properties.paddleOcr().timeoutSeconds());
        return Math.max(PAGE_TASK_LOCK_SECONDS, timeoutSeconds + PAGE_TASK_LOCK_SAFETY_BUFFER_SECONDS);
    }

    /**
     * 正数使用配置值，否则使用默认值。
     *
     * @param value 配置值
     * @param defaultValue 默认值
     * @return 正整数
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private int positiveOrDefault(int value, int defaultValue) {
        return value > 0 ? value : Math.max(1, defaultValue);
    }

    /**
     * 页任务 worker 生效运行时配置。
     *
     * @param batchSize 每轮抢占页任务数量
     * @param lockSeconds 页任务锁秒数
     * @param poolSize 页任务执行线程数
     * @param queueCapacity 页任务执行队列容量
     * @param recoveryLimit 每轮恢复过期页任务数量
     * @param intervalMillis 页任务扫描间隔毫秒
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    record PageTaskWorkerRuntimeSettings(
            int batchSize,
            int lockSeconds,
            int poolSize,
            int queueCapacity,
            int recoveryLimit,
            int intervalMillis
    ) {
    }
}
