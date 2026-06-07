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
 * OCR 事件仓储的 MyBatis-Plus 实现。
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
     * 创建 MyBatis-Plus OCR 事件仓储。
     *
     * @param jsonCodec JSON 编解码器
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public MybatisPlusOcrEventRepository(JsonCodec jsonCodec) {
        this.jsonCodec = jsonCodec;
    }

    /**
     * 保存单个 OCR 事件。
     *
     * @param event OCR 事件
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Override
    public void save(OcrEvent event) {
        super.save(toEntity(event));
    }

    /**
     * 批量保存 OCR 事件。
     *
     * @param events OCR 事件集合
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Override
    public void saveAll(List<OcrEvent> events) {
        saveBatch(events.stream().map(this::toEntity).toList());
    }

    /**
     * 按发生顺序列出 OCR 事件，并限制分页大小。
     *
     * @param batchId 批次 ID
     * @return OCR 事件集合
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
     * 将领域 OCR 事件转换为持久化实体。
     *
     * @param event OCR 事件
     * @return OCR 事件持久化实体
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
     * 将持久化实体转换为领域 OCR 事件。
     *
     * @param entity OCR 事件持久化实体
     * @return OCR 事件
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
     * 解析可选 JSON 对象载荷。
     *
     * @param payload JSON 对象载荷
     * @return 解析后的对象 Map
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
