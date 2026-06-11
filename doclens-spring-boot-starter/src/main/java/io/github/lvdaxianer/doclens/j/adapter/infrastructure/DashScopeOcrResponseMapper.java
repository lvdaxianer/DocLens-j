package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrBlock;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * DashScope 在线 OCR 响应映射器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
final class DashScopeOcrResponseMapper {

    private static final String CHANNEL_SOURCE = "aliyun_bailian_dashscope";
    private static final String FIELD_CHOICES = "choices";
    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_CONTENT = "content";
    private static final double ONLINE_CONFIDENCE = 1D;
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    /**
     * 创建 DashScope 在线 OCR 响应映射器。
     *
     * @param objectMapper JSON 映射器
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    DashScopeOcrResponseMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将在线 OCR 响应转换为归一化 OCR 结果。
     *
     * @param request 图片 OCR 请求
     * @param responseBody 在线 OCR 响应体
     * @return 图片 OCR 结果
     * @throws IOException JSON 解析失败
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    ImageOcrResult map(ImageOcrRequest request, String responseBody) throws IOException {
        JsonNode responseJson = objectMapper.readTree(responseBody);
        OcrBlock block = block(request, responseJson);
        return ImageOcrResult.fromBlocks(request.pageNo(), rawOutput(responseJson), List.of(block), List.of());
    }

    /**
     * 构建在线 OCR 文本块。
     *
     * @param request 图片 OCR 请求
     * @param responseJson 在线 OCR JSON 响应
     * @return OCR 文本块
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private OcrBlock block(ImageOcrRequest request, JsonNode responseJson) {
        String text = responseJson.path(FIELD_CHOICES).path(0).path(FIELD_MESSAGE).path(FIELD_CONTENT).asText("");
        return new OcrBlock(request.pageNo(), text, ONLINE_CONFIDENCE, List.of(), List.of(), CHANNEL_SOURCE);
    }

    /**
     * 将原始 JSON 转为可持久化 Map。
     *
     * @param responseJson 在线 OCR JSON 响应
     * @return 原始输出 Map
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private Map<String, Object> rawOutput(JsonNode responseJson) {
        return objectMapper.convertValue(responseJson, OBJECT_MAP);
    }
}
