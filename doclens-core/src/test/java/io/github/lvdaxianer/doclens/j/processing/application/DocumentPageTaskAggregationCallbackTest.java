package io.github.lvdaxianer.doclens.j.processing.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus;
import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResult;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTask;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.application.ChunkStrategy;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 文档页任务聚合回调测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-16
 */
class DocumentPageTaskAggregationCallbackTest {

    private static final String TEST_CLIENT_ID = "rag-flow";
    private static final String TEST_SOURCE_APP = "knowledge-base";
    private static final String TEST_TENANT_KEY = "tenant-east";

    /**
     * 页级 OCR 文档完成时应创建完成事件和可投递回调任务。
     *
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    @Test
    void recordSuccessCreatesCompletedEventAndCallbackJobWhenBatchHasCallbackUrl() {
        TestContext context = testContext();
        context.documentRepository.save(document());
        context.batchRepository.save(callbackBatch());
        context.taskRepository.saveAll(List.of(completedTask("task-1", 1)));
        context.pageResultRepository.upsert(pageResult());

        context.service.recordSuccess(completedTask("task-1", 1));

        Map<String, Object> expectedPayload = Map.of(
                "meta", Map.of("source", "page-upload"),
                "text", "page text",
                "idempotency_key", "idem-page-001",
                "caller", expectedCaller());
        assertThat(context.eventRepository.events())
                .filteredOn(event -> DocLensConstants.EVENT_DOCUMENT_COMPLETED.equals(event.eventType()))
                .singleElement()
                .satisfies(event -> assertThat(event.resultSummary()).containsEntry("callback_body",
                        expectedPayload));
        assertThat(context.callbackJobRepository.listByBatchId("batch-1"))
                .singleElement()
                .satisfies(job -> {
                    assertThat(job.status()).isEqualTo(CallbackJobStatus.PENDING);
                    assertThat(job.callbackUrl()).isEqualTo("https://callback.example.test/page");
                    assertThat(job.documentId()).contains("doc-1");
                    assertThat(job.payload()).isEqualTo(expectedPayload);
                });
    }

    /**
     * 创建测试上下文。
     *
     * @return 测试上下文
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    private TestContext testContext() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryDocumentPageTaskRepository taskRepository = new InMemoryDocumentPageTaskRepository();
        DocumentPageTaskExecutionTestDoubles.InMemoryDocumentPageResultRepository pageResultRepository =
                new DocumentPageTaskExecutionTestDoubles.InMemoryDocumentPageResultRepository();
        InMemoryBatchRepository batchRepository = new InMemoryBatchRepository();
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        InMemoryOcrEventRepository eventRepository = new InMemoryOcrEventRepository();
        InMemoryCallbackJobRepository callbackJobRepository = new InMemoryCallbackJobRepository();
        DocumentPageTaskAggregationDependencies dependencies = new DocumentPageTaskAggregationDependencies(
                documentRepository, batchRepository, taskRepository, pageResultRepository, resultRepository,
                new InMemoryObjectStorage(), new IdGenerator(), eventRepository, callbackJobRepository,
                new OcrEventFactory(new IdGenerator()), MarkdownPostProcessor.noop());
        DocumentPageTaskAggregationService service = new DocumentPageTaskAggregationService(dependencies,
                new DocumentPageTaskExecutionTestDoubles.InlineTransactionRunner());
        return new TestContext(documentRepository, batchRepository, taskRepository, pageResultRepository,
                eventRepository, callbackJobRepository, service);
    }

    /**
     * 创建测试文档。
     *
     * @return 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    private DocumentJob document() {
        DocumentJobCreateRequest request = new DocumentJobCreateRequest("doc-1", "batch-1", "demo.png",
                DocumentType.IMAGE, 5, 1, "local://doc-1", "stub_ocr", Optional.empty(),
                new JsonPayload(Map.of("source", "page-upload")), 0, OffsetDateTime.now(), ChunkStrategy.general());
        return DocumentJob.create(request).markOcrQueued(1, OffsetDateTime.now());
    }

    /**
     * 创建带回调地址的批次。
     *
     * @return 测试批次
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    private Batch callbackBatch() {
        return new Batch("batch-1", BatchStatus.PROCESSING, 1, 0, 0, Optional.empty(), Optional.empty(),
                "ocr_queued", new JsonPayload(Map.of("source", "page-upload")),
                Optional.of("https://callback.example.test/page"), Optional.of("idem-page-001"),
                OffsetDateTime.now(), OffsetDateTime.now(), new CallerIdentity(TEST_CLIENT_ID, TEST_SOURCE_APP,
                Optional.of(TEST_TENANT_KEY)));
    }

    /**
     * 创建预期回调调用方载荷。
     *
     * @return 调用方载荷
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private Map<String, Object> expectedCaller() {
        return Map.of(
                CallerIdentity.CLIENT_ID_FIELD, TEST_CLIENT_ID,
                CallerIdentity.SOURCE_APP_FIELD, TEST_SOURCE_APP,
                CallerIdentity.TENANT_KEY_FIELD, TEST_TENANT_KEY);
    }

    /**
     * 创建已完成页任务。
     *
     * @param taskId 页任务 ID
     * @param pageNo 页码
     * @return 页任务
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    private DocumentPageTask completedTask(String taskId, int pageNo) {
        DocumentPageTaskCreateRequest request = new DocumentPageTaskCreateRequest(taskId, "batch-1", "doc-1",
                pageNo, "local://doc-1/page-" + pageNo, OffsetDateTime.now());
        return DocumentPageTask.create(request).markProcessing("worker-test", OffsetDateTime.now())
                .markCompleted("worker-test", OffsetDateTime.now());
    }

    /**
     * 创建页 OCR 结果。
     *
     * @return 页 OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    private DocumentPageResult pageResult() {
        return new DocumentPageResult("doc-1", 1, Map.of("pageNo", 1), "page text",
                List.of(Map.of("pageNo", 1, "text", "page text")), 0.99D, List.of(), "node-test",
                OffsetDateTime.now(), OffsetDateTime.now());
    }

    /**
     * 内存对象存储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    private static class InMemoryObjectStorage implements io.github.lvdaxianer.doclens.j.storage.ObjectStorage {

        /**
         * 返回对象键作为存储地址。
         *
         * @param objectKey 对象键
         * @param content 待存储字节
         * @return 存储地址
         * @author lvdaxianerplus
         * @date 2026-06-16
         */
        @Override
        public String writeBytes(String objectKey, byte[] content) {
            return objectKey;
        }

        /**
         * 返回空内容。
         *
         * @param storageUri 存储地址
         * @return 空字节数组
         * @author lvdaxianerplus
         * @date 2026-06-16
         */
        @Override
        public byte[] readBytes(String storageUri) {
            return "".getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * 聚合回调测试上下文。
     *
     * @param documentRepository 文档仓储
     * @param batchRepository 批次仓储
     * @param taskRepository 页任务仓储
     * @param pageResultRepository 页结果仓储
     * @param eventRepository 事件仓储
     * @param callbackJobRepository 回调任务仓储
     * @param service 聚合服务
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    private record TestContext(
            InMemoryDocumentJobRepository documentRepository,
            InMemoryBatchRepository batchRepository,
            InMemoryDocumentPageTaskRepository taskRepository,
            DocumentPageTaskExecutionTestDoubles.InMemoryDocumentPageResultRepository pageResultRepository,
            InMemoryOcrEventRepository eventRepository,
            InMemoryCallbackJobRepository callbackJobRepository,
            DocumentPageTaskAggregationService service
    ) {
    }
}
