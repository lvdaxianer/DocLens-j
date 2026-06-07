package io.github.lvdaxianer.doclens.j.processing.domain;

import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import java.time.OffsetDateTime;

/**
 * Factory for OCR lifecycle events.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
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
