package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTask;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskStatus;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.MybatisPlusPages;
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
        LambdaQueryWrapper<DocumentPageTaskEntity> wrapper = new LambdaQueryWrapper<DocumentPageTaskEntity>()
                .eq(DocumentPageTaskEntity::getStatus, DocumentPageTaskStatus.QUEUED.name())
                .orderByAsc(DocumentPageTaskEntity::getCreatedAt)
                .orderByAsc(DocumentPageTaskEntity::getTaskId);
        return page(MybatisPlusPages.limit(limit), wrapper).getRecords().stream().map(this::toDomain).toList();
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
