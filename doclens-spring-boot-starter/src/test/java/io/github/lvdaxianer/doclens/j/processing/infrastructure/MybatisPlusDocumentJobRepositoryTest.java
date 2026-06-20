package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensAutoConfiguration;
import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensCallbackDeliveryAutoConfiguration;
import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensDashboardMetricsAutoConfiguration;
import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensDocumentLifecycleAutoConfiguration;
import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensExtractionAutoConfiguration;
import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensLlmMarkdownAutoConfiguration;
import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensOcrNodeManagementAutoConfiguration;
import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensOcrResourceAutoConfiguration;
import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensOcrThreadPoolAutoConfiguration;
import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensPaddleOcrAutoConfiguration;
import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensPageTaskWorkerAutoConfiguration;
import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensProcessingAutoConfiguration;
import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensStaleDocumentRecoveryAutoConfiguration;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.JsonCodec;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 文档任务 MyBatis-Plus 仓储集成测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
@SpringBootTest(classes = MybatisPlusDocumentJobRepositoryTest.TestApplication.class)
class MybatisPlusDocumentJobRepositoryTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-20T10:00:00+08:00");
    private static final int FILE_SIZE = 8;
    private static final int PAGE_COUNT = 1;
    private static final int QUERY_LIMIT = 2;

    @Autowired
    private DocumentJobRepository repository;

    /**
     * 配置 H2 与 Flyway 测试数据库。
     *
     * @param registry 动态属性注册表
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:document_job_repo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
    }

    /**
     * 启动恢复查询应按去重批次限流，而不是先按文档行限流。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void listQueuedBatchIdsReturnsDistinctQueuedBatchesWithinLimit() {
        repository.saveAll(List.of(
                queuedDocument("doc-a-1", "batch-a", 0),
                queuedDocument("doc-a-2", "batch-a", 1),
                queuedDocument("doc-b-1", "batch-b", 2),
                queuedDocument("doc-c-1", "batch-c", 3),
                terminalDocument("doc-d-1", "batch-d", 4),
                stalledDocument("doc-e-1", "batch-e", 5)
        ));

        assertThat(repository.listQueuedBatchIds(QUERY_LIMIT))
                .containsExactly("batch-a", "batch-b");
    }

    /**
     * 创建等待处理文档任务。
     *
     * @param documentId 文档 ID
     * @param batchId 批次 ID
     * @param sortOrder 上传顺序
     * @return 等待处理文档任务
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private DocumentJob queuedDocument(String documentId, String batchId, int sortOrder) {
        return DocumentJob.create(new DocumentJobCreateRequest(documentId, batchId, documentId + ".txt",
                DocumentType.TEXT, FILE_SIZE, PAGE_COUNT, "local://" + documentId, "stub_ocr", Optional.empty(),
                JsonPayload.empty(), sortOrder, BASE_TIME.plusSeconds(sortOrder)));
    }

    /**
     * 创建终态完成文档任务。
     *
     * @param documentId 文档 ID
     * @param batchId 批次 ID
     * @param sortOrder 上传顺序
     * @return 终态完成文档任务
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private DocumentJob terminalDocument(String documentId, String batchId, int sortOrder) {
        return queuedDocument(documentId, batchId, sortOrder)
                .complete("result-" + documentId, BASE_TIME.plusSeconds(sortOrder));
    }

    /**
     * 创建卡死文档任务。
     *
     * @param documentId 文档 ID
     * @param batchId 批次 ID
     * @param sortOrder 上传顺序
     * @return 卡死文档任务
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private DocumentJob stalledDocument(String documentId, String batchId, int sortOrder) {
        return queuedDocument(documentId, batchId, sortOrder)
                .stall("STALE", "stalled", BASE_TIME.plusSeconds(sortOrder));
    }

    /**
     * 文档任务仓储测试应用。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DocLensAutoConfiguration.class,
            DocLensOcrThreadPoolAutoConfiguration.class,
            DocLensOcrNodeManagementAutoConfiguration.class,
            DocLensDashboardMetricsAutoConfiguration.class,
            DocLensPaddleOcrAutoConfiguration.class,
            DocLensOcrResourceAutoConfiguration.class,
            DocLensExtractionAutoConfiguration.class,
            DocLensLlmMarkdownAutoConfiguration.class,
            DocLensProcessingAutoConfiguration.class,
            DocLensDocumentLifecycleAutoConfiguration.class,
            DocLensStaleDocumentRecoveryAutoConfiguration.class,
            DocLensPageTaskWorkerAutoConfiguration.class,
            DocLensCallbackDeliveryAutoConfiguration.class
    })
    @MapperScan(basePackageClasses = DocumentJobMapper.class, annotationClass = Mapper.class)
    @Import({
            io.github.lvdaxianer.doclens.j.shared.config.MybatisPlusConfiguration.class,
            MybatisPlusDocumentJobRepository.class
    })
    static class TestApplication {

        /**
         * 测试应用只装配文档任务 Mapper 和仓储，避免启动完整 OCR 处理链路。
         *
         * @return JSON 编解码器
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Bean
        JsonCodec jsonCodec() {
            return new JsonCodec(new ObjectMapper());
        }
    }
}
