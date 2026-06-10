package io.github.lvdaxianer.doclens.j.query.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResult;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * OCR 查询服务测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
class OcrQueryServiceTest {

    /**
     * 文档结果查询应返回 LLM 失败原因，便于前端判断是密钥、超时还是供应商错误。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void getDocumentResultIncludesLlmErrorMessageWhenPresent() {
        OcrResult result = new OcrResult(
                "result-1",
                "doc-1",
                "原始 OCR 文本",
                "local://results/doc-1.md",
                Map.of("llm_markdown_applied", false, "llm_error_message", "LLM Markdown returned HTTP 401"),
                Map.of("pages", List.of()),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                0.91D,
                List.of("llm_markdown_post_processing_failed"),
                OffsetDateTime.now());
        OcrQueryService service = new OcrQueryService(new NoopBatchRepository(), new NoopDocumentJobRepository(),
                new FixedOcrResultRepository(result), new NoopOcrEventRepository());

        Map<String, Object> payload = service.getDocumentResult("doc-1");

        assertThat(payload)
                .extractingByKey("result")
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("llm_error_message", "LLM Markdown returned HTTP 401");
    }

    /**
     * 固定返回单个 OCR 结果的仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private record FixedOcrResultRepository(OcrResult result) implements OcrResultRepository {

        @Override
        public void save(OcrResult result) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveAll(List<OcrResult> results) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<OcrResult> findByDocumentId(String documentId) {
            if (result.documentId().equals(documentId)) {
                return Optional.of(result);
            } else {
                return Optional.empty();
            }
        }
    }

    /**
     * 不参与当前测试的批次仓储桩。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static class NoopBatchRepository implements BatchRepository {

        @Override
        public void save(Batch batch) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Batch> findById(String batchId) {
            return Optional.empty();
        }

        @Override
        public Optional<Batch> findByIdempotencyKey(String idempotencyKey) {
            return Optional.empty();
        }

        @Override
        public List<Batch> listRecent(int limit) {
            return List.of();
        }

        @Override
        public void updateSummary(String batchId, int completedFiles, int failedFiles, BatchStatus status) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * 不参与当前测试的文档仓储桩。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static class NoopDocumentJobRepository implements DocumentJobRepository {

        @Override
        public void save(DocumentJob document) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveAll(List<DocumentJob> documents) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void update(DocumentJob document) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateAll(List<DocumentJob> documents) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<DocumentJob> findById(String documentId) {
            return Optional.empty();
        }

        @Override
        public List<DocumentJob> listByBatchId(String batchId) {
            return List.of();
        }

        @Override
        public List<DocumentJob> listByBatchIds(List<String> batchIds) {
            return List.of();
        }

        @Override
        public List<DocumentJob> listRecent(int limit) {
            return List.of();
        }
    }

    /**
     * 不参与当前测试的事件仓储桩。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static class NoopOcrEventRepository implements OcrEventRepository {

        @Override
        public void save(OcrEvent event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveAll(List<OcrEvent> events) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<OcrEvent> listByBatchId(String batchId) {
            return List.of();
        }

        @Override
        public List<OcrEvent> listRecent(int limit) {
            return List.of();
        }
    }
}
