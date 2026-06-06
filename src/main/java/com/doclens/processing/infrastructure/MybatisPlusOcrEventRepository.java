package com.doclens.processing.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.doclens.processing.domain.OcrEvent;
import com.doclens.processing.domain.OcrEventRepository;
import com.doclens.shared.domain.DocLensConstants;
import com.doclens.shared.domain.JsonPayload;
import com.doclens.shared.infrastructure.JsonCodec;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * MyBatis-Plus implementation of OCR event repository.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@Repository
public class MybatisPlusOcrEventRepository
        extends ServiceImpl<OcrEventMapper, OcrEventEntity>
        implements OcrEventRepository {

    private final JsonCodec jsonCodec;

    /**
     * Creates MyBatis-Plus OCR event repository.
     *
     * @param jsonCodec JSON codec
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public MybatisPlusOcrEventRepository(JsonCodec jsonCodec) {
        this.jsonCodec = jsonCodec;
    }

    @Override
    public void save(OcrEvent event) {
        super.save(toEntity(event));
    }

    @Override
    public void saveAll(List<OcrEvent> events) {
        saveBatch(events.stream().map(this::toEntity).toList());
    }

    @Override
    public List<OcrEvent> listByBatchId(String batchId) {
        LambdaQueryWrapper<OcrEventEntity> wrapper = new LambdaQueryWrapper<OcrEventEntity>()
                .eq(OcrEventEntity::getBatchId, batchId)
                .orderByAsc(OcrEventEntity::getOccurredAt)
                .orderByAsc(OcrEventEntity::getEventId);
        Page<OcrEventEntity> page = Page.of(DocLensConstants.FIRST_PAGE_NO, DocLensConstants.DEFAULT_QUERY_LIMIT);
        return page(page, wrapper).getRecords().stream().map(this::toDomain).toList();
    }

    private OcrEventEntity toEntity(OcrEvent event) {
        OcrEventEntity entity = new OcrEventEntity();
        entity.setEventId(event.eventId());
        entity.setEventType(event.eventType());
        entity.setBatchId(event.batchId());
        entity.setDocumentId(event.documentId().orElse(null));
        entity.setStatus(event.status());
        entity.setStage(event.stage());
        entity.setProgress(jsonCodec.toJson(event.progress()));
        entity.setMetadata(jsonCodec.toJson(event.metadata().values()));
        entity.setResultId(event.resultId().orElse(null));
        entity.setResultSummary(event.resultSummary().isEmpty() ? null : jsonCodec.toJson(event.resultSummary()));
        entity.setError(event.error().isEmpty() ? null : jsonCodec.toJson(event.error()));
        entity.setOccurredAt(event.occurredAt());
        return entity;
    }

    private OcrEvent toDomain(OcrEventEntity entity) {
        return new OcrEvent(entity.getEventId(), entity.getEventType(), entity.getBatchId(),
                Optional.ofNullable(entity.getDocumentId()), entity.getStatus(), entity.getStage(),
                jsonCodec.parseObject(entity.getProgress()), new JsonPayload(jsonCodec.parseObject(entity.getMetadata())),
                Optional.ofNullable(entity.getResultId()), parseNullableObject(entity.getResultSummary()),
                parseNullableObject(entity.getError()), entity.getOccurredAt());
    }

    private java.util.Map<String, Object> parseNullableObject(String payload) {
        if (payload == null || payload.isBlank()) {
            return java.util.Map.of();
        } else {
            return jsonCodec.parseObject(payload);
        }
    }
}
