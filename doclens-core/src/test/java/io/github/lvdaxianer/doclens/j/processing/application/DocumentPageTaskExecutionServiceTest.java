package io.github.lvdaxianer.doclens.j.processing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrRouteExecutionResult;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingService;
import io.github.lvdaxianer.doclens.j.adapter.application.InMemoryOcrRunningPageTaskTracker;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRunningPageTask;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRunningPageTaskAssignment;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRunningPageTaskCommand;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRunningPageTaskTracker;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrBlock;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTask;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/**
 * 文档页任务执行服务测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
class DocumentPageTaskExecutionServiceTest {

    /**
     * 执行器应跨文档消费等待页任务，并保持文档 ID 与页码不串。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void runOnceConsumesQueuedPagesAcrossDocumentsAndPreservesTaskIdentity() {
        InMemoryDocumentPageTaskRepository taskRepository = new InMemoryDocumentPageTaskRepository();
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        DocumentPageTaskExecutionTestDoubles.InMemoryDocumentPageResultRepository resultRepository =
                new DocumentPageTaskExecutionTestDoubles.InMemoryDocumentPageResultRepository();
        DocumentPageTaskExecutionTestDoubles.RecordingObjectStorage objectStorage =
                new DocumentPageTaskExecutionTestDoubles.RecordingObjectStorage();
        RecordingOcrRoutingService routingService = new RecordingOcrRoutingService();
        seedDocuments(documentRepository);
        taskRepository.saveAll(List.of(task("task-1", "doc-1", 1), task("task-2", "doc-2", 1),
                task("task-3", "doc-1", 2)));
        DocumentPageTaskExecutionService service = service(taskRepository, documentRepository, resultRepository,
                objectStorage, routingService);

        service.runOnce();

        assertThat(routingService.requests)
                .extracting(ImageOcrRequest::documentId, ImageOcrRequest::pageNo)
                .containsExactly(tuple("doc-1", 1), tuple("doc-2", 1), tuple("doc-1", 2));
        assertThat(resultRepository.listByDocumentId("doc-1")).hasSize(2);
        assertThat(taskRepository.listByDocumentId("doc-1"))
                .extracting(DocumentPageTask::status)
                .containsOnly(DocumentPageTaskStatus.COMPLETED);
    }

    /**
     * 执行器应只抢占并投递页任务，避免调度线程被 OCR 调用阻塞。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void runOnceSubmitsClaimedPagesWithoutRunningOcrInline() {
        InMemoryDocumentPageTaskRepository taskRepository = new InMemoryDocumentPageTaskRepository();
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        ManualExecutor executor = new ManualExecutor();
        seedDocuments(documentRepository);
        taskRepository.saveAll(List.of(task("task-1", "doc-1", 1)));
        DocumentPageTaskExecutionService service = service(new DocumentPageTaskExecutionDependencies(taskRepository,
                documentRepository, new DocumentPageTaskExecutionTestDoubles.InMemoryDocumentPageResultRepository(),
                new DocumentPageTaskExecutionTestDoubles.RecordingObjectStorage(), new RecordingOcrRoutingService()),
                executor);

        int submittedCount = service.runOnce();

        assertThat(submittedCount).isEqualTo(1);
        assertThat(executor.pendingTasks).hasSize(1);
        assertThat(taskRepository.listByDocumentId("doc-1"))
                .extracting(DocumentPageTask::status)
                .containsExactly(DocumentPageTaskStatus.PROCESSING);
    }

    /**
     * 聚合监听失败不应把已成功 OCR 的页任务回写为失败。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void runOnceKeepsPageCompletedWhenSuccessListenerFails() {
        InMemoryDocumentPageTaskRepository taskRepository = new InMemoryDocumentPageTaskRepository();
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        DocumentPageTaskExecutionTestDoubles.InMemoryDocumentPageResultRepository resultRepository =
                new DocumentPageTaskExecutionTestDoubles.InMemoryDocumentPageResultRepository();
        seedDocuments(documentRepository);
        taskRepository.saveAll(List.of(task("task-1", "doc-1", 1)));
        DocumentPageTaskExecutionService service = service(new DocumentPageTaskExecutionDependencies(taskRepository,
                documentRepository, resultRepository, new DocumentPageTaskExecutionTestDoubles.RecordingObjectStorage(),
                new RecordingOcrRoutingService()), new DirectExecutor(), task -> {
                    throw new IllegalStateException("aggregation failed");
                });

        int submittedCount = service.runOnce();

        assertThat(submittedCount).isEqualTo(1);
        assertThat(resultRepository.findByDocumentIdAndPageNo("doc-1", 1)).isPresent();
        assertThat(taskRepository.listByDocumentId("doc-1"))
                .extracting(DocumentPageTask::status)
                .containsExactly(DocumentPageTaskStatus.COMPLETED);
    }

    /**
     * 执行器应在 OCR 调用期间暴露页码和线程名，并在调用结束后清理运行中行。
     *
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Test
    void runOncePublishesRunningPageTaskDuringOcrAndClearsItAfterCompletion() {
        InMemoryDocumentPageTaskRepository taskRepository = new InMemoryDocumentPageTaskRepository();
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        DocumentPageTaskExecutionTestDoubles.InMemoryDocumentPageResultRepository resultRepository =
                new DocumentPageTaskExecutionTestDoubles.InMemoryDocumentPageResultRepository();
        InMemoryOcrRunningPageTaskTracker runningTracker = new InMemoryOcrRunningPageTaskTracker();
        InspectingOcrRoutingService routingService = new InspectingOcrRoutingService(runningTracker);
        seedDocuments(documentRepository);
        taskRepository.saveAll(List.of(task("task-1", "doc-1", 1)));
        DocumentPageTaskExecutionService service = service(new DocumentPageTaskExecutionDependencies(taskRepository,
                documentRepository, resultRepository, new DocumentPageTaskExecutionTestDoubles.RecordingObjectStorage(),
                routingService, runningTracker), new DirectExecutor());

        service.runOnce();

        assertThat(routingService.sawRunningPage).isTrue();
        assertThat(runningTracker.snapshotByBatch("batch-test")).isEmpty();
    }

    /**
     * 运行态追踪失败不应阻断页任务 OCR 成功处理。
     *
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Test
    void runOnceCompletesPageWhenRunningTrackerFails() {
        InMemoryDocumentPageTaskRepository taskRepository = new InMemoryDocumentPageTaskRepository();
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        DocumentPageTaskExecutionTestDoubles.InMemoryDocumentPageResultRepository resultRepository =
                new DocumentPageTaskExecutionTestDoubles.InMemoryDocumentPageResultRepository();
        seedDocuments(documentRepository);
        taskRepository.saveAll(List.of(task("task-1", "doc-1", 1)));
        DocumentPageTaskExecutionService service = service(new DocumentPageTaskExecutionDependencies(taskRepository,
                documentRepository, resultRepository, new DocumentPageTaskExecutionTestDoubles.RecordingObjectStorage(),
                new RecordingOcrRoutingService(), new FailingRunningPageTaskTracker()), new DirectExecutor());

        service.runOnce();

        assertThat(resultRepository.findByDocumentIdAndPageNo("doc-1", 1)).isPresent();
        assertThat(taskRepository.listByDocumentId("doc-1"))
                .extracting(DocumentPageTask::status)
                .containsExactly(DocumentPageTaskStatus.COMPLETED);
    }

    /**
     * 创建页任务执行服务。
     *
     * @param taskRepository 页任务仓储
     * @param documentRepository 文档仓储
     * @param resultRepository 页结果仓储
     * @param objectStorage 对象存储
     * @param routingService OCR 路由服务
     * @return 页任务执行服务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentPageTaskExecutionService service(
            InMemoryDocumentPageTaskRepository taskRepository,
            InMemoryDocumentJobRepository documentRepository,
            DocumentPageTaskExecutionTestDoubles.InMemoryDocumentPageResultRepository resultRepository,
            ObjectStorage objectStorage,
            OcrRoutingService routingService
    ) {
        return new DocumentPageTaskExecutionService(new DocumentPageTaskExecutionDependencies(taskRepository,
                documentRepository, resultRepository, objectStorage, routingService),
                new DocumentPageTaskExecutionTestDoubles.InlineTransactionRunner(),
                options(new DirectExecutor(), 4));
    }

    /**
     * 创建页任务执行配置。
     *
     * @param executor 页任务执行器
     * @param batchSize 批量大小
     * @return 页任务执行配置
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentPageTaskExecutionOptions options(Executor executor, int batchSize) {
        return new DocumentPageTaskExecutionOptions("worker-test", batchSize, 30, executor, task -> {
            // 当前执行器单元测试只验证页任务执行，聚合监听由独立测试覆盖。
        });
    }

    /**
     * 创建可指定执行器的页任务执行服务。
     *
     * @param context 测试服务上下文
     * @param executor 页任务执行器
     * @return 页任务执行服务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentPageTaskExecutionService service(DocumentPageTaskExecutionDependencies dependencies,
                                                     Executor executor) {
        return service(dependencies, executor, task -> {
            // 当前执行器单元测试只验证页任务执行，聚合监听由独立测试覆盖。
        });
    }

    /**
     * 创建可指定监听器的页任务执行服务。
     *
     * @param dependencies 页任务执行依赖
     * @param executor 页任务执行器
     * @param pageSuccessListener 页成功监听器
     * @return 页任务执行服务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentPageTaskExecutionService service(
            DocumentPageTaskExecutionDependencies dependencies,
            Executor executor,
            Consumer<DocumentPageTask> pageSuccessListener
    ) {
        return new DocumentPageTaskExecutionService(dependencies,
                new DocumentPageTaskExecutionTestDoubles.InlineTransactionRunner(),
                new DocumentPageTaskExecutionOptions("worker-test", 1, 30, executor, pageSuccessListener));
    }

    /**
     * 初始化测试文档。
     *
     * @param documentRepository 文档仓储
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void seedDocuments(InMemoryDocumentJobRepository documentRepository) {
        documentRepository.save(document("doc-1"));
        documentRepository.save(document("doc-2"));
    }

    /**
     * 创建测试文档。
     *
     * @param documentId 文档 ID
     * @return 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentJob document(String documentId) {
        DocumentJobCreateRequest request = new DocumentJobCreateRequest(documentId, "batch-test",
                documentId + ".png", DocumentType.IMAGE, 5, 1, "local://" + documentId, "stub_ocr",
                Optional.empty(), JsonPayload.empty(), 0, OffsetDateTime.now(), ChunkStrategy.general());
        return DocumentJob.create(request).markOcrQueued(2, OffsetDateTime.now());
    }

    /**
     * 创建等待 OCR 的页任务。
     *
     * @param taskId 页任务 ID
     * @param documentId 文档 ID
     * @param pageNo 页码
     * @return 页任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentPageTask task(String taskId, String documentId, int pageNo) {
        DocumentPageTaskCreateRequest request = new DocumentPageTaskCreateRequest(taskId, "batch-test", documentId,
                pageNo, "local://" + documentId + "/page-" + pageNo, OffsetDateTime.now());
        return DocumentPageTask.create(request);
    }

    /**
     * 记录 OCR 请求的路由服务。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static class RecordingOcrRoutingService extends OcrRoutingService {

        private final List<ImageOcrRequest> requests = new ArrayList<>(4);

        /**
         * 创建记录型 OCR 路由服务。
         *
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        RecordingOcrRoutingService() {
            super(DocumentPageTaskRoutingTestDoubles.routingDependencies());
        }

        /**
         * 记录 OCR 请求并返回固定测试结果。
         *
         * @param request 图片 OCR 请求
         * @param requestedPolicy 请求路由策略
         * @return OCR 路由执行结果
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public OcrRouteExecutionResult recognize(ImageOcrRequest request, OcrRoutePolicy requestedPolicy) {
            requests.add(request);
            ImageOcrResult result = ImageOcrResult.fromBlocks(request.pageNo(), Map.of("ok", true),
                    List.of(new OcrBlock(request.pageNo(), "page-" + request.pageNo(), 0.99D, List.of(),
                            List.of(), "test")), List.of());
            return new OcrRouteExecutionResult(result, "stub_ocr", "node-test", 1L, 0);
        }
    }

    /**
     * OCR 调用期间检查运行中图片页任务的路由服务。
     *
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private static class InspectingOcrRoutingService extends RecordingOcrRoutingService {

        private final OcrRunningPageTaskTracker runningTracker;
        private boolean sawRunningPage;

        /**
         * 创建检查型 OCR 路由服务。
         *
         * @param runningTracker 运行中图片页任务追踪器
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        InspectingOcrRoutingService(OcrRunningPageTaskTracker runningTracker) {
            this.runningTracker = runningTracker;
        }

        /**
         * 在 OCR 调用期间检查运行中快照。
         *
         * @param request 图片 OCR 请求
         * @param requestedPolicy 请求路由策略
         * @return OCR 路由执行结果
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        @Override
        public OcrRouteExecutionResult recognize(ImageOcrRequest request, OcrRoutePolicy requestedPolicy) {
            sawRunningPage = runningTracker.snapshotByBatch(request.batchId()).stream()
                    .anyMatch(task -> task.pageNo() == request.pageNo()
                            && task.documentId().equals(request.documentId())
                            && task.workerId().equals("worker-test")
                            && task.threadName().equals(Thread.currentThread().getName()));
            return super.recognize(request, requestedPolicy);
        }
    }

    /**
     * 直接执行任务的测试执行器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static class DirectExecutor implements Executor {

        /**
         * 直接执行命令。
         *
         * @param command 待执行命令
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }

    /**
     * 只记录任务不执行的测试执行器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static class ManualExecutor implements Executor {

        private final List<Runnable> pendingTasks = new ArrayList<>(1);

        /**
         * 记录待执行命令。
         *
         * @param command 待执行命令
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public void execute(Runnable command) {
            pendingTasks.add(command);
        }
    }

    /**
     * 总是失败的运行态追踪器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private static class FailingRunningPageTaskTracker implements OcrRunningPageTaskTracker {

        /**
         * 模拟记录开始失败。
         *
         * @param command 运行中图片页任务命令
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        @Override
        public void recordStart(OcrRunningPageTaskCommand command) {
            throw new IllegalStateException("tracker failed");
        }

        /**
         * 模拟记录分配失败。
         *
         * @param assignment OCR 节点分配
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        @Override
        public void recordAssignment(OcrRunningPageTaskAssignment assignment) {
            throw new IllegalStateException("tracker failed");
        }

        /**
         * 模拟记录完成失败。
         *
         * @param taskId 页任务 ID
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        @Override
        public void recordCompletion(String taskId) {
            throw new IllegalStateException("tracker failed");
        }

        /**
         * 返回空快照。
         *
         * @param batchId 批次 ID
         * @return 空快照
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        @Override
        public List<OcrRunningPageTask> snapshotByBatch(String batchId) {
            return List.of();
        }
    }

}
