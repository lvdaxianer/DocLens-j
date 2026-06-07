package io.github.lvdaxianer.doclens.j.shared.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用于持久化和 HTTP 表单载荷的 JSON 序列化辅助类。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public class JsonCodec {

    private static final TypeReference<Map<String, Object>> OBJECT_TYPE = new TypeReference<>() {
    };
    private final ObjectMapper objectMapper;

    /**
     * 创建 JSON 编解码器。
     *
     * @param objectMapper 共享 Jackson 映射器
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public JsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将任意对象序列化为 JSON 文本。
     *
     * @param value 对象值
     * @return JSON 文本
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
     * 解析对象 JSON 文本。
     *
     * @param payload JSON 文本
     * @return JSON 对象 Map
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
