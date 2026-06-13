package io.github.lvdaxianer.doclens.j.processing.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResult;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTask;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskStatus;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 文档页任务恢复服务测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
class DocumentPageTaskRecoveryServiceTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-11T01:10:00+08:00");
    private static final int RECOVERY_LIMIT = 10;

    /**
     * 页结果已落库时，恢复服务应把过期处理中页任务修复为已完成。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void recoverExpiredTasksMarksCompletedWhenPageResultAlreadyExists() {
        TestContext context = testContext();
        // 模拟宕机点：页结果已经提交成功，但页任务状态尚未从 PROCESSING 修正为 COMPLETED。
        context.taskRepository.saveAll(List.of(processingTask("task-1", "doc-1", 1)));
        context.pageResultRepository.upsert(pageResult("doc-1", 1, "done"));

        int recoveredCount = context.service.recoverExpiredTasks(BASE_TIME, RECOVERY_LIMIT);

        // 恢复后必须触发完成回调，保证文档聚合不会因为重启停在半完成状态。
        assertThat(recoveredCount).isEqualTo(1);
        assertThat(context.taskRepository.findByDocumentIdAndPageNo("doc-1", 1)).get()
                .extracting(DocumentPageTask::status)
                .isEqualTo(DocumentPageTaskStatus.COMPLETED);
        assertThat(context.completedCallbacks)
                .extracting(DocumentPageTask::taskId)
                .containsExactly("task-1");
    }

    /**
     * 页结果未落库时，恢复服务应把过期处理中页任务重新放回等待队列。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void recoverExpiredTasksRequeuesWhenPageResultDoesNotExist() {
        TestContext context = testContext();
        // 没有页结果代表 OCR 可能在远程调用中断或结果落库前宕机，需要重新排队。
        context.taskRepository.saveAll(List.of(processingTask("task-2", "doc-1", 2)));

        int recoveredCount = context.service.recoverExpiredTasks(BASE_TIME, RECOVERY_LIMIT);

        // 重入队列时清空锁归属，下一轮 worker 才能重新原子抢占该页。
        assertThat(recoveredCount).isEqualTo(1);
        assertThat(context.taskRepository.findByDocumentIdAndPageNo("doc-1", 2)).get().satisfies(task -> {
            assertThat(task.status()).isEqualTo(DocumentPageTaskStatus.QUEUED);
            assertThat(task.retryCount()).isEqualTo(1);
            assertThat(task.lockedBy()).isEmpty();
        });
        assertThat(context.completedCallbacks).isEmpty();
    }

    /**
     * 创建测试上下文。
     *
     * @return 测试上下文
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private TestContext testContext() {
        InMemoryDocumentPageTaskRepository taskRepository = new InMemoryDocumentPageTaskRepository();
        DocumentPageTaskExecutionTestDoubles.InMemoryDocumentPageResultRepository pageResultRepository =
                new DocumentPageTaskExecutionTestDoubles.InMemoryDocumentPageResultRepository();
        List<DocumentPageTask> completedCallbacks = new ArrayList<>(2);
        DocumentPageTaskRecoveryDependencies dependencies =
                new DocumentPageTaskRecoveryDependencies(taskRepository, pageResultRepository);
        DocumentPageTaskRecoveryService service = new DocumentPageTaskRecoveryService(dependencies,
                new DocumentPageTaskExecutionTestDoubles.InlineTransactionRunner(), completedCallbacks::add);
        return new TestContext(taskRepository, pageResultRepository, completedCallbacks, service);
    }

    /**
     * 创建处理中过期页任务。
     *
     * @param taskId 页任务 ID
     * @param documentId 文档 ID
     * @param pageNo 页码
     * @return 处理中过期页任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentPageTask processingTask(String taskId, String documentId, int pageNo) {
        DocumentPageTaskCreateRequest request = new DocumentPageTaskCreateRequest(taskId, "batch-1", documentId,
                pageNo, "local://" + documentId + "/page-" + pageNo, BASE_TIME.minusMinutes(5));
        return DocumentPageTask.create(request).markProcessing("worker-a", BASE_TIME.minusSeconds(1),
                BASE_TIME.minusMinutes(4));
    }

    /**
     * 创建页 OCR 结果。
     *
     * @param documentId 文档 ID
     * @param pageNo 页码
     * @param text 页文本
     * @return 页 OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentPageResult pageResult(String documentId, int pageNo, String text) {
        return new DocumentPageResult(documentId, pageNo, Map.of("pageNo", pageNo), text,
                List.of(Map.of("text", text)), 0.99D, List.of(), "node-test", BASE_TIME, BASE_TIME);
    }

    /**
     * 恢复测试上下文。
     *
     * @param taskRepository 页任务仓储
     * @param pageResultRepository 页结果仓储
     * @param completedCallbacks 完成回调记录
     * @param service 页任务恢复服务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private record TestContext(
            InMemoryDocumentPageTaskRepository taskRepository,
            DocumentPageTaskExecutionTestDoubles.InMemoryDocumentPageResultRepository pageResultRepository,
            List<DocumentPageTask> completedCallbacks,
            DocumentPageTaskRecoveryService service
    ) {
    }
}
