package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.JsonCodec;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.MybatisPlusPages;
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

    /**
     * Saves one OCR event.
     *
     * @param event OCR event
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Override
    public void save(OcrEvent event) {
        super.save(toEntity(event));
    }

    /**
     * Saves OCR events in batch.
     *
     * @param events OCR events
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Override
    public void saveAll(List<OcrEvent> events) {
        saveBatch(events.stream().map(this::toEntity).toList());
    }

    /**
     * Lists OCR events by occurrence order with bounded page size.
     *
     * @param batchId batch id
     * @return OCR events
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Override
    public List<OcrEvent> listByBatchId(String batchId) {
        LambdaQueryWrapper<OcrEventEntity> wrapper = new LambdaQueryWrapper<OcrEventEntity>()
                .eq(OcrEventEntity::getBatchId, batchId)
                .orderByAsc(OcrEventEntity::getOccurredAt)
                .orderByAsc(OcrEventEntity::getEventId);
        return page(MybatisPlusPages.listLimit(), wrapper).getRecords().stream().map(this::toDomain).toList();
    }

    /**
     * Converts domain OCR event to persistence entity.
     *
     * @param event OCR event
     * @return OCR event persistence entity
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
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

    /**
     * Converts persistence entity to domain OCR event.
     *
     * @param entity OCR event persistence entity
     * @return OCR event
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    private OcrEvent toDomain(OcrEventEntity entity) {
        return new OcrEvent(entity.getEventId(), entity.getEventType(), entity.getBatchId(),
                Optional.ofNullable(entity.getDocumentId()), entity.getStatus(), entity.getStage(),
                jsonCodec.parseObject(entity.getProgress()), new JsonPayload(jsonCodec.parseObject(entity.getMetadata())),
                Optional.ofNullable(entity.getResultId()), parseNullableObject(entity.getResultSummary()),
                parseNullableObject(entity.getError()), entity.getOccurredAt());
    }

    /**
     * Parses optional JSON object payload.
     *
     * @param payload JSON object payload
     * @return parsed object map
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    private java.util.Map<String, Object> parseNullableObject(String payload) {
        if (payload == null || payload.isBlank()) {
            return java.util.Map.of();
        } else {
            return jsonCodec.parseObject(payload);
        }
    }
}
