package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTask;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskClaimRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskCompletionRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskFailureRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskStatus;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 测试用内存页任务仓储。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
class InMemoryDocumentPageTaskRepository implements DocumentPageTaskRepository {

    private static final int TEST_PAGE_CAPACITY = 4;

    private final List<DocumentPageTask> tasks = new ArrayList<>(TEST_PAGE_CAPACITY);

    /**
     * 批量保存页任务。
     *
     * @param tasks 页任务集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public void saveAll(List<DocumentPageTask> tasks) {
        this.tasks.addAll(tasks);
    }

    /**
     * 查询等待任务。
     *
     * @param limit 最大返回数量
     * @return 等待任务集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public List<DocumentPageTask> listQueued(int limit) {
        return tasks.stream().filter(task -> task.status() == DocumentPageTaskStatus.QUEUED).limit(limit).toList();
    }

    /**
     * 查询抢占锁已过期的处理中任务。
     *
     * @param now 当前时间
     * @param limit 最大返回数量
     * @return 过期处理中任务集合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public List<DocumentPageTask> listProcessingExpired(OffsetDateTime now, int limit) {
        return tasks.stream()
                .filter(task -> task.status() == DocumentPageTaskStatus.PROCESSING)
                .filter(task -> task.lockedUntil().map(lockedUntil -> lockedUntil.isBefore(now)).orElse(false))
                .limit(limit)
                .toList();
    }

    /**
     * 批量更新页任务。
     *
     * @param tasks 页任务集合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void updateAll(List<DocumentPageTask> tasks) {
        tasks.forEach(this::replace);
    }

    /**
     * 按文档删除全部页任务。
     *
     * @param documentId 文档 ID
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Override
    public void deleteByDocumentId(String documentId) {
        tasks.removeIf(task -> documentId.equals(task.documentId()));
    }

    /**
     * 原子抢占等待任务。
     *
     * @param request 抢占请求
     * @return 是否抢占成功
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public boolean tryMarkProcessing(DocumentPageTaskClaimRequest request) {
        Optional<DocumentPageTask> task = findByTaskId(request.taskId());
        if (task.isPresent() && task.get().status() == DocumentPageTaskStatus.QUEUED) {
            replace(task.get().markProcessing(request.workerId(), request.lockedUntil(), request.now()));
            return true;
        } else {
            return false;
        }
    }

    /**
     * 标记任务完成。
     *
     * @param request 完成请求
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public void markCompleted(DocumentPageTaskCompletionRequest request) {
        findByTaskId(request.taskId()).ifPresent(task -> replace(task.markCompleted(request.workerId(),
                request.now())));
    }

    /**
     * 标记任务失败。
     *
     * @param request 失败请求
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public void markFailed(DocumentPageTaskFailureRequest request) {
        findByTaskId(request.taskId()).ifPresent(task -> replace(task.markFailed(request.errorCode(),
                request.errorMessage(), request.now())));
    }

    /**
     * 按文档查询页任务。
     *
     * @param documentId 文档 ID
     * @return 页任务集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public List<DocumentPageTask> listByDocumentId(String documentId) {
        return tasks.stream().filter(task -> documentId.equals(task.documentId())).toList();
    }

    /**
     * 按文档与页码查询页任务。
     *
     * @param documentId 文档 ID
     * @param pageNo 页码
     * @return 可选页任务
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public Optional<DocumentPageTask> findByDocumentIdAndPageNo(String documentId, int pageNo) {
        return tasks.stream()
                .filter(task -> documentId.equals(task.documentId()))
                .filter(task -> task.pageNo() == pageNo)
                .findFirst();
    }

    /**
     * 按任务 ID 查询页任务。
     *
     * @param taskId 页任务 ID
     * @return 可选页任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private Optional<DocumentPageTask> findByTaskId(String taskId) {
        return tasks.stream().filter(task -> taskId.equals(task.taskId())).findFirst();
    }

    /**
     * 替换已有页任务。
     *
     * @param nextTask 新页任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void replace(DocumentPageTask nextTask) {
        findByTaskId(nextTask.taskId()).ifPresent(currentTask -> {
            tasks.remove(currentTask);
            tasks.add(nextTask);
        });
    }
}
