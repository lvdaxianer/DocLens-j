package io.github.lvdaxianer.doclens.j.integration.interfaces;

import java.util.Map;

/**
 * Open WebUI OCR metadata。
 *
 * @param values 原始 metadata 字段
 * @param userId Open WebUI 用户 ID
 * @param fileId Open WebUI 文件 ID
 * @param knowledgeId Open WebUI 知识库 ID
 * @param requestId Open WebUI 请求 ID
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
public record OpenWebuiMetadata(
        Map<String, Object> values,
        String userId,
        String fileId,
        String knowledgeId,
        String requestId
) {
}
