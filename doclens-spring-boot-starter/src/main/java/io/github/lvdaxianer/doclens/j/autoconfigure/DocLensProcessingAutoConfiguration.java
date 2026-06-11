package io.github.lvdaxianer.doclens.j.autoconfigure;

import io.github.lvdaxianer.doclens.j.adapter.domain.DefaultAdapterRegistry;
import io.github.lvdaxianer.doclens.j.ingestion.application.BatchProcessingScheduler;
import io.github.lvdaxianer.doclens.j.ingestion.application.CreateBatchDependencies;
import io.github.lvdaxianer.doclens.j.ingestion.application.CreateBatchUseCase;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.infrastructure.AsyncBatchProcessingScheduler;
import io.github.lvdaxianer.doclens.j.processing.application.BatchProcessingDependencies;
import io.github.lvdaxianer.doclens.j.processing.application.BatchProcessingUseCase;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessor;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractor;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.shared.config.DocLensProperties;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
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
     * @param documentProcessingExecutor 文档处理线程池
     * @return 依赖持有对象
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean
    @ConditionalOnMissingBean
    BatchProcessingDependencies batchProcessingDependencies(
            BatchProcessingBeanDependencies dependencies,
            @Qualifier("doclensDocumentProcessingExecutor") ExecutorService documentProcessingExecutor
    ) {
        return new BatchProcessingDependencies(dependencies.documentRepository(), dependencies.resultRepository(),
                dependencies.eventRepository(), dependencies.batchRepository(), dependencies.adapterRegistry(),
                dependencies.objectStorage(), dependencies.documentTextExtractor(), dependencies.idGenerator(),
                dependencies.eventFactory(), dependencies.markdownPostProcessor(), documentProcessingExecutor);
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
