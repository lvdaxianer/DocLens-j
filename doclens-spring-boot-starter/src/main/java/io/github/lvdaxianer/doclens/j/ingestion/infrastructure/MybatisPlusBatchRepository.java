package io.github.lvdaxianer.doclens.j.ingestion.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.JsonCodec;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.MybatisPlusPages;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 批次仓储的 MyBatis-Plus 实现。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@Repository
public class MybatisPlusBatchRepository extends ServiceImpl<BatchMapper, BatchEntity> implements BatchRepository {

    private final JsonCodec jsonCodec;

    /**
     * 创建 MyBatis-Plus 批次仓储。
     *
     * @param jsonCodec JSON 编解码器
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public MybatisPlusBatchRepository(JsonCodec jsonCodec) {
        this.jsonCodec = jsonCodec;
    }

    /**
     * 保存批次聚合。
     *
     * @param batch 批次聚合
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Override
    public void save(Batch batch) {
        super.save(toEntity(batch));
    }

    /**
     * 根据 ID 查找批次。
     *
     * @param batchId 批次 ID
     * @return 可选批次聚合
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Override
    public Optional<Batch> findById(String batchId) {
        return Optional.ofNullable(getById(batchId)).map(this::toDomain);
    }

    /**
     * 按幂等键查找批次，并显式限制单行结果。
     *
     * @param idempotencyKey 幂等键
     * @return 可选批次聚合
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Override
    public Optional<Batch> findByIdempotencyKey(String idempotencyKey) {
        LambdaQueryWrapper<BatchEntity> wrapper = new LambdaQueryWrapper<BatchEntity>()
                .eq(BatchEntity::getIdempotencyKey, idempotencyKey);
        return page(MybatisPlusPages.one(), wrapper).getRecords().stream().findFirst().map(this::toDomain);
    }

    /**
     * 按更新时间倒序列出最近批次。
     *
     * @param limit 最大返回数量
     * @return 最近批次集合
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Override
    public List<Batch> listRecent(int limit) {
        LambdaQueryWrapper<BatchEntity> wrapper = new LambdaQueryWrapper<BatchEntity>()
                .orderByDesc(BatchEntity::getUpdatedAt)
                .orderByDesc(BatchEntity::getBatchId);
        return page(MybatisPlusPages.limit(limit), wrapper).getRecords().stream().map(this::toDomain).toList();
    }

    /**
     * 根据 ID 删除批次。
     *
     * @param batchId 批次 ID
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public void deleteById(String batchId) {
        removeById(batchId);
    }

    /**
     * 更新批次完成摘要。
     *
     * @param batchId 批次 ID
     * @param completedFiles 已完成文件数
     * @param failedFiles 失败文件数
     * @param status 最终批次状态
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Override
    public void updateSummary(String batchId, int completedFiles, int failedFiles, BatchStatus status) {
        LambdaUpdateWrapper<BatchEntity> wrapper = new LambdaUpdateWrapper<BatchEntity>()
                .eq(BatchEntity::getBatchId, batchId)
                .set(BatchEntity::getStatus, status.name().toLowerCase())
                .set(BatchEntity::getCompletedFiles, completedFiles)
                .set(BatchEntity::getFailedFiles, failedFiles)
                .set(BatchEntity::getCurrentDocumentId, null)
                .set(BatchEntity::getCurrentDocumentName, null)
                .set(BatchEntity::getCurrentStage, status.name().toLowerCase())
                .set(BatchEntity::getUpdatedAt, OffsetDateTime.now());
        update(wrapper);
    }

    /**
     * 更新批次完成摘要，并同步总文件数。
     *
     * @param batchId 批次 ID
     * @param totalFiles 总文件数
     * @param completedFiles 已完成文件数
     * @param failedFiles 失败文件数
     * @param status 最终批次状态
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public void updateSummary(
            String batchId,
            int totalFiles,
            int completedFiles,
            int failedFiles,
            BatchStatus status
    ) {
        LambdaUpdateWrapper<BatchEntity> wrapper = new LambdaUpdateWrapper<BatchEntity>()
                .eq(BatchEntity::getBatchId, batchId)
                .set(BatchEntity::getStatus, status.name().toLowerCase())
                .set(BatchEntity::getTotalFiles, totalFiles)
                .set(BatchEntity::getCompletedFiles, completedFiles)
                .set(BatchEntity::getFailedFiles, failedFiles)
                .set(BatchEntity::getCurrentDocumentId, null)
                .set(BatchEntity::getCurrentDocumentName, null)
                .set(BatchEntity::getCurrentStage, status.name().toLowerCase())
                .set(BatchEntity::getUpdatedAt, OffsetDateTime.now());
        update(wrapper);
    }

    /**
     * 将领域批次转换为持久化实体。
     *
     * @param batch 批次聚合
     * @return 批次持久化实体
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    private BatchEntity toEntity(Batch batch) {
        BatchEntity entity = new BatchEntity();
        entity.setBatchId(batch.batchId());
        entity.setStatus(batch.status().name().toLowerCase());
        entity.setTotalFiles(batch.totalFiles());
        entity.setCompletedFiles(batch.completedFiles());
        entity.setFailedFiles(batch.failedFiles());
        entity.setCurrentDocumentId(batch.currentDocumentId().orElse(null));
        entity.setCurrentDocumentName(batch.currentDocumentName().orElse(DocLensConstants.EMPTY_VALUE));
        entity.setCurrentStage(batch.currentStage());
        entity.setMetadata(jsonCodec.toJson(batch.metadata().values()));
        entity.setCallbackUrl(batch.callbackUrl().orElse(null));
        entity.setIdempotencyKey(batch.idempotencyKey().orElse(null));
        entity.setCreatedAt(batch.createdAt());
        entity.setUpdatedAt(batch.updatedAt());
        return entity;
    }

    /**
     * 将持久化实体转换为领域批次。
     *
     * @param entity 批次持久化实体
     * @return 批次聚合
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    private Batch toDomain(BatchEntity entity) {
        return new Batch(entity.getBatchId(), BatchStatus.valueOf(entity.getStatus().toUpperCase()),
                entity.getTotalFiles(), entity.getCompletedFiles(), entity.getFailedFiles(),
                Optional.ofNullable(entity.getCurrentDocumentId()),
                Optional.ofNullable(entity.getCurrentDocumentName()).filter(value -> !value.isBlank()),
                entity.getCurrentStage(), new JsonPayload(jsonCodec.parseObject(entity.getMetadata())),
                Optional.ofNullable(entity.getCallbackUrl()), Optional.ofNullable(entity.getIdempotencyKey()),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
