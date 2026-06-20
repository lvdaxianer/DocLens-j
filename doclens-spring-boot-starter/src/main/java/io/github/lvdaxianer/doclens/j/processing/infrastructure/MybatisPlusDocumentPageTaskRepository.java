package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTask;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskClaimRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskCompletionRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskFailureRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskStatus;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.MybatisPlusPages;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 文档页 OCR 任务仓储的 MyBatis-Plus 实现。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
@Repository
public class MybatisPlusDocumentPageTaskRepository
        extends ServiceImpl<DocumentPageTaskMapper, DocumentPageTaskEntity>
        implements DocumentPageTaskRepository {

    private static final int UPDATED_ONE_ROW = 1;
    private static final String COMPLETE_OPERATION = "complete";
    private static final String FAIL_OPERATION = "fail";
    private static final String STATE_UPDATE_FAILED_MESSAGE = "page task %s state update failed: %s";
    private static final String STATE_UPDATE_CONTEXT_MESSAGE = "taskId=%s, status=%s, lockedBy=%s, lockedUntil=%s, workerId=%s";

    /**
     * 批量保存页任务。
     *
     * @param tasks 页任务集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public void saveAll(List<DocumentPageTask> tasks) {
        saveBatch(tasks.stream().map(this::toEntity).toList());
    }

    /**
     * 查询等待调度的页任务。
     *
     * @param limit 最大返回数量
     * @return 等待调度页任务集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public List<DocumentPageTask> listQueued(int limit) {
        int safeLimit = Math.max(0, limit);
        if (safeLimit > 0) {
            // 有调度容量时直接使用跨文档公平查询，避免大文档长期独占扫描名额。
            return baseMapper.listQueuedFairly(safeLimit).stream().map(this::toDomain).toList();
        } else {
            // 调用方请求零个任务时直接返回空集合，避免底层分页兜底扩大查询。
            return List.of();
        }
    }

    /**
     * 查询抢占锁已过期的处理中页任务。
     *
     * @param now 当前时间
     * @param limit 最大返回数量
     * @return 过期处理中页任务集合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public List<DocumentPageTask> listProcessingExpired(OffsetDateTime now, int limit) {
        LambdaQueryWrapper<DocumentPageTaskEntity> wrapper = new LambdaQueryWrapper<DocumentPageTaskEntity>()
                .eq(DocumentPageTaskEntity::getStatus, DocumentPageTaskStatus.PROCESSING.name())
                .lt(DocumentPageTaskEntity::getLockedUntil, now)
                .orderByAsc(DocumentPageTaskEntity::getLockedUntil)
                .orderByAsc(DocumentPageTaskEntity::getTaskId);
        return page(MybatisPlusPages.limit(limit), wrapper).getRecords().stream().map(this::toDomain).toList();
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
        if (tasks.isEmpty()) {
            // 空批次无需触发 MyBatis-Plus 批量更新，避免不同版本对空集合行为不一致。
        } else {
            // 非空批次一次性提交，避免恢复流程循环逐条写库。
            updateBatchById(tasks.stream().map(this::toEntity).toList());
        }
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
        remove(byDocument(documentId));
    }

    /**
     * 原子抢占等待中的页任务。
     *
     * @param request 抢占请求
     * @return true 表示当前工作线程抢占成功
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public boolean tryMarkProcessing(DocumentPageTaskClaimRequest request) {
        return baseMapper.tryMarkProcessing(request) == UPDATED_ONE_ROW;
    }

    /**
     * 标记页任务已完成。
     *
     * @param request 完成请求
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public void markCompleted(DocumentPageTaskCompletionRequest request) {
        ensureUpdated(baseMapper.markCompleted(request), request.taskId(), COMPLETE_OPERATION, request.workerId());
    }

    /**
     * 标记页任务终态失败。
     *
     * @param request 失败请求
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public void markFailed(DocumentPageTaskFailureRequest request) {
        ensureUpdated(baseMapper.markFailed(request), request.taskId(), FAIL_OPERATION, null);
    }

    /**
     * 校验页任务状态更新结果。
     *
     * @param updatedRows 更新行数
     * @param taskId 页任务 ID
     * @param operation 操作名称
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void ensureUpdated(int updatedRows, String taskId, String operation, String workerId) {
        // 条件更新 0 行说明锁归属或状态不匹配，继续当作成功会破坏恢复语义。
        if (updatedRows != UPDATED_ONE_ROW) {
            throw new IllegalStateException(STATE_UPDATE_FAILED_MESSAGE.formatted(operation,
                    stateUpdateContext(taskId, workerId)));
        } else {
            // 更新成功时不额外查询数据库，保持原有成功路径轻量。
        }
    }

    /**
     * 构造页任务状态更新失败上下文。
     *
     * @param taskId 页任务 ID
     * @return 状态更新上下文
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private String stateUpdateContext(String taskId, String workerId) {
        DocumentPageTaskEntity entity = getBaseMapper().selectById(taskId);
        if (entity == null) {
            return STATE_UPDATE_CONTEXT_MESSAGE.formatted(taskId, "missing", "missing", "missing",
                    workerId == null ? "unknown" : workerId);
        } else {
            return STATE_UPDATE_CONTEXT_MESSAGE.formatted(taskId, entity.getStatus(), entity.getLockedBy(),
                    entity.getLockedUntil(), workerId == null ? "unknown" : workerId);
        }
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
        LambdaQueryWrapper<DocumentPageTaskEntity> wrapper = byDocument(documentId)
                .orderByAsc(DocumentPageTaskEntity::getPageNo);
        return page(MybatisPlusPages.listLimit(), wrapper).getRecords().stream().map(this::toDomain).toList();
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
        LambdaQueryWrapper<DocumentPageTaskEntity> wrapper = byDocument(documentId)
                .eq(DocumentPageTaskEntity::getPageNo, pageNo);
        return page(MybatisPlusPages.one(), wrapper).getRecords().stream().findFirst().map(this::toDomain);
    }

    /**
     * 构造文档维度查询条件。
     *
     * @param documentId 文档 ID
     * @return 查询条件
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private LambdaQueryWrapper<DocumentPageTaskEntity> byDocument(String documentId) {
        return new LambdaQueryWrapper<DocumentPageTaskEntity>().eq(DocumentPageTaskEntity::getDocumentId, documentId);
    }

    /**
     * 将领域页任务转换为持久化实体。
     *
     * @param task 页任务
     * @return 持久化实体
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private DocumentPageTaskEntity toEntity(DocumentPageTask task) {
        DocumentPageTaskEntity entity = new DocumentPageTaskEntity();
        entity.setTaskId(task.taskId());
        entity.setBatchId(task.batchId());
        entity.setDocumentId(task.documentId());
        entity.setPageNo(task.pageNo());
        entity.setImageStorageUri(task.imageStorageUri());
        entity.setStatus(task.status().name());
        entity.setLockedBy(task.lockedBy().orElse(null));
        entity.setLockedUntil(task.lockedUntil().orElse(null));
        entity.setRetryCount(task.retryCount());
        entity.setErrorCode(task.errorCode().orElse(null));
        entity.setErrorMessage(task.errorMessage().orElse(null));
        entity.setStartedAt(task.startedAt().orElse(null));
        entity.setCompletedAt(task.completedAt().orElse(null));
        entity.setCreatedAt(task.createdAt());
        entity.setUpdatedAt(task.updatedAt());
        return entity;
    }

    /**
     * 将持久化实体转换为领域页任务。
     *
     * @param entity 持久化实体
     * @return 页任务
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private DocumentPageTask toDomain(DocumentPageTaskEntity entity) {
        return new DocumentPageTask(entity.getTaskId(), entity.getBatchId(), entity.getDocumentId(),
                entity.getPageNo(), entity.getImageStorageUri(), DocumentPageTaskStatus.valueOf(entity.getStatus()),
                Optional.ofNullable(entity.getLockedBy()), Optional.ofNullable(entity.getLockedUntil()),
                entity.getRetryCount(), Optional.ofNullable(entity.getErrorCode()),
                Optional.ofNullable(entity.getErrorMessage()), Optional.ofNullable(entity.getStartedAt()),
                Optional.ofNullable(entity.getCompletedAt()), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
