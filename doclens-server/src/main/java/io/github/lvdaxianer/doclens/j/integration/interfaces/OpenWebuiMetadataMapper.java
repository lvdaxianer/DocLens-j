package io.github.lvdaxianer.doclens.j.integration.interfaces;

import io.github.lvdaxianer.doclens.j.shared.infrastructure.JsonCodec;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Open WebUI metadata 与幂等键校验器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
@Component
public class OpenWebuiMetadataMapper {

    private static final String SOURCE_FIELD = "source";
    private static final String SOURCE_VALUE = "open-webui";
    private static final String USER_ID_FIELD = "openwebui_user_id";
    private static final String FILE_ID_FIELD = "openwebui_file_id";
    private static final String KNOWLEDGE_ID_FIELD = "openwebui_knowledge_id";
    private static final String REQUEST_ID_FIELD = "openwebui_request_id";
    private static final String IDEMPOTENCY_PREFIX_TEMPLATE = "openwebui:file:%s:hash:";

    private final JsonCodec jsonCodec;

    /**
     * 创建 Open WebUI metadata 映射器。
     *
     * @param jsonCodec JSON 编解码器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public OpenWebuiMetadataMapper(JsonCodec jsonCodec) {
        this.jsonCodec = jsonCodec;
    }

    /**
     * 解析并校验 Open WebUI metadata。
     *
     * @param metadataJson metadata JSON
     * @param identity Open WebUI 请求身份
     * @param idempotencyKey 幂等键
     * @return Open WebUI metadata
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public OpenWebuiMetadata toMetadata(
            String metadataJson,
            OpenWebuiIdentity identity,
            String idempotencyKey
    ) {
        Map<String, Object> values = jsonCodec.parseObject(metadataJson);
        requireEquals(SOURCE_FIELD, values, SOURCE_VALUE);
        String userId = requiredText(values, USER_ID_FIELD);
        String fileId = requiredText(values, FILE_ID_FIELD);
        String knowledgeId = requiredText(values, KNOWLEDGE_ID_FIELD);
        String requestId = requiredText(values, REQUEST_ID_FIELD);
        requireSameIdentity(identity, userId, requestId);
        requireIdempotencyKey(fileId, idempotencyKey);
        return new OpenWebuiMetadata(values, userId, fileId, knowledgeId, requestId);
    }

    /**
     * 校验字段值是否等于预期值。
     *
     * @param fieldName 字段名
     * @param values metadata 字段集合
     * @param expectedValue 预期字段值
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private void requireEquals(String fieldName, Map<String, Object> values, String expectedValue) {
        String actualValue = requiredText(values, fieldName);
        if (!expectedValue.equals(actualValue)) {
            throw OpenWebuiIntegrationException.badRequest(fieldName + " must be " + expectedValue);
        } else {
            // metadata 来源正确。
        }
    }

    /**
     * 获取必填 metadata 字符串字段。
     *
     * @param values metadata 字段集合
     * @param fieldName 字段名
     * @return 字段文本
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private String requiredText(Map<String, Object> values, String fieldName) {
        Object value = values.get(fieldName);
        if (value instanceof String text && StringUtils.hasText(text)) {
            return text;
        } else {
            throw OpenWebuiIntegrationException.badRequest("missing required metadata: " + fieldName);
        }
    }

    /**
     * 校验请求头身份与 metadata 身份一致。
     *
     * @param identity Open WebUI 请求身份
     * @param userId metadata 用户 ID
     * @param requestId metadata 请求 ID
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private void requireSameIdentity(OpenWebuiIdentity identity, String userId, String requestId) {
        if (!identity.userId().equals(userId)) {
            throw OpenWebuiIntegrationException.badRequest("metadata user id must match header user id");
        } else if (!identity.requestId().equals(requestId)) {
            throw OpenWebuiIntegrationException.badRequest("metadata request id must match header request id");
        } else {
            // 身份头与 metadata 一致。
        }
    }

    /**
     * 校验幂等键是否绑定 Open WebUI 文件 ID。
     *
     * @param fileId Open WebUI 文件 ID
     * @param idempotencyKey 幂等键
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private void requireIdempotencyKey(String fileId, String idempotencyKey) {
        String expectedPrefix = IDEMPOTENCY_PREFIX_TEMPLATE.formatted(fileId);
        if (!StringUtils.hasText(idempotencyKey) || !idempotencyKey.startsWith(expectedPrefix)) {
            throw OpenWebuiIntegrationException.badRequest("idempotency_key must start with " + expectedPrefix);
        } else {
            // 幂等键绑定 Open WebUI 文件 ID。
        }
    }
}
