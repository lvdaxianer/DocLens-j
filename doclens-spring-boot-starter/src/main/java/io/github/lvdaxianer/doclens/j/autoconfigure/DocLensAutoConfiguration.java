package io.github.lvdaxianer.doclens.j.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.adapter.domain.DefaultAdapterRegistry;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrAdapter;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.MybatisPlusOcrNodeCallRepository;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.MybatisPlusOcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrNodeCallMapper;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrNodeMapper;
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
import io.github.lvdaxianer.doclens.j.processing.infrastructure.DocumentJobMapper;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.MybatisPlusDocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.MybatisPlusOcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.MybatisPlusOcrResultRepository;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.OcrEventMapper;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.OcrResultMapper;
import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryService;
import io.github.lvdaxianer.doclens.j.query.application.OcrQueryService;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.shared.config.DocLensProperties;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
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
        OcrEventMapper.class,
        OcrResultMapper.class,
        OcrNodeMapper.class,
        OcrNodeCallMapper.class
}, annotationClass = Mapper.class)
@Import({
        MybatisPlusConfiguration.class,
        WorkerConfiguration.class,
        StubOcrAdapter.class,
        MybatisPlusBatchRepository.class,
        MybatisPlusDocumentJobRepository.class,
        MybatisPlusOcrEventRepository.class,
        MybatisPlusOcrResultRepository.class,
        MybatisPlusOcrNodeRepository.class,
        MybatisPlusOcrNodeCallRepository.class,
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
                new DocLensProperties.ExtractionProperties(properties.extraction().ocrConcurrency()),
                new DocLensProperties.PdfRenderProperties(properties.pdfRender().dpi(),
                        properties.pdfRender().imageFormat()),
                new DocLensProperties.WordConversionProperties(properties.wordConversion().command(),
                        properties.wordConversion().timeoutSeconds()));
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
            OcrEventRepository eventRepository
    ) {
        return new DashboardQueryService(batchRepository, documentRepository, eventRepository);
    }

    /**
     * 创建公开的 DocLens 引擎。
     *
     * @param createBatchUseCase 创建批次用例
     * @param queryService 查询服务
     * @return DocLens 引擎
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Bean
    @ConditionalOnMissingBean
    DocLensEngine docLensEngine(
            CreateBatchUseCase createBatchUseCase,
            OcrQueryService queryService,
            DefaultAdapterRegistry adapterRegistry
    ) {
        return new DefaultDocLensEngine(createBatchUseCase, queryService, adapterRegistry);
    }
}
