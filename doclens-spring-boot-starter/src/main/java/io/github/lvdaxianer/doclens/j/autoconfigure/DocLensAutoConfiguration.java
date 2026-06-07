package io.github.lvdaxianer.doclens.j.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.adapter.domain.DefaultAdapterRegistry;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrAdapter;
import io.github.lvdaxianer.doclens.j.api.DefaultDocLensEngine;
import io.github.lvdaxianer.doclens.j.api.DocLensEngine;
import io.github.lvdaxianer.doclens.j.api.DocLensEventSink;
import io.github.lvdaxianer.doclens.j.api.NoopDocLensEventSink;
import io.github.lvdaxianer.doclens.j.ingestion.application.CreateBatchDependencies;
import io.github.lvdaxianer.doclens.j.ingestion.application.CreateBatchUseCase;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.processing.application.BatchProcessingDependencies;
import io.github.lvdaxianer.doclens.j.processing.application.BatchProcessingUseCase;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import io.github.lvdaxianer.doclens.j.query.application.OcrQueryService;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.shared.config.DocLensProperties;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.JsonCodec;
import io.github.lvdaxianer.doclens.j.shared.config.MybatisPlusConfiguration;
import io.github.lvdaxianer.doclens.j.shared.config.WorkerConfiguration;
import io.github.lvdaxianer.doclens.j.shared.application.SpringTransactionRunner;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;
import java.util.List;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Spring Boot auto-configuration for embedded DocLens runtime.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@AutoConfiguration
@EnableConfigurationProperties(DocLensSpringProperties.class)
@MapperScan({
        "io.github.lvdaxianer.doclens.j.ingestion.infrastructure",
        "io.github.lvdaxianer.doclens.j.processing.infrastructure"
})
@Import({MybatisPlusConfiguration.class, WorkerConfiguration.class})
public class DocLensAutoConfiguration {

    /**
     * Adapts Spring configuration properties to core runtime properties.
     *
     * @param properties Spring-bound properties
     * @return core runtime properties
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Bean
    @ConditionalOnMissingBean
    DocLensProperties docLensProperties(DocLensSpringProperties properties) {
        return new DocLensProperties(properties.storageRoot(), properties.autoProcessOnUpload(), properties.workerId(),
                new DocLensProperties.CallbackProperties(properties.callback().maxRetries(),
                        properties.callback().timeoutSeconds()));
    }

    /**
     * Creates default identifier generator.
     *
     * @return identifier generator
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Bean
    @ConditionalOnMissingBean
    IdGenerator idGenerator() {
        return new IdGenerator();
    }

    /**
     * Creates shared JSON codec.
     *
     * @param objectMapper Jackson object mapper
     * @return JSON codec
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Bean
    @ConditionalOnMissingBean
    JsonCodec jsonCodec(ObjectMapper objectMapper) {
        return new JsonCodec(objectMapper);
    }

    /**
     * Creates Spring transaction adapter.
     *
     * @param transactionTemplate Spring transaction template
     * @return transaction runner
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Bean
    @ConditionalOnMissingBean
    TransactionRunner transactionRunner(TransactionTemplate transactionTemplate) {
        return new SpringTransactionRunner(transactionTemplate);
    }

    /**
     * Creates event factory.
     *
     * @param idGenerator identifier generator
     * @return OCR event factory
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Bean
    @ConditionalOnMissingBean
    OcrEventFactory ocrEventFactory(IdGenerator idGenerator) {
        return new OcrEventFactory(idGenerator);
    }

    /**
     * Creates default event sink.
     *
     * @return no-op event sink
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Bean
    @ConditionalOnMissingBean
    DocLensEventSink docLensEventSink() {
        return new NoopDocLensEventSink();
    }

    /**
     * Creates OCR adapter registry.
     *
     * @param adapters OCR adapters
     * @return adapter registry
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Bean
    @ConditionalOnMissingBean
    DefaultAdapterRegistry defaultAdapterRegistry(List<OcrAdapter> adapters) {
        return new DefaultAdapterRegistry(adapters);
    }

    /**
     * Creates batch processing use case.
     *
     * @param dependencies use case dependencies
     * @param transactionRunner transaction runner
     * @return batch processing use case
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
     * Creates batch processing dependency holder.
     *
     * @param documentRepository document repository
     * @param resultRepository result repository
     * @param eventRepository event repository
     * @param batchRepository batch repository
     * @param adapterRegistry adapter registry
     * @param idGenerator id generator
     * @param eventFactory event factory
     * @return dependency holder
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Bean
    @ConditionalOnMissingBean
    BatchProcessingDependencies batchProcessingDependencies(
            DocumentJobRepository documentRepository,
            OcrResultRepository resultRepository,
            OcrEventRepository eventRepository,
            BatchRepository batchRepository,
            DefaultAdapterRegistry adapterRegistry,
            IdGenerator idGenerator,
            OcrEventFactory eventFactory
    ) {
        return new BatchProcessingDependencies(documentRepository, resultRepository, eventRepository, batchRepository,
                adapterRegistry, idGenerator, eventFactory);
    }

    /**
     * Creates batch creation use case.
     *
     * @param dependencies use case dependencies
     * @param transactionRunner transaction runner
     * @return create batch use case
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Bean
    @ConditionalOnMissingBean
    CreateBatchUseCase createBatchUseCase(CreateBatchDependencies dependencies, TransactionRunner transactionRunner) {
        return new CreateBatchUseCase(dependencies, transactionRunner);
    }

    /**
     * Creates batch creation dependency holder.
     *
     * @param batchRepository batch repository
     * @param documentRepository document repository
     * @param eventRepository event repository
     * @param objectStorage object storage
     * @param idGenerator id generator
     * @param properties runtime properties
     * @param batchProcessingUseCase batch processing use case
     * @param eventFactory event factory
     * @return dependency holder
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Bean
    @ConditionalOnMissingBean
    CreateBatchDependencies createBatchDependencies(
            BatchRepository batchRepository,
            DocumentJobRepository documentRepository,
            OcrEventRepository eventRepository,
            ObjectStorage objectStorage,
            IdGenerator idGenerator,
            DocLensProperties properties,
            BatchProcessingUseCase batchProcessingUseCase,
            OcrEventFactory eventFactory
    ) {
        return new CreateBatchDependencies(batchRepository, documentRepository, eventRepository, objectStorage,
                idGenerator, properties, batchProcessingUseCase, eventFactory);
    }

    /**
     * Creates query service.
     *
     * @param batchRepository batch repository
     * @param documentRepository document repository
     * @param resultRepository result repository
     * @param eventRepository event repository
     * @return OCR query service
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
     * Creates public DocLens engine.
     *
     * @param createBatchUseCase create batch use case
     * @param queryService query service
     * @return DocLens engine
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Bean
    @ConditionalOnMissingBean
    DocLensEngine docLensEngine(CreateBatchUseCase createBatchUseCase, OcrQueryService queryService) {
        return new DefaultDocLensEngine(createBatchUseCase, queryService);
    }
}
