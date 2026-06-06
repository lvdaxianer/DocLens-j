package com.doclens.processing.domain;

import com.doclens.shared.infrastructure.IdGenerator;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

/**
 * Factory for OCR lifecycle events.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@Component
public class OcrEventFactory {

    private final IdGenerator idGenerator;

    /**
     * Creates OCR event factory.
     *
     * @param idGenerator id generator
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public OcrEventFactory(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    /**
     * Creates an OCR lifecycle event.
     *
     * @param request event creation request
     * @return OCR lifecycle event
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
