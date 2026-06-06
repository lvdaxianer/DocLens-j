package com.doclens.shared.domain;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable JSON object payload represented as a map.
 *
 * @param values JSON object values
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record JsonPayload(Map<String, Object> values) {

    /**
     * Creates a JSON payload and copies input values.
     *
     * @param values JSON object values
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public JsonPayload {
        if (values == null) {
            values = new LinkedHashMap<>();
        } else {
            values = new LinkedHashMap<>(values);
        }
    }

    /**
     * Creates an empty payload.
     *
     * @return empty JSON payload
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public static JsonPayload empty() {
        return new JsonPayload(Map.of());
    }
}
