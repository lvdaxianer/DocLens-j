package io.github.lvdaxianer.doclens.j.processing.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResult;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTask;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskCompletionRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.processing.domain.EmptyCallbackJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResult;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.ProcessingStage;
import io.github.lvdaxianer.doclens.j.processing.application.ChunkStrategy;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 文档页任务聚合服务测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
class DocumentPageTaskAggregationServiceTest {

    private static final int TWO_PAGES = 2;
    private static final int TWO_DOCUMENTS = 2;

    /**
     * 聚合服务应在全部页完成后按页码顺序生成最终 OCR 结果。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void recordSuccessFinalizesDocumentOnlyAfterAllPagesArriveAndSortsByPageNo() {
        TestContext context = testContext();
        context.documentRepository.save(document());
        context.taskRepository.saveAll(List.of(processingTask("task-1", 1), completedTask("task-2", 2)));
        context.pageResultRepository.upsert(pageResult(2, "second"));
        context.pageResultRepository.upsert(pageResult(1, "first"));

        context.service.recordSuccess(completedTask("task-2", 2));
        assertThat(context.resultRepository.findByDocumentId("doc-1")).isEmpty();
        context.taskRepository.markCompleted(new DocumentPageTaskCompletionRequest("task-1", "worker-test",
                OffsetDateTime.now()));
        context.service.recordSuccess(completedTask("task-1", 1));

        OcrResult result = context.resultRepository.findByDocumentId("doc-1").orElseThrow();
        assertThat(result.pageText()).extracting(page -> page.get("pageNo")).containsExactly(1, 2);
        assertThat(result.finalText()).isEqualTo("first\n\nsecond");
        assertThat(context.documentRepository.findById("doc-1").orElseThrow().stage())
                .isEqualTo(ProcessingStage.COMPLETED);
    }

    /**
     * 聚合服务重复收到成功回调时不应重复生成 OCR 结果。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void recordSuccessSkipsAlreadyCompletedDocument() {
        TestContext context = testContext();
        context.documentRepository.save(document());
        context.taskRepository.saveAll(List.of(completedTask("task-1", 1), completedTask("task-2", 2)));
        context.pageResultRepository.upsert(pageResult(1, "first"));
        context.pageResultRepository.upsert(pageResult(2, "second"));

        context.service.recordSuccess(completedTask("task-2", 2));
        context.service.recordSuccess(completedTask("task-2", 2));

        assertThat(context.resultRepository.saveCount()).isOne();
        assertThat(context.documentRepository.findById("doc-1").orElseThrow().stage())
                .isEqualTo(ProcessingStage.COMPLETED);
    }

    /**
     * 聚合服务完成图片文档后应同步刷新批次摘要，避免公开批次 API 仍停留在旧进度。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void recordSuccessRefreshesBatchSummaryAfterDocumentCompletion() {
        TestContext context = testContext();
        context.batchRepository.save(batchWithOneCompletedDocument());
        context.documentRepository.save(completedMarkdownDocument());
        context.documentRepository.save(document());
        context.taskRepository.saveAll(List.of(completedTask("task-1", 1), completedTask("task-2", 2)));
        context.pageResultRepository.upsert(pageResult(1, "first"));
        context.pageResultRepository.upsert(pageResult(2, "second"));

        context.service.recordSuccess(completedTask("task-2", 2));

        Batch batch = context.batchRepository.findById("batch-1").orElseThrow();
        assertThat(batch.completedFiles()).isEqualTo(TWO_DOCUMENTS);
        assertThat(batch.failedFiles()).isZero();
        assertThat(batch.status()).isEqualTo(BatchStatus.COMPLETED);
    }

    /**
     * 创建测试上下文。
     *
     * @return 测试上下文
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private TestContext testContext() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryDocumentPageTaskRepository taskRepository = new InMemoryDocumentPageTaskRepository();
        InMemoryBatchRepository batchRepository = new InMemoryBatchRepository();
        DocumentPageTaskExecutionTestDoubles.InMemoryDocumentPageResultRepository pageResultRepository =
                new DocumentPageTaskExecutionTestDoubles.InMemoryDocumentPageResultRepository();
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        InMemoryOcrEventRepository eventRepository = new InMemoryOcrEventRepository();
        InMemoryObjectStorage objectStorage = new InMemoryObjectStorage();
        DocumentPageTaskAggregationDependencies dependencies = new DocumentPageTaskAggregationDependencies(
                documentRepository, batchRepository, taskRepository, pageResultRepository, resultRepository,
                objectStorage, new IdGenerator(), eventRepository, new EmptyCallbackJobRepository(),
                new OcrEventFactory(new IdGenerator()));
        DocumentPageTaskAggregationService service = new DocumentPageTaskAggregationService(dependencies,
                new DocumentPageTaskExecutionTestDoubles.InlineTransactionRunner());
        return new TestContext(documentRepository, batchRepository, taskRepository, pageResultRepository,
                resultRepository, service);
    }

    /**
     * 创建测试文档。
     *
     * @return 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentJob document() {
        DocumentJobCreateRequest request = new DocumentJobCreateRequest("doc-1", "batch-1", "demo.png",
                DocumentType.IMAGE, 5, TWO_PAGES, "local://doc-1", "stub_ocr", Optional.empty(),
                JsonPayload.empty(), 0, OffsetDateTime.now(), ChunkStrategy.general());
        return DocumentJob.create(request).markOcrQueued(TWO_PAGES, OffsetDateTime.now());
    }

    /**
     * 创建已完成的 Markdown 文档，用于模拟同批次内已有同步文档完成。
     *
     * @return 已完成 Markdown 文档
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentJob completedMarkdownDocument() {
        DocumentJobCreateRequest request = new DocumentJobCreateRequest("doc-2", "batch-1", "demo.md",
                DocumentType.MARKDOWN, 5, 1, "local://doc-2", "stub_ocr", Optional.empty(),
                JsonPayload.empty(), 1, OffsetDateTime.now(), ChunkStrategy.general());
        return DocumentJob.create(request).startProcessing(OffsetDateTime.now())
                .complete("result-doc-2", OffsetDateTime.now());
    }

    /**
     * 创建摘要里已有一个完成文档的批次。
     *
     * @return 测试批次
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private Batch batchWithOneCompletedDocument() {
        OffsetDateTime now = OffsetDateTime.now();
        return new Batch("batch-1", BatchStatus.PROCESSING, TWO_DOCUMENTS, 1, 0, Optional.empty(),
                Optional.empty(), "ocr_queued", JsonPayload.empty(), Optional.empty(), Optional.empty(), now, now);
    }

    /**
     * 创建已完成页任务。
     *
     * @param taskId 页任务 ID
     * @param pageNo 页码
     * @return 页任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentPageTask completedTask(String taskId, int pageNo) {
        return processingTask(taskId, pageNo).markCompleted("worker-test", OffsetDateTime.now());
    }

    /**
     * 创建处理中的页任务。
     *
     * @param taskId 页任务 ID
     * @param pageNo 页码
     * @return 页任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentPageTask processingTask(String taskId, int pageNo) {
        DocumentPageTaskCreateRequest request = new DocumentPageTaskCreateRequest(taskId, "batch-1", "doc-1",
                pageNo, "local://doc-1/page-" + pageNo, OffsetDateTime.now());
        return DocumentPageTask.create(request).markProcessing("worker-test", OffsetDateTime.now());
    }

    /**
     * 创建页 OCR 结果。
     *
     * @param pageNo 页码
     * @param text 页文本
     * @return 页 OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentPageResult pageResult(int pageNo, String text) {
        return new DocumentPageResult("doc-1", pageNo, Map.of("pageNo", pageNo), text,
                List.of(Map.of("pageNo", pageNo, "text", text)), 0.99D, List.of(), "node-test",
                OffsetDateTime.now(), OffsetDateTime.now());
    }

    /**
     * 内存 OCR 结果仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static class InMemoryOcrResultRepository implements OcrResultRepository {

        private final Map<String, OcrResult> results = new HashMap<>(1);
        private int saveCount;

        /**
         * 保存 OCR 结果。
         *
         * @param result OCR 结果
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public void save(OcrResult result) {
            saveCount++;
            results.put(result.documentId(), result);
        }

        /**
         * 批量保存 OCR 结果。
         *
         * @param results OCR 结果集合
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public void saveAll(List<OcrResult> results) {
            results.forEach(this::save);
        }

        /**
         * 根据文档 ID 查询 OCR 结果。
         *
         * @param documentId 文档 ID
         * @return 可选 OCR 结果
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public Optional<OcrResult> findByDocumentId(String documentId) {
            return Optional.ofNullable(results.get(documentId));
        }

        /**
         * 返回保存次数。
         *
         * @return 保存次数
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        int saveCount() {
            return saveCount;
        }
    }

    /**
     * 内存对象存储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static class InMemoryObjectStorage implements ObjectStorage {

        /**
         * 返回对象键作为存储地址。
         *
         * @param objectKey 对象键
         * @param content 待存储字节
         * @return 存储地址
         * @author lvdaxianerplus
         * @date 2026-06-11
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
         * @date 2026-06-11
         */
        @Override
        public byte[] readBytes(String storageUri) {
            return "".getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * 聚合测试上下文。
     *
     * @param documentRepository 文档仓储
     * @param taskRepository 页任务仓储
     * @param pageResultRepository 页结果仓储
     * @param resultRepository OCR 结果仓储
     * @param service 聚合服务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private record TestContext(
            InMemoryDocumentJobRepository documentRepository,
            InMemoryBatchRepository batchRepository,
            InMemoryDocumentPageTaskRepository taskRepository,
            DocumentPageTaskExecutionTestDoubles.InMemoryDocumentPageResultRepository pageResultRepository,
            InMemoryOcrResultRepository resultRepository,
            DocumentPageTaskAggregationService service
    ) {
    }

    /**
     * 内存批次仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static class InMemoryBatchRepository implements BatchRepository {

        private final Map<String, Batch> batches = new HashMap<>(1);

        /**
         * 保存批次。
         *
         * @param batch 批次
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public void save(Batch batch) {
            batches.put(batch.batchId(), batch);
        }

        /**
         * 按 ID 查询批次。
         *
         * @param batchId 批次 ID
         * @return 可选批次
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public Optional<Batch> findById(String batchId) {
            return Optional.ofNullable(batches.get(batchId));
        }

        /**
         * 按幂等键查询批次。
         *
         * @param idempotencyKey 幂等键
         * @return 空批次
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public Optional<Batch> findByIdempotencyKey(String idempotencyKey) {
            return Optional.empty();
        }

        /**
         * 查询最近批次。
         *
         * @param limit 最大数量
         * @return 批次集合
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public List<Batch> listRecent(int limit) {
            return batches.values().stream().limit(limit).toList();
        }

        /**
         * 更新批次摘要。
         *
         * @param batchId 批次 ID
         * @param completedFiles 已完成文件数
         * @param failedFiles 失败文件数
         * @param status 批次状态
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public void updateSummary(String batchId, int completedFiles, int failedFiles, BatchStatus status) {
            Batch batch = batches.get(batchId);
            batches.put(batchId, new Batch(batch.batchId(), status, batch.totalFiles(), completedFiles,
                    failedFiles, batch.currentDocumentId(), batch.currentDocumentName(), status.name().toLowerCase(),
                    batch.metadata(), batch.callbackUrl(), batch.idempotencyKey(), batch.createdAt(),
                    OffsetDateTime.now()));
        }
    }
}
