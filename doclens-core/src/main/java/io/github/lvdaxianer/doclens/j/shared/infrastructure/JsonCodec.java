package io.github.lvdaxianer.doclens.j.shared.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON serialization helper for persistence and HTTP form payloads.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public class JsonCodec {

    private static final TypeReference<Map<String, Object>> OBJECT_TYPE = new TypeReference<>() {
    };
    private final ObjectMapper objectMapper;

    /**
     * Creates a JSON codec.
     *
     * @param objectMapper shared Jackson mapper
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public JsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Serializes any object to JSON text.
     *
     * @param value object value
     * @return JSON text
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("failed to serialize JSON", ex);
        }
    }

    /**
     * Parses object JSON text.
     *
     * @param payload JSON text
     * @return JSON object map
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public Map<String, Object> parseObject(String payload) {
        if (payload == null || payload.isBlank()) {
            return new LinkedHashMap<>();
        } else {
            return readObject(payload);
        }
    }

    private Map<String, Object> readObject(String payload) {
        try {
            return objectMapper.readValue(payload, OBJECT_TYPE);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("invalid metadata json", ex);
        }
    }
}
