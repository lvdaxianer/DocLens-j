package io.github.lvdaxianer.doclens.j.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.adapter.domain.DefaultAdapterRegistry;
import io.github.lvdaxianer.doclens.j.ingestion.application.BatchProcessingScheduler;
import io.github.lvdaxianer.doclens.j.ingestion.application.CreateBatchDependencies;
import io.github.lvdaxianer.doclens.j.ingestion.application.CreateBatchUseCase;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.infrastructure.AsyncBatchProcessingScheduler;
import io.github.lvdaxianer.doclens.j.processing.application.BatchProcessingDependencies;
import io.github.lvdaxianer.doclens.j.processing.application.BatchProcessingUseCase;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentDeleteDependencies;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentDeleteUseCase;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentRetryDependencies;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentRetryUseCase;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigTester;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigService;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessor;
import io.github.lvdaxianer.doclens.j.processing.application.StaleDocumentRecoveryDependencies;
import io.github.lvdaxianer.doclens.j.processing.application.StaleDocumentRecoveryService;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigRepository;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractor;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.ConfigurableMarkdownPostProcessor;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.DefaultLlmMarkdownConfigTester;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.HttpMarkdownPostProcessor;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.HttpMarkdownPostProcessor.HttpMarkdownPostProcessorOptions;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.LlmMarkdownHealthChecker;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.LlmMarkdownHealthCheckScheduler;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.StaleDocumentRecoveryScheduler;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.shared.config.DocLensProperties;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.NamedThreadPoolFactory;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * 批次处理用例与后台调度自动配置。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
@AutoConfiguration(after = DocLensExtractionAutoConfiguration.class)
public class DocLensProcessingAutoConfiguration {

    private static final int BATCH_WORKER_POOL_SIZE = 1;
    private static final int BATCH_WORKER_QUEUE_CAPACITY = 1000;
    private static final int THREAD_KEEP_ALIVE_SECONDS = 60;
    private static final int LLM_MARKDOWN_TIMEOUT_SECONDS = 60;
    private static final int LLM_MARKDOWN_HEALTH_INTERVAL_SECONDS = 5;
    private static final int STALE_DOCUMENT_SCAN_INTERVAL_SECONDS = 60;
    private static final Duration STALE_DOCUMENT_THRESHOLD = Duration.ofMinutes(5);

    /**
     * 创建批次处理用例。
     *
     * @param dependencies 用例依赖
     * @param transactionRunner 事务执行器
     * @return 批次处理用例
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Bean
    @ConditionalOnMissingBean
    BatchProcessingUseCase batchProcessingUseCase(
            BatchProcessingDependencies dependencies,
            TransactionRunner transactionRunner
    ) {
        return new BatchProcessingUseCase(dependencies, transactionRunner);
    }

    /**
     * 创建批次处理用例的依赖持有对象。
     *
     * @param dependencies 自动配置依赖
     * @return 依赖持有对象
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean
    @ConditionalOnMissingBean
    BatchProcessingDependencies batchProcessingDependencies(BatchProcessingBeanDependencies dependencies) {
        return new BatchProcessingDependencies(dependencies.documentRepository(), dependencies.resultRepository(),
                dependencies.eventRepository(), dependencies.batchRepository(), dependencies.adapterRegistry(),
                dependencies.objectStorage(), dependencies.documentTextExtractor(), dependencies.idGenerator(),
                dependencies.eventFactory(), dependencies.markdownPostProcessor());
    }

    /**
     * 创建文档级重试用例。
     *
     * @param dependencies 重试依赖
     * @param transactionRunner 事务执行器
     * @return 文档级重试用例
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Bean
    @ConditionalOnMissingBean
    DocumentRetryUseCase documentRetryUseCase(
            DocumentRetryDependencies dependencies,
            TransactionRunner transactionRunner
    ) {
        return new DocumentRetryUseCase(dependencies, transactionRunner);
    }

    /**
     * 创建文档重试用例依赖持有对象。
     *
     * @param context Spring 上下文
     * @return 文档重试依赖
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Bean
    @ConditionalOnMissingBean
    DocumentRetryDependencies documentRetryDependencies(ApplicationContext context) {
        return new DocumentRetryDependencies(context.getBean(DocumentJobRepository.class),
                context.getBean(BatchRepository.class), context.getBean(OcrEventRepository.class),
                context.getBean(BatchProcessingScheduler.class), context.getBean(OcrEventFactory.class));
    }

    /**
     * 创建文档级删除用例。
     *
     * @param dependencies 删除依赖
     * @param transactionRunner 事务执行器
     * @return 文档级删除用例
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Bean
    @ConditionalOnMissingBean
    DocumentDeleteUseCase documentDeleteUseCase(
            DocumentDeleteDependencies dependencies,
            TransactionRunner transactionRunner
    ) {
        return new DocumentDeleteUseCase(dependencies, transactionRunner);
    }

    /**
     * 创建文档删除用例依赖持有对象。
     *
     * @param context Spring 上下文
     * @return 文档删除依赖
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Bean
    @ConditionalOnMissingBean
    DocumentDeleteDependencies documentDeleteDependencies(ApplicationContext context) {
        return new DocumentDeleteDependencies(context.getBean(DocumentJobRepository.class),
                context.getBean(BatchRepository.class), context.getBean(OcrResultRepository.class),
                context.getBean(OcrEventRepository.class), context.getBean(ObjectStorage.class),
                context.getBean(OcrEventFactory.class));
    }

    /**
     * 创建卡死文档恢复服务。
     *
     * @param dependencies 恢复依赖
     * @param transactionRunner 事务执行器
     * @return 卡死文档恢复服务
     * @author lvdaxianerplus
     * @date 2026-06-10
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
     * @date 2026-06-10
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
     * @date 2026-06-10
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
     * @date 2026-06-10
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
     * @date 2026-06-10
     */
    @Bean
    @ConditionalOnMissingBean(name = "staleDocumentRecoverySchedulerRunner")
    ApplicationRunner staleDocumentRecoverySchedulerRunner(StaleDocumentRecoveryScheduler scheduler) {
        return args -> scheduler.start();
    }

    /**
     * 创建 LLM Markdown 配置服务。
     *
     * @param configRepository LLM Markdown 配置仓储
     * @return LLM Markdown 配置服务
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Bean
    @ConditionalOnMissingBean
    LlmMarkdownConfigService llmMarkdownConfigService(LlmMarkdownConfigRepository configRepository) {
        return new LlmMarkdownConfigService(configRepository);
    }

    /**
     * 创建 LLM Markdown 配置测试器。
     *
     * @param objectMapper JSON 映射器
     * @return LLM 配置测试器
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Bean
    @ConditionalOnMissingBean
    LlmMarkdownConfigTester llmMarkdownConfigTester(
            ObjectMapper objectMapper,
            LlmMarkdownConfigService llmMarkdownConfigService
    ) {
        return new DefaultLlmMarkdownConfigTester(objectMapper, llmMarkdownConfigService);
    }

    /**
     * 创建 LLM Markdown 健康检查器。
     *
     * @param configRepository LLM Markdown 配置仓储
     * @param configTester LLM Markdown 配置测试器
     * @return LLM Markdown 健康检查器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Bean
    @ConditionalOnMissingBean
    LlmMarkdownHealthChecker llmMarkdownHealthChecker(
            LlmMarkdownConfigRepository configRepository,
            LlmMarkdownConfigTester configTester
    ) {
        return new LlmMarkdownHealthChecker(configRepository, configTester);
    }

    /**
     * 创建 LLM Markdown 健康检查调度线程池。
     *
     * @return LLM Markdown 健康检查调度线程池
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "doclensLlmHealthSchedulerExecutor")
    ScheduledExecutorService doclensLlmHealthSchedulerExecutor() {
        return Executors.newSingleThreadScheduledExecutor(new NamedThreadPoolFactory("doclens-llm-health-scheduler-"));
    }

    /**
     * 创建 LLM Markdown 健康检查工作线程池。
     *
     * @return LLM Markdown 健康检查工作线程池
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "doclensLlmHealthExecutor")
    ExecutorService doclensLlmHealthExecutor() {
        return Executors.newSingleThreadExecutor(new NamedThreadPoolFactory("doclens-llm-health-"));
    }

    /**
     * 创建 LLM Markdown 健康检查周期调度器。
     *
     * @param healthChecker LLM Markdown 健康检查器
     * @param schedulerExecutor LLM Markdown 健康检查调度线程池
     * @param healthExecutor LLM Markdown 健康检查工作线程池
     * @return LLM Markdown 健康检查调度器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Bean
    @ConditionalOnMissingBean
    LlmMarkdownHealthCheckScheduler llmMarkdownHealthCheckScheduler(
            LlmMarkdownHealthChecker healthChecker,
            @Qualifier("doclensLlmHealthSchedulerExecutor") ScheduledExecutorService schedulerExecutor,
            @Qualifier("doclensLlmHealthExecutor") ExecutorService healthExecutor
    ) {
        return new LlmMarkdownHealthCheckScheduler(healthChecker, schedulerExecutor, healthExecutor,
                LLM_MARKDOWN_HEALTH_INTERVAL_SECONDS);
    }

    /**
     * 应用启动完成后启动 LLM Markdown 健康检查调度。
     *
     * @param scheduler LLM Markdown 健康检查调度器
     * @return 启动任务
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Bean
    @ConditionalOnMissingBean(name = "llmMarkdownHealthCheckSchedulerRunner")
    ApplicationRunner llmMarkdownHealthCheckSchedulerRunner(LlmMarkdownHealthCheckScheduler scheduler) {
        return args -> scheduler.start();
    }

    /**
     * 创建支持运行时配置的 Markdown 后处理器。
     *
     * @param objectMapper JSON 映射器
     * @param properties Spring 配置属性
     * @param configRepository LLM Markdown 配置仓储
     * @return Markdown 后处理器
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Bean
    @ConditionalOnMissingBean
    MarkdownPostProcessor markdownPostProcessor(
            ObjectMapper objectMapper,
            DocLensSpringProperties properties,
            LlmMarkdownConfigRepository configRepository
    ) {
        MarkdownPostProcessor fallbackProcessor = fallbackMarkdownPostProcessor(objectMapper, properties);
        return new ConfigurableMarkdownPostProcessor(objectMapper, configRepository, fallbackProcessor);
    }

    /**
     * 创建 Spring 配置兜底 Markdown 后处理器。
     *
     * @param objectMapper JSON 映射器
     * @param properties Spring 配置属性
     * @return Markdown 后处理器
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    MarkdownPostProcessor fallbackMarkdownPostProcessor(ObjectMapper objectMapper, DocLensSpringProperties properties) {
        DocLensSpringProperties.LlmMarkdownProperties llmMarkdown = properties.llmMarkdown();
        if (isConfigured(llmMarkdown)) {
            // URL 与模型已配置时启用 HTTP LLM Markdown 后处理。
            HttpMarkdownPostProcessorOptions options = new HttpMarkdownPostProcessorOptions(
                    URI.create(llmMarkdown.url()), llmMarkdown.model(), llmMarkdown.apiKey(),
                    Duration.ofSeconds(LLM_MARKDOWN_TIMEOUT_SECONDS));
            return new HttpMarkdownPostProcessor(objectMapper, options);
        } else {
            // 未配置 URL 或模型时保持 OCR 合并纯文本直通。
            return MarkdownPostProcessor.noop();
        }
    }

    /**
     * 判断 LLM Markdown 是否已配置。
     *
     * @param properties LLM Markdown 配置
     * @return 是否已配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private boolean isConfigured(DocLensSpringProperties.LlmMarkdownProperties properties) {
        return properties.url() != null && !properties.url().isBlank()
                && properties.model() != null && !properties.model().isBlank();
    }

    /**
     * 收集批次处理依赖，避免用例依赖 Bean 方法参数过长。
     *
     * @param context Spring 上下文
     * @return 批次处理依赖参数
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean
    @ConditionalOnMissingBean
    BatchProcessingBeanDependencies batchProcessingBeanDependencies(ApplicationContext context) {
        BatchProcessingBeanDependencies dependencies = batchProcessingBeanDependenciesFrom(context);
        return new BatchProcessingBeanDependencies(dependencies.documentRepository(), dependencies.resultRepository(),
                dependencies.eventRepository(), dependencies.batchRepository(), dependencies.adapterRegistry(),
                dependencies.objectStorage(), dependencies.documentTextExtractor(), dependencies.idGenerator(),
                dependencies.eventFactory(), dependencies.markdownPostProcessor());
    }

    /**
     * 创建批次创建用例。
     *
     * @param dependencies 用例依赖
     * @param transactionRunner 事务执行器
     * @return 创建批次用例
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Bean
    @ConditionalOnMissingBean
    CreateBatchUseCase createBatchUseCase(CreateBatchDependencies dependencies, TransactionRunner transactionRunner) {
        return new CreateBatchUseCase(dependencies, transactionRunner);
    }

    /**
     * 创建批次处理专用线程池。
     *
     * @return 批次处理线程池
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "docLensBatchProcessingExecutor")
    ExecutorService docLensBatchProcessingExecutor() {
        return new ThreadPoolExecutor(BATCH_WORKER_POOL_SIZE, BATCH_WORKER_POOL_SIZE, THREAD_KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS, new LinkedBlockingQueue<>(BATCH_WORKER_QUEUE_CAPACITY), batchThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * 创建批次处理线程工厂。
     *
     * @return 批次处理线程工厂
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private ThreadFactory batchThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("doclens-batch-worker");
            thread.setDaemon(true);
            return thread;
        };
    }

    /**
     * 创建批次处理调度器。
     *
     * @param batchProcessingUseCase 批次处理用例
     * @param batchProcessingExecutor 批次处理线程池
     * @return 批次处理调度器
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean
    @ConditionalOnMissingBean
    BatchProcessingScheduler batchProcessingScheduler(
            BatchProcessingUseCase batchProcessingUseCase,
            @Qualifier("docLensBatchProcessingExecutor") ExecutorService batchProcessingExecutor
    ) {
        return new AsyncBatchProcessingScheduler(batchProcessingUseCase, batchProcessingExecutor);
    }

    /**
     * 创建批次创建用例的依赖持有对象。
     *
     * @param dependencies 自动配置依赖
     * @return 依赖持有对象
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean
    @ConditionalOnMissingBean
    CreateBatchDependencies createBatchDependencies(CreateBatchBeanDependencies dependencies) {
        return new CreateBatchDependencies(dependencies.batchRepository(), dependencies.documentRepository(),
                dependencies.eventRepository(), dependencies.objectStorage(), dependencies.idGenerator(),
                dependencies.properties(), dependencies.batchProcessingScheduler(), dependencies.eventFactory());
    }

    /**
     * 收集批次创建依赖，避免用例依赖 Bean 方法参数过长。
     *
     * @param context Spring 上下文
     * @return 批次创建依赖
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean
    @ConditionalOnMissingBean
    CreateBatchBeanDependencies createBatchBeanDependencies(ApplicationContext context) {
        CreateBatchBeanDependencies dependencies = createBatchBeanDependenciesFrom(context);
        return new CreateBatchBeanDependencies(dependencies.batchRepository(), dependencies.documentRepository(),
                dependencies.eventRepository(), dependencies.objectStorage(), dependencies.idGenerator(),
                dependencies.properties(), dependencies.batchProcessingScheduler(), dependencies.eventFactory());
    }

    /**
     * 从 Spring 上下文收集批次处理依赖。
     *
     * @param context Spring 上下文
     * @return 批次处理依赖
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private BatchProcessingBeanDependencies batchProcessingBeanDependenciesFrom(ApplicationContext context) {
        return new BatchProcessingBeanDependencies(context.getBean(DocumentJobRepository.class),
                context.getBean(OcrResultRepository.class), context.getBean(OcrEventRepository.class),
                context.getBean(BatchRepository.class), context.getBean(DefaultAdapterRegistry.class),
                context.getBean(ObjectStorage.class), context.getBean(DocumentTextExtractor.class),
                context.getBean(IdGenerator.class), context.getBean(OcrEventFactory.class),
                context.getBean(MarkdownPostProcessor.class));
    }

    /**
     * 批次处理 Bean 自动装配依赖。
     *
     * @param documentRepository 文档仓储
     * @param resultRepository 结果仓储
     * @param eventRepository 事件仓储
     * @param batchRepository 批次仓储
     * @param adapterRegistry 适配器注册表
     * @param objectStorage 对象存储
     * @param documentTextExtractor 文档文本提取器
     * @param idGenerator ID 生成器
     * @param eventFactory 事件工厂
     * @param markdownPostProcessor Markdown 后处理器
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private record BatchProcessingBeanDependencies(
            DocumentJobRepository documentRepository,
            OcrResultRepository resultRepository,
            OcrEventRepository eventRepository,
            BatchRepository batchRepository,
            DefaultAdapterRegistry adapterRegistry,
            ObjectStorage objectStorage,
            DocumentTextExtractor documentTextExtractor,
            IdGenerator idGenerator,
            OcrEventFactory eventFactory,
            MarkdownPostProcessor markdownPostProcessor
    ) {
    }

    /**
     * 从 Spring 上下文收集批次创建依赖。
     *
     * @param context Spring 上下文
     * @return 批次创建依赖
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private CreateBatchBeanDependencies createBatchBeanDependenciesFrom(ApplicationContext context) {
        return new CreateBatchBeanDependencies(context.getBean(BatchRepository.class),
                context.getBean(DocumentJobRepository.class), context.getBean(OcrEventRepository.class),
                context.getBean(ObjectStorage.class), context.getBean(IdGenerator.class),
                context.getBean(DocLensProperties.class), context.getBean(BatchProcessingScheduler.class),
                context.getBean(OcrEventFactory.class));
    }

    /**
     * 批次创建 Bean 自动装配依赖。
     *
     * @param batchRepository 批次仓储
     * @param documentRepository 文档仓储
     * @param eventRepository 事件仓储
     * @param objectStorage 对象存储
     * @param idGenerator ID 生成器
     * @param properties 运行时属性
     * @param batchProcessingScheduler 批次处理调度器
     * @param eventFactory 事件工厂
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private record CreateBatchBeanDependencies(
            BatchRepository batchRepository,
            DocumentJobRepository documentRepository,
            OcrEventRepository eventRepository,
            ObjectStorage objectStorage,
            IdGenerator idGenerator,
            DocLensProperties properties,
            BatchProcessingScheduler batchProcessingScheduler,
            OcrEventFactory eventFactory
    ) {
    }
}
