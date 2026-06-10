package io.github.lvdaxianer.doclens.j.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.adapter.domain.DefaultAdapterRegistry;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrCallIdGenerator;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodeSelector;
import io.github.lvdaxianer.doclens.j.adapter.application.WeightedCapacityOcrNodeSelector;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrAdapter;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.MybatisPlusOcrNodeCallRepository;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.MybatisPlusOcrGovernanceConfigRepository;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.MybatisPlusOcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrGovernanceConfigMapper;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrNodeCallMapper;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrNodeMapper;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrRuntimeNodePool;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.StubOcrAdapter;
import io.github.lvdaxianer.doclens.j.api.DefaultDocLensEngine;
import io.github.lvdaxianer.doclens.j.api.DocLensEngine;
import io.github.lvdaxianer.doclens.j.api.DocLensEventSink;
import io.github.lvdaxianer.doclens.j.api.NoopDocLensEventSink;
import io.github.lvdaxianer.doclens.j.ingestion.application.CreateBatchUseCase;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.infrastructure.BatchMapper;
import io.github.lvdaxianer.doclens.j.ingestion.infrastructure.MybatisPlusBatchRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentDeleteUseCase;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentRetryUseCase;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.DocumentJobMapper;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.DocumentPageResultMapper;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.DocumentPageTaskMapper;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.LlmMarkdownConfigMapper;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.MybatisPlusDocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.MybatisPlusDocumentPageResultRepository;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.MybatisPlusDocumentPageTaskRepository;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.MybatisPlusLlmMarkdownConfigRepository;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.MybatisPlusOcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.MybatisPlusOcrResultRepository;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.OcrEventMapper;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.OcrResultMapper;
import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryService;
import io.github.lvdaxianer.doclens.j.query.application.DashboardOcrMetricsProvider;
import io.github.lvdaxianer.doclens.j.query.application.OcrQueryService;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.shared.config.DocLensProperties;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGeneratorOcrCallIdGenerator;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.JsonCodec;
import io.github.lvdaxianer.doclens.j.shared.config.MybatisPlusConfiguration;
import io.github.lvdaxianer.doclens.j.shared.config.WorkerConfiguration;
import io.github.lvdaxianer.doclens.j.shared.application.SpringTransactionRunner;
import io.github.lvdaxianer.doclens.j.storage.LocalObjectStorage;
import java.util.List;
import org.mybatis.spring.annotation.MapperScan;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 嵌入式 DocLens 运行时的 Spring Boot 自动配置。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@AutoConfiguration
@EnableConfigurationProperties(DocLensSpringProperties.class)
@MapperScan(basePackageClasses = {
        BatchMapper.class,
        DocumentJobMapper.class,
        DocumentPageResultMapper.class,
        DocumentPageTaskMapper.class,
        LlmMarkdownConfigMapper.class,
        OcrEventMapper.class,
        OcrResultMapper.class,
        OcrGovernanceConfigMapper.class,
        OcrNodeMapper.class,
        OcrNodeCallMapper.class
}, annotationClass = Mapper.class)
@Import({
        MybatisPlusConfiguration.class,
        WorkerConfiguration.class,
        StubOcrAdapter.class,
        MybatisPlusBatchRepository.class,
        MybatisPlusDocumentJobRepository.class,
        MybatisPlusDocumentPageResultRepository.class,
        MybatisPlusDocumentPageTaskRepository.class,
        MybatisPlusLlmMarkdownConfigRepository.class,
        MybatisPlusOcrEventRepository.class,
        MybatisPlusOcrResultRepository.class,
        MybatisPlusOcrGovernanceConfigRepository.class,
        MybatisPlusOcrNodeRepository.class,
        MybatisPlusOcrNodeCallRepository.class,
        OcrRuntimeNodePool.class,
        LocalObjectStorage.class
})
public class DocLensAutoConfiguration {

    /**
     * 将 Spring 配置属性适配为核心运行时属性。
     *
     * @param properties 绑定到 Spring 的属性
     * @return 核心运行时属性
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Bean
    @ConditionalOnMissingBean
    DocLensProperties docLensProperties(DocLensSpringProperties properties) {
        return new DocLensProperties(properties.storageRoot(), properties.autoProcessOnUpload(), properties.workerId(),
                new DocLensProperties.CallbackProperties(properties.callback().maxRetries(),
                        properties.callback().timeoutSeconds()),
                new DocLensProperties.AdapterProperties(properties.adapter().defaultKey()),
                new DocLensProperties.PaddleOcrProperties(properties.paddleOcr().enabled(),
                        properties.paddleOcr().endpoint(), properties.paddleOcr().timeoutSeconds(),
                        properties.paddleOcr().visualize()),
                new DocLensProperties.OcrHealthProperties(properties.ocr().failureThreshold(),
                        properties.ocr().recoverySuccessThreshold()),
                new DocLensProperties.ExtractionProperties(properties.extraction().ocrConcurrency()),
                new DocLensProperties.PdfRenderProperties(properties.pdfRender().dpi(),
                        properties.pdfRender().imageFormat()),
                new DocLensProperties.WordConversionProperties(properties.wordConversion().command(),
                        properties.wordConversion().timeoutSeconds()),
                threadPools(properties.threadPools()));
    }

    /**
     * 将 Spring 线程池属性转换为 core 线程池属性。
     *
     * @param properties Spring 线程池属性
     * @return core 线程池属性
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private DocLensProperties.ThreadPoolsProperties threadPools(
            DocLensSpringProperties.ThreadPoolsProperties properties
    ) {
        return new DocLensProperties.ThreadPoolsProperties(
                threadPool(properties.documentProcessingThreadPool()),
                threadPool(properties.ocrRequestThreadPool()),
                threadPool(properties.ocrHealthThreadPool()),
                threadPool(properties.callbackThreadPool()));
    }

    /**
     * 将 Spring 单线程池属性转换为 core 单线程池属性。
     *
     * @param properties Spring 单线程池属性
     * @return core 单线程池属性
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private DocLensProperties.ThreadPoolProperties threadPool(DocLensSpringProperties.ThreadPoolProperties properties) {
        return new DocLensProperties.ThreadPoolProperties(properties.coreSize(), properties.maxSize(),
                properties.queueCapacity(), properties.keepAliveSeconds(), properties.threadNamePrefix());
    }

    /**
     * 创建默认标识生成器。
     *
     * @return 标识生成器
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Bean
    @ConditionalOnMissingBean
    IdGenerator idGenerator() {
        return new IdGenerator();
    }

    /**
     * 创建 OCR 调用记录 ID 生成器。
     *
     * @param idGenerator 通用 ID 生成器
     * @return OCR 调用记录 ID 生成器
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean
    @ConditionalOnMissingBean
    OcrCallIdGenerator ocrCallIdGenerator(IdGenerator idGenerator) {
        return new IdGeneratorOcrCallIdGenerator(idGenerator);
    }

    /**
     * 创建 OCR 节点选择器。
     *
     * @return OCR 节点选择器
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean
    @ConditionalOnMissingBean
    OcrNodeSelector ocrNodeSelector(DocLensSpringProperties properties) {
        return new WeightedCapacityOcrNodeSelector(properties.ocr().idleFactor(), properties.ocr().weightFactor(),
                properties.ocr().topBucketThreshold());
    }

    /**
     * 为非 Web 嵌入式宿主创建默认对象映射器。
     *
     * @return Jackson 对象映射器
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Bean
    @ConditionalOnMissingBean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * 创建共享 JSON 编解码器。
     *
     * @param objectMapper Jackson 对象映射器
     * @return JSON 编解码器
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Bean
    @ConditionalOnMissingBean
    JsonCodec jsonCodec(ObjectMapper objectMapper) {
        return new JsonCodec(objectMapper);
    }

    /**
     * 创建 Spring 事务适配器。
     *
     * @param transactionTemplate Spring 事务模板
     * @return 事务执行器
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Bean
    @ConditionalOnMissingBean
    TransactionRunner transactionRunner(TransactionTemplate transactionTemplate) {
        return new SpringTransactionRunner(transactionTemplate);
    }

    /**
     * 创建事件工厂。
     *
     * @param idGenerator 标识生成器
     * @return OCR 事件工厂
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Bean
    @ConditionalOnMissingBean
    OcrEventFactory ocrEventFactory(IdGenerator idGenerator) {
        return new OcrEventFactory(idGenerator);
    }

    /**
     * 创建默认事件接收器。
     *
     * @return 空操作事件接收器
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Bean
    @ConditionalOnMissingBean
    DocLensEventSink docLensEventSink() {
        return new NoopDocLensEventSink();
    }

    /**
     * 创建 OCR 适配器注册表。
     *
     * @param adapters OCR 适配器集合
     * @return 适配器注册表
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Bean
    @ConditionalOnMissingBean
    DefaultAdapterRegistry defaultAdapterRegistry(List<OcrAdapter> adapters) {
        return new DefaultAdapterRegistry(adapters);
    }

    /**
     * 创建查询服务。
     *
     * @param batchRepository 批次仓储
     * @param documentRepository 文档仓储
     * @param resultRepository 结果仓储
     * @param eventRepository 事件仓储
     * @return OCR 查询服务
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Bean
    @ConditionalOnMissingBean
    OcrQueryService ocrQueryService(
            BatchRepository batchRepository,
            DocumentJobRepository documentRepository,
            OcrResultRepository resultRepository,
            OcrEventRepository eventRepository
    ) {
        return new OcrQueryService(batchRepository, documentRepository, resultRepository, eventRepository);
    }

    /**
     * 创建 Dashboard 查询服务。
     *
     * @param batchRepository 批次仓储
     * @param documentRepository 文档仓储
     * @param eventRepository 事件仓储
     * @return Dashboard 查询服务
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean
    @ConditionalOnMissingBean
    DashboardQueryService dashboardQueryService(
            BatchRepository batchRepository,
            DocumentJobRepository documentRepository,
            OcrEventRepository eventRepository,
            DashboardOcrMetricsProvider ocrMetricsProvider
    ) {
        return new DashboardQueryService(batchRepository, documentRepository, eventRepository, ocrMetricsProvider);
    }

    /**
     * 创建公开的 DocLens 引擎。
     *
     * @param createBatchUseCase 创建批次用例
     * @param queryService 查询服务
     * @param documentRetryUseCase 文档重试用例
     * @param documentDeleteUseCase 文档删除用例
     * @return DocLens 引擎
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Bean
    @ConditionalOnMissingBean
    DocLensEngine docLensEngine(
            CreateBatchUseCase createBatchUseCase,
            OcrQueryService queryService,
            DefaultAdapterRegistry adapterRegistry,
            DocumentRetryUseCase documentRetryUseCase,
            DocumentDeleteUseCase documentDeleteUseCase
    ) {
        return new DefaultDocLensEngine(createBatchUseCase, queryService, adapterRegistry, documentRetryUseCase,
                documentDeleteUseCase);
    }

}
