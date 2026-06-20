package io.github.lvdaxianer.doclens.j.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.BASE_TIME;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.batch;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.completedDocument;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceOcrResultFixtures.fixedOcrResultRepository;

import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus;
import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.InMemoryBatchRepository;
import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.InMemoryDocumentJobRepository;
import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.InMemoryOcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.EmptyCallbackJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.EmptyOcrResultRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResult;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Dashboard 批次接入信息测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-19
 */
class DashboardQueryServiceBatchIntakeInfoTest {

    private static final String TEST_CALLBACK_URL = "https://client.example.com/ocr-callback";
    private static final String TEST_IDEMPOTENCY_KEY = "openwebui:file:file-123:hash:abc";
    private static final String TEST_METADATA_FILE_ID = "file-123";
    private static final String TEST_METADATA_SOURCE = "open-webui";
    private static final String TEST_CLIENT_ID = "openwebui-prod";
    private static final String TEST_SOURCE_APP = "open-webui";
    private static final String TEST_TENANT_KEY = "knowledge-team";

    /**
     * 批次详情应暴露创建时传入的接入信息。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Test
    void batchDetailExposesBatchIntakeInfo() {
        DashboardQueryService service = serviceWithBatch(batchWithIntakeInfo());

        Map<?, ?> batch = batchOf(service.batchDetail("batch-test"));
        Map<?, ?> metadata = (Map<?, ?>) batch.get("metadata");

        assertThat(batch.get("callback_url")).isEqualTo(TEST_CALLBACK_URL);
        assertThat(batch.get("idempotency_key")).isEqualTo(TEST_IDEMPOTENCY_KEY);
        assertThat(metadata.get("source")).isEqualTo(TEST_METADATA_SOURCE);
        assertThat(metadata.get("openwebui_file_id")).isEqualTo(TEST_METADATA_FILE_ID);
    }

    /**
     * 批次详情应暴露可信调用方身份，便于按接入系统排查。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Test
    void batchDetailExposesCallerIdentity() {
        DashboardQueryService service = serviceWithBatch(batchWithIntakeInfo());

        Map<?, ?> batch = batchOf(service.batchDetail("batch-test"));

        assertThat(batch.get("client_id")).isEqualTo(TEST_CLIENT_ID);
        assertThat(batch.get("source_app")).isEqualTo(TEST_SOURCE_APP);
        assertThat(batch.get("tenant_key")).isEqualTo(TEST_TENANT_KEY);
    }

    /**
     * 创建包含指定批次的批次详情查询服务。
     *
     * @param batchEntry 测试批次
     * @return 批次详情查询服务
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private DashboardQueryService serviceWithBatch(Batch batchEntry) {
        return new DashboardQueryService(new DashboardQueryService.Dependencies(
                new DashboardQueryService.Dependencies.Repositories(new InMemoryBatchRepository(List.of(batchEntry)),
                        new InMemoryDocumentJobRepository(List.of(completedDocument("doc-text", DocumentType.TEXT, 0))),
                        new InMemoryOcrEventRepository(), fixedOcrResultRepository(List.of(new OcrResult(
                        "result-doc-markdown", "doc-markdown", "markdown", "local://results/doc-markdown.md",
                        Map.of("llm_chunk_count", 4), Map.of(), List.of(), List.of(), List.of(), List.of(), 1.0D,
                        List.of(), BASE_TIME)))),
                new DashboardQueryService.Dependencies.Services(new EmptyDashboardOcrMetricsProvider(),
                        new EmptyCallbackJobRepository())));
    }

    /**
     * 创建带接入信息的测试批次。
     *
     * @return 带接入信息的测试批次
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private Batch batchWithIntakeInfo() {
        Map<String, Object> metadata = Map.of("source", TEST_METADATA_SOURCE, "openwebui_file_id",
                TEST_METADATA_FILE_ID);
        return new Batch("batch-test", BatchStatus.COMPLETED, 1, 1, 0, Optional.empty(), Optional.empty(),
                "completed", new JsonPayload(metadata), Optional.of(TEST_CALLBACK_URL),
                Optional.of(TEST_IDEMPOTENCY_KEY), BASE_TIME, BASE_TIME.plusMinutes(5),
                new CallerIdentity(TEST_CLIENT_ID, TEST_SOURCE_APP, Optional.of(TEST_TENANT_KEY)));
    }

    /**
     * 从详情结果中取出批次对象。
     *
     * @param detail 批次详情结果
     * @return 批次对象
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private Map<?, ?> batchOf(Map<String, Object> detail) {
        return (Map<?, ?>) detail.get("batch");
    }
}
