package com.doclens.processing.application;

import com.doclens.processing.domain.DocumentJob;
import java.util.Map;

/**
 * Parameter object for building OCR document events.
 *
 * @param document document job snapshot
 * @param eventType OCR event type
 * @param progress progress payload
 * @param detail event detail payload
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record DocumentEventPlan(
        DocumentJob document,
        String eventType,
        Map<String, Object> progress,
        Map<String, Object> detail
) {
}
