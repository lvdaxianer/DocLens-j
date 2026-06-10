package io.github.lvdaxianer.doclens.j.processing.domain;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * 文档单页 OCR 任务。
 *
 * @param taskId 任务 ID
 * @param batchId 批次 ID
 * @param documentId 文档 ID
 * @param pageNo 页码
 * @param imageStorageUri 页面图片存储地址
 * @param status 页任务状态
 * @param lockedBy 抢占任务的工作线程标识
 * @param lockedUntil 抢占锁过期时间
 * @param retryCount 重试次数
 * @param errorCode 错误码
 * @param errorMessage 错误信息
 * @param startedAt 开始时间
 * @param completedAt 完成时间
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record DocumentPageTask(
        String taskId,
        String batchId,
        String documentId,
        int pageNo,
        String imageStorageUri,
        DocumentPageTaskStatus status,
        Optional<String> lockedBy,
        Optional<OffsetDateTime> lockedUntil,
        int retryCount,
        Optional<String> errorCode,
        Optional<String> errorMessage,
        Optional<OffsetDateTime> startedAt,
        Optional<OffsetDateTime> completedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /**
     * 创建等待调度的页任务。
     *
     * @param request 创建请求
     * @return 页任务
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public static DocumentPageTask create(DocumentPageTaskCreateRequest request) {
        return new DocumentPageTask(request.taskId(), request.batchId(), request.documentId(), request.pageNo(),
                request.imageStorageUri(),
                DocumentPageTaskStatus.QUEUED, Optional.empty(), Optional.empty(), 0, Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), request.now(), request.now());
    }

    /**
     * 标记页任务已被工作线程抢占。
     *
     * @param workerId 工作线程标识
     * @param lockUntil 锁过期时间
     * @return 处理中的页任务
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public DocumentPageTask markProcessing(String workerId, OffsetDateTime lockUntil) {
        return markProcessing(workerId, lockUntil, updatedAt);
    }

    /**
     * 标记页任务已被工作线程抢占。
     *
     * @param workerId 工作线程标识
     * @param lockUntil 锁过期时间
     * @param now 当前更新时间
     * @return 处理中的页任务
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public DocumentPageTask markProcessing(String workerId, OffsetDateTime lockUntil, OffsetDateTime now) {
        return new DocumentPageTask(taskId, batchId, documentId, pageNo, imageStorageUri,
                DocumentPageTaskStatus.PROCESSING, Optional.of(workerId), Optional.of(lockUntil), retryCount,
                Optional.empty(), Optional.empty(), Optional.of(now), Optional.empty(), createdAt, now);
    }

    /**
     * 标记页任务 OCR 已完成。
     *
     * @param workerId 工作线程标识
     * @param now 当前完成时间
     * @return 已完成页任务
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public DocumentPageTask markCompleted(String workerId, OffsetDateTime now) {
        return new DocumentPageTask(taskId, batchId, documentId, pageNo, imageStorageUri,
                DocumentPageTaskStatus.COMPLETED, Optional.of(workerId), lockedUntil, retryCount, Optional.empty(),
                Optional.empty(), startedAt, Optional.of(now), createdAt, now);
    }

    /**
     * 标记页任务 OCR 终态失败。
     *
     * @param errorCode 错误码
     * @param errorMessage 错误信息
     * @param now 当前失败时间
     * @return 已失败页任务
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public DocumentPageTask markFailed(String errorCode, String errorMessage, OffsetDateTime now) {
        return new DocumentPageTask(taskId, batchId, documentId, pageNo, imageStorageUri,
                DocumentPageTaskStatus.FAILED, lockedBy, lockedUntil, retryCount, Optional.of(errorCode),
                Optional.of(errorMessage), startedAt, Optional.of(now), createdAt, now);
    }

    /**
     * 将过期处理中任务重置为等待重试。
     *
     * @param now 当前更新时间
     * @return 等待重试的页任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    public DocumentPageTask resetForRetry(OffsetDateTime now) {
        return new DocumentPageTask(taskId, batchId, documentId, pageNo, imageStorageUri,
                DocumentPageTaskStatus.QUEUED, Optional.empty(), Optional.empty(), retryCount + 1,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), createdAt, now);
    }
}
