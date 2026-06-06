package com.doclens.ingestion.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.doclens.ingestion.domain.Batch;
import com.doclens.ingestion.domain.BatchRepository;
import com.doclens.ingestion.domain.BatchStatus;
import com.doclens.shared.domain.DocLensConstants;
import com.doclens.shared.domain.JsonPayload;
import com.doclens.shared.infrastructure.JsonCodec;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * MyBatis-Plus implementation of batch repository.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@Repository
public class MybatisPlusBatchRepository extends ServiceImpl<BatchMapper, BatchEntity> implements BatchRepository {

    private final JsonCodec jsonCodec;

    /**
     * Creates MyBatis-Plus batch repository.
     *
     * @param jsonCodec JSON codec
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public MybatisPlusBatchRepository(JsonCodec jsonCodec) {
        this.jsonCodec = jsonCodec;
    }

    @Override
    public void save(Batch batch) {
        super.save(toEntity(batch));
    }

    @Override
    public Optional<Batch> findById(String batchId) {
        return Optional.ofNullable(getById(batchId)).map(this::toDomain);
    }

    @Override
    public Optional<Batch> findByIdempotencyKey(String idempotencyKey) {
        LambdaQueryWrapper<BatchEntity> wrapper = new LambdaQueryWrapper<BatchEntity>()
                .eq(BatchEntity::getIdempotencyKey, idempotencyKey);
        return Optional.ofNullable(getOne(wrapper, false)).map(this::toDomain);
    }

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
