package io.github.lvdaxianer.doclens.j.autoconfigure;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingService;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentPageTaskExecutionDependencies;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentPageTaskExecutionOptions;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentPageTaskExecutionService;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResultRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskRepository;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.PageTaskWorkerScheduler;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
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
    /**
     * 页任务 OCR 执行线程数，独立于批次调度和心跳线程池，避免互相挤占。
     * 默认值和单轮扫描批量大小一致，确保调度器每次补位都能打满执行池。
     */
    private static final int PAGE_TASK_EXECUTOR_POOL_SIZE = 8;
    /** 页任务执行队列容量，避免 OCR 节点短时抖动导致任务提交无界堆积。 */
    private static final int PAGE_TASK_EXECUTOR_QUEUE_CAPACITY = 200;
    /** 页任务线程池空闲线程保活秒数，和项目内其他业务线程池保持一致。 */
    private static final int THREAD_KEEP_ALIVE_SECONDS = 60;

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
        ExecutorService pageTaskExecutor = context.getBean("doclensPageTaskExecutor", ExecutorService.class);
        DocumentPageTaskExecutionOptions options = new DocumentPageTaskExecutionOptions(properties.workerId(),
                PAGE_TASK_WORKER_BATCH_SIZE, PAGE_TASK_LOCK_SECONDS, pageTaskExecutor);
        return new DocumentPageTaskExecutionService(dependencies, transactionRunner, options);
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
    ExecutorService doclensPageTaskExecutor() {
        return new ThreadPoolExecutor(PAGE_TASK_EXECUTOR_POOL_SIZE, PAGE_TASK_EXECUTOR_POOL_SIZE,
                THREAD_KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(PAGE_TASK_EXECUTOR_QUEUE_CAPACITY),
                new NamedThreadPoolFactory("doclens-page-task-ocr-"), new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * 创建页任务 OCR worker 调度器。
     *
     * @param executionService 页任务执行服务
     * @param schedulerExecutor 页任务调度线程池
     * @return 页任务 OCR worker 调度器
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean
    @ConditionalOnBean(DocumentPageTaskExecutionService.class)
    @ConditionalOnMissingBean
    PageTaskWorkerScheduler pageTaskWorkerScheduler(
            DocumentPageTaskExecutionService executionService,
            @Qualifier("doclensPageTaskWorkerSchedulerExecutor") ScheduledExecutorService schedulerExecutor
    ) {
        return new PageTaskWorkerScheduler(executionService, schedulerExecutor, PAGE_TASK_WORKER_INTERVAL_MILLIS);
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
}
