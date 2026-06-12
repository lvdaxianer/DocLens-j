package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ollama DeepSeek-OCR HTTP 客户端。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
public class OllamaOcrClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(OllamaOcrClient.class);
    public static final String DEEPSEEK_OCR_MARKDOWN_PROMPT = "<|grounding|>Convert the document to markdown.";
    private static final String GENERATE_PATH = "/api/generate";
    private static final String FIELD_MODEL = "model";
    private static final String FIELD_PROMPT = "prompt";
    private static final String FIELD_IMAGES = "images";
    private static final String FIELD_STREAM = "stream";
    private static final int ERROR_BODY_MAX_LENGTH = 500;

    private final ObjectMapper objectMapper;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final OllamaOcrResponseMapper responseMapper;

    /**
     * 创建 Ollama OCR 客户端。
     *
     * @param objectMapper JSON 映射器
     * @param timeout HTTP 超时时间
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public OllamaOcrClient(ObjectMapper objectMapper, Duration timeout) {
        this.objectMapper = objectMapper;
        this.timeout = timeout;
        this.responseMapper = new OllamaOcrResponseMapper(objectMapper);
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(timeout)
                .build();
    }

    /**
     * 通过 Ollama DeepSeek-OCR 识别单张图片。
     *
     * @param node OCR 运行时节点
     * @param request 图片 OCR 请求
     * @return 图片 OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public ImageOcrResult recognizeImage(OcrRuntimeNode node, ImageOcrRequest request) {
        try {
            String model = providerModel(node);
            String requestBody = buildRequestBody(model, request);
            HttpRequest httpRequest = buildRequest(node, requestBody);
            Instant startedAt = Instant.now();
            logRequest(node, request, model);
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            logResponse(node, response, startedAt, model);
            return parseResponse(request, model, response);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ollama OCR request interrupted", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Ollama OCR request failed", ex);
        }
    }

    /**
     * 拼接运行时节点 Ollama generate 地址。
     *
     * @param node OCR 运行时节点
     * @return Ollama generate 地址
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    URI generateUri(OcrRuntimeNode node) {
        return URI.create("http://" + node.node().host() + ":" + node.node().port() + GENERATE_PATH);
    }

    /**
     * 构建 Ollama generate 请求体。
     *
     * @param model OCR 模型名称
     * @param request 图片 OCR 请求
     * @return JSON 请求体
     * @throws IOException JSON 序列化失败
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private String buildRequestBody(String model, ImageOcrRequest request) throws IOException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put(FIELD_MODEL, model);
        body.put(FIELD_PROMPT, DEEPSEEK_OCR_MARKDOWN_PROMPT);
        body.set(FIELD_IMAGES, images(request));
        body.put(FIELD_STREAM, false);
        return objectMapper.writeValueAsString(body);
    }

    /**
     * 构建 Ollama images 字段。
     *
     * @param request 图片 OCR 请求
     * @return 图片 base64 数组
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private ArrayNode images(ImageOcrRequest request) {
        ArrayNode images = objectMapper.createArrayNode();
        images.add(Base64.getEncoder().encodeToString(request.imageContent()));
        return images;
    }

    /**
     * 构建 HTTP 请求。
     *
     * @param node OCR 运行时节点
     * @param requestBody JSON 请求体
     * @return HTTP 请求
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private HttpRequest buildRequest(OcrRuntimeNode node, String requestBody) {
        return HttpRequest.newBuilder(generateUri(node))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    /**
     * 解析 Ollama HTTP 响应。
     *
     * @param request 图片 OCR 请求
     * @param model OCR 模型名称
     * @param response HTTP 响应
     * @return 图片 OCR 结果
     * @throws IOException JSON 解析失败
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private ImageOcrResult parseResponse(
            ImageOcrRequest request,
            String model,
            HttpResponse<String> response
    ) throws IOException {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return responseMapper.map(request, model, response.body());
        } else {
            throw new IllegalStateException(buildErrorMessage(response));
        }
    }

    /**
     * 读取 Ollama OCR 模型名称。
     *
     * @param node OCR 运行时节点
     * @return 模型名称
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private String providerModel(OcrRuntimeNode node) {
        return node.node().providerModel().orElse(node.node().modelKey());
    }

    /**
     * 记录脱敏请求摘要。
     *
     * @param node OCR 运行时节点
     * @param request 图片 OCR 请求
     * @param model OCR 模型名称
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private void logRequest(OcrRuntimeNode node, ImageOcrRequest request, String model) {
        LOGGER.debug("[服务间调用] 发起请求|OllamaOCR|{}|POST|-|-|nodeId={}, model={}, fileBytes={}",
                generateUri(node), node.node().id(), model, request.imageContent().length);
    }

    /**
     * 记录脱敏响应摘要。
     *
     * @param node OCR 运行时节点
     * @param response HTTP 响应
     * @param startedAt 请求开始时间
     * @param model OCR 模型名称
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private void logResponse(OcrRuntimeNode node, HttpResponse<String> response, Instant startedAt, String model) {
        long elapsedMillis = Duration.between(startedAt, Instant.now()).toMillis();
        LOGGER.debug("[服务间调用] 收到响应|OllamaOCR|{}|{}|{}ms|nodeId={}, model={}",
                generateUri(node), response.statusCode(), elapsedMillis, node.node().id(), model);
    }

    /**
     * 构建 HTTP 失败消息。
     *
     * @param response HTTP 响应
     * @return 失败消息
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private String buildErrorMessage(HttpResponse<String> response) {
        return "Ollama OCR returned HTTP " + response.statusCode() + ": " + summarizeBody(response.body());
    }

    /**
     * 截断响应体，避免异常和日志过长。
     *
     * @param responseBody 响应体
     * @return 响应体摘要
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private String summarizeBody(String responseBody) {
        if (responseBody.length() <= ERROR_BODY_MAX_LENGTH) {
            return responseBody;
        } else {
            return responseBody.substring(0, ERROR_BODY_MAX_LENGTH);
        }
    }
}
