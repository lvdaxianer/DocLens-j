package io.github.lvdaxianer.doclens.j.processing.domain;

import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import java.time.OffsetDateTime;

/**
 * OCR 生命周期事件工厂。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public class OcrEventFactory {

    private final IdGenerator idGenerator;

    /**
     * 创建 OCR 事件工厂。
     *
     * @param idGenerator ID 生成器
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public OcrEventFactory(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    /**
     * 创建 OCR 生命周期事件。
     *
     * @param request 事件创建请求
     * @return OCR 生命周期事件
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public OcrEvent create(EventCreateRequest request) {
        return new OcrEvent(
                idGenerator.newEventId(),
                request.eventType(),
                request.batchId(),
                request.documentId(),
                request.status(),
                request.stage(),
                request.progress(),
                request.metadata(),
                request.resultId(),
                request.resultSummary(),
                request.error(),
                OffsetDateTime.now()
        );
    }
}
