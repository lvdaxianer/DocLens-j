package io.github.lvdaxianer.doclens.j.processing.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResult;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTask;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskCompletionRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResult;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.ProcessingStage;
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
     * 创建测试上下文。
     *
     * @return 测试上下文
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private TestContext testContext() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryDocumentPageTaskRepository taskRepository = new InMemoryDocumentPageTaskRepository();
        DocumentPageTaskExecutionTestDoubles.InMemoryDocumentPageResultRepository pageResultRepository =
                new DocumentPageTaskExecutionTestDoubles.InMemoryDocumentPageResultRepository();
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        InMemoryObjectStorage objectStorage = new InMemoryObjectStorage();
        DocumentPageTaskAggregationDependencies dependencies = new DocumentPageTaskAggregationDependencies(
                documentRepository, taskRepository, pageResultRepository, resultRepository, objectStorage,
                new IdGenerator());
        DocumentPageTaskAggregationService service = new DocumentPageTaskAggregationService(dependencies,
                new DocumentPageTaskExecutionTestDoubles.InlineTransactionRunner());
        return new TestContext(documentRepository, taskRepository, pageResultRepository, resultRepository, service);
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
                JsonPayload.empty(), 0, OffsetDateTime.now());
        return DocumentJob.create(request).markOcrQueued(TWO_PAGES, OffsetDateTime.now());
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
            InMemoryDocumentPageTaskRepository taskRepository,
            DocumentPageTaskExecutionTestDoubles.InMemoryDocumentPageResultRepository pageResultRepository,
            InMemoryOcrResultRepository resultRepository,
            DocumentPageTaskAggregationService service
    ) {
    }
}
