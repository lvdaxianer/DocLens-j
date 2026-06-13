package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResult;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResultRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTask;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskRepository;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文档页任务重启恢复服务。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
public class DocumentPageTaskRecoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentPageTaskRecoveryService.class);
    private static final float HASH_SET_LOAD_FACTOR = 0.75F;
    private static final String RECOVERY_WORKER_ID = "recovery";
    private static final String PAGE_KEY_SEPARATOR = "#";

    private final DocumentPageTaskRepository pageTaskRepository;
    private final DocumentPageResultRepository pageResultRepository;
    private final TransactionRunner transactionRunner;
    private final Consumer<DocumentPageTask> pageCompletedListener;

    /**
     * 创建文档页任务恢复服务。
     *
     * @param dependencies 恢复依赖
     * @param transactionRunner 事务执行器
     * @param pageCompletedListener 页完成监听器
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    public DocumentPageTaskRecoveryService(
            DocumentPageTaskRecoveryDependencies dependencies,
            TransactionRunner transactionRunner,
            Consumer<DocumentPageTask> pageCompletedListener
    ) {
        this.pageTaskRepository = dependencies.pageTaskRepository();
        this.pageResultRepository = dependencies.pageResultRepository();
        this.transactionRunner = transactionRunner;
        this.pageCompletedListener = pageCompletedListener;
    }

    /**
     * 恢复抢占锁已过期的页任务。
     *
     * @param now 当前恢复时间
     * @param limit 最大恢复数量
     * @return 已恢复页任务数量
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    public int recoverExpiredTasks(OffsetDateTime now, int limit) {
        List<DocumentPageTask> expiredTasks = pageTaskRepository.listProcessingExpired(now, limit);
        if (expiredTasks.isEmpty()) {
            // 本轮没有过期锁时直接结束，避免执行不必要的批量查询。
            return 0;
        } else {
            // 存在过期任务时进入批量恢复，统一处理已落库结果和待重试页。
            return recoverExpiredTasks(now, expiredTasks);
        }
    }

    /**
     * 批量恢复已过期页任务。
     *
     * @param now 当前恢复时间
     * @param expiredTasks 过期页任务集合
     * @return 已恢复页任务数量
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private int recoverExpiredTasks(OffsetDateTime now, List<DocumentPageTask> expiredTasks) {
        Set<String> completedKeys = completedPageKeys(expiredTasks);
        List<DocumentPageTask> recoveredTasks = recoveredTasks(expiredTasks, completedKeys, now);
        transactionRunner.requiredVoid(() -> pageTaskRepository.updateAll(recoveredTasks));
        notifyRecoveredCompletedDocuments(recoveredTasks, completedKeys);
        LOGGER.info("[页任务恢复] 过期页任务恢复完成, recoveredCount={}", recoveredTasks.size());
        return recoveredTasks.size();
    }

    /**
     * 批量查询已存在页结果的页唯一键。
     *
     * @param expiredTasks 过期页任务集合
     * @return 已存在页结果的页唯一键集合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private Set<String> completedPageKeys(List<DocumentPageTask> expiredTasks) {
        List<String> documentIds = distinctDocumentIds(expiredTasks);
        int capacity = hashSetCapacity(expiredTasks.size());
        return pageResultRepository.listByDocumentIds(documentIds).stream()
                .map(this::pageKey)
                .collect(Collectors.toCollection(() -> new HashSet<>(capacity)));
    }

    /**
     * 提取去重文档 ID。
     *
     * @param tasks 页任务集合
     * @return 文档 ID 集合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private List<String> distinctDocumentIds(List<DocumentPageTask> tasks) {
        return tasks.stream().map(DocumentPageTask::documentId).distinct().toList();
    }

    /**
     * 构建恢复后的页任务集合。
     *
     * @param expiredTasks 过期页任务集合
     * @param completedKeys 已有页结果键集合
     * @param now 当前恢复时间
     * @return 恢复后的页任务集合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private List<DocumentPageTask> recoveredTasks(
            List<DocumentPageTask> expiredTasks,
            Set<String> completedKeys,
            OffsetDateTime now
    ) {
        return expiredTasks.stream().map(task -> recoveredTask(task, completedKeys, now)).toList();
    }

    /**
     * 按文档触发已完成页恢复回调。
     *
     * @param recoveredTasks 已恢复页任务集合
     * @param completedKeys 已有页结果键集合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void notifyRecoveredCompletedDocuments(
            List<DocumentPageTask> recoveredTasks,
            Set<String> completedKeys
    ) {
        Set<String> notifiedDocumentIds = new HashSet<>(hashSetCapacity(recoveredTasks.size()));
        recoveredTasks.stream()
                .filter(task -> completedKeys.contains(pageKey(task)))
                .filter(task -> notifiedDocumentIds.add(task.documentId()))
                .forEach(pageCompletedListener);
    }

    /**
     * 根据页结果存在性恢复单个页任务。
     *
     * @param task 过期页任务
     * @param completedKeys 已有页结果键集合
     * @param now 当前恢复时间
     * @return 恢复后的页任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentPageTask recoveredTask(
            DocumentPageTask task,
            Set<String> completedKeys,
            OffsetDateTime now
    ) {
        if (completedKeys.contains(pageKey(task))) {
            // 页结果已经存在，说明宕机发生在结果落库后，补齐 COMPLETED 状态即可。
            return task.markCompleted(task.lockedBy().orElse(RECOVERY_WORKER_ID), now);
        } else {
            // 页结果不存在，说明 OCR 或落库未完成，需要清锁后回到队列等待重试。
            return task.resetForRetry(now);
        }
    }

    /**
     * 生成页任务唯一键。
     *
     * @param task 页任务
     * @return 页唯一键
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private String pageKey(DocumentPageTask task) {
        return pageKey(task.documentId(), task.pageNo());
    }

    /**
     * 生成页结果唯一键。
     *
     * @param result 页结果
     * @return 页唯一键
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private String pageKey(DocumentPageResult result) {
        return pageKey(result.documentId(), result.pageNo());
    }

    /**
     * 生成文档页唯一键。
     *
     * @param documentId 文档 ID
     * @param pageNo 页码
     * @return 页唯一键
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private String pageKey(String documentId, int pageNo) {
        return documentId + PAGE_KEY_SEPARATOR + pageNo;
    }

    /**
     * 计算 HashSet 初始容量。
     *
     * @param expectedSize 预期元素数
     * @return HashSet 初始容量
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private int hashSetCapacity(int expectedSize) {
        return (int) (expectedSize / HASH_SET_LOAD_FACTOR) + 1;
    }
}
