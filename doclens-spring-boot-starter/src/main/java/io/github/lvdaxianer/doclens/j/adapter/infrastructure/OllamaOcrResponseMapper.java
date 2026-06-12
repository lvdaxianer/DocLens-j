package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrBlock;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ollama OCR 响应到归一化结果的映射器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
public class OllamaOcrResponseMapper {

    private static final String OLLAMA_PROVIDER = "ollama";
    private static final String MARKDOWN_FORMAT = "markdown";
    private static final String FIELD_RESPONSE = "response";
    private static final double OLLAMA_CONFIDENCE = 1D;
    private static final int RAW_OUTPUT_MIN_CAPACITY = 4;
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    /**
     * 创建 Ollama OCR 响应映射器。
     *
     * @param objectMapper JSON 映射器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public OllamaOcrResponseMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将 Ollama 响应转换为 OCR Markdown 结果。
     *
     * @param request 图片 OCR 请求
     * @param model OCR 模型名称
     * @param responseBody Ollama 响应体
     * @return 图片 OCR 结果
     * @throws IOException JSON 解析失败
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public ImageOcrResult map(ImageOcrRequest request, String model, String responseBody) throws IOException {
        JsonNode responseJson = objectMapper.readTree(responseBody);
        String markdown = responseJson.path(FIELD_RESPONSE).asText("").trim();
        if (markdown.isBlank()) {
            throw new IllegalStateException("Ollama OCR returned empty response");
        } else {
            return ImageOcrResult.fromBlocks(request.pageNo(), rawOutput(responseJson, model),
                    List.of(block(request, markdown)), List.of());
        }
    }

    /**
     * 构建 Markdown 文本块。
     *
     * @param request 图片 OCR 请求
     * @param markdown Markdown 文本
     * @return OCR 文本块
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private OcrBlock block(ImageOcrRequest request, String markdown) {
        return new OcrBlock(request.pageNo(), markdown, OLLAMA_CONFIDENCE, List.of(), List.of(), OLLAMA_PROVIDER);
    }

    /**
     * 构建包含标准 OCR 元信息的原始输出。
     *
     * @param responseJson Ollama 响应 JSON
     * @param model OCR 模型名称
     * @return 原始输出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private Map<String, Object> rawOutput(JsonNode responseJson, String model) {
        Map<String, Object> rawOutput = new LinkedHashMap<>(Math.max(RAW_OUTPUT_MIN_CAPACITY, responseJson.size()));
        rawOutput.putAll(objectMapper.convertValue(responseJson, OBJECT_MAP));
        rawOutput.put("ocr_provider", OLLAMA_PROVIDER);
        rawOutput.put("ocr_format", MARKDOWN_FORMAT);
        rawOutput.put("ocr_model", model);
        return rawOutput;
    }
}
