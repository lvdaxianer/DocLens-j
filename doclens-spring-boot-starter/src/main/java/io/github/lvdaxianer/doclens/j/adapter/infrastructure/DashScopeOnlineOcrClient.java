package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrBlock;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DashScope OpenAI compatible 在线 OCR 客户端。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public class DashScopeOnlineOcrClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashScopeOnlineOcrClient.class);
    public static final URI DEFAULT_ENDPOINT = URI.create(
            "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions");
    private static final String CHANNEL_SOURCE = "aliyun_bailian_dashscope";
    private static final String OCR_PROMPT = "请仅输出图像中的文本内容。";
    private static final String ROLE_USER = "user";
    private static final String CONTENT_TYPE_TEXT = "text";
    private static final String CONTENT_TYPE_IMAGE_URL = "image_url";
    private static final String MIME_IMAGE_JPEG = "image/jpeg";
    private static final String MIME_IMAGE_PNG = "image/png";
    private static final String MIME_IMAGE_WEBP = "image/webp";
    private static final int ERROR_BODY_MAX_LENGTH = 500;
    private static final double ONLINE_CONFIDENCE = 1D;
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final URI endpoint;
    private final Duration timeout;
    private final HttpClient httpClient;

    /**
     * 创建 DashScope compatible 在线 OCR 客户端。
     *
     * @param objectMapper JSON 映射器
     * @param endpoint 在线 OCR endpoint
     * @param timeout HTTP 请求超时时间
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public DashScopeOnlineOcrClient(ObjectMapper objectMapper, URI endpoint, Duration timeout) {
        this.objectMapper = objectMapper;
        this.endpoint = endpoint;
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(timeout)
                .build();
    }

    /**
     * 通过 DashScope compatible API 识别单张图片。
     *
     * @param node OCR 运行时节点
     * @param request 图片 OCR 请求
     * @return 图片 OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public ImageOcrResult recognizeImage(OcrRuntimeNode node, ImageOcrRequest request) {
        try {
            String requestBody = buildRequestBody(node, request);
            HttpRequest httpRequest = buildRequest(node, requestBody);
            Instant startedAt = Instant.now();
            logRequest(node, request);
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            logResponse(node, response, startedAt);
            return parseResponse(node, request, response);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("DashScope online OCR request interrupted", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("DashScope online OCR request failed", ex);
        }
    }

    /**
     * 构建在线 OCR HTTP 请求。
     *
     * @param node OCR 运行时节点
     * @param requestBody JSON 请求体
     * @return HTTP 请求
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private HttpRequest buildRequest(OcrRuntimeNode node, String requestBody) {
        String credential = node.node().credentialRef()
                .orElseThrow(() -> new IllegalStateException("online OCR credential is not configured"));
        return HttpRequest.newBuilder(endpoint)
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(timeout)
                .header("Authorization", "Bearer " + credential)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    /**
     * 构建 DashScope compatible chat completions 请求体。
     *
     * @param node OCR 运行时节点
     * @param request 图片 OCR 请求
     * @return JSON 请求体
     * @throws IOException JSON 序列化失败
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private String buildRequestBody(OcrRuntimeNode node, ImageOcrRequest request) throws IOException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", providerModel(node));
        body.set("messages", messages(request));
        return objectMapper.writeValueAsString(body);
    }

    /**
     * 构建包含固定 OCR 提示词和图片内容的消息。
     *
     * @param request 图片 OCR 请求
     * @return compatible messages
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private ArrayNode messages(ImageOcrRequest request) {
        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", ROLE_USER);
        userMessage.set("content", messageContent(request));
        messages.add(userMessage);
        return messages;
    }

    /**
     * 构建多模态消息内容。
     *
     * @param request 图片 OCR 请求
     * @return 多模态消息内容
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private ArrayNode messageContent(ImageOcrRequest request) {
        ArrayNode content = objectMapper.createArrayNode();
        content.add(textPart());
        content.add(imagePart(request));
        return content;
    }

    /**
     * 构建固定文本提示词片段。
     *
     * @return 文本提示词片段
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private ObjectNode textPart() {
        ObjectNode textPart = objectMapper.createObjectNode();
        textPart.put("type", CONTENT_TYPE_TEXT);
        textPart.put(CONTENT_TYPE_TEXT, OCR_PROMPT);
        return textPart;
    }

    /**
     * 构建 base64 data URL 图片片段。
     *
     * @param request 图片 OCR 请求
     * @return 图片片段
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private ObjectNode imagePart(ImageOcrRequest request) {
        ObjectNode imagePart = objectMapper.createObjectNode();
        ObjectNode imageUrl = objectMapper.createObjectNode();
        imagePart.put("type", CONTENT_TYPE_IMAGE_URL);
        imageUrl.put("url", dataUrl(request));
        imagePart.set(CONTENT_TYPE_IMAGE_URL, imageUrl);
        return imagePart;
    }

    /**
     * 构建图片 data URL。
     *
     * @param request 图片 OCR 请求
     * @return 图片 data URL
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private String dataUrl(ImageOcrRequest request) {
        String encodedImage = Base64.getEncoder().encodeToString(request.imageContent());
        return "data:" + mimeType(request.fileName()) + ";base64," + encodedImage;
    }

    /**
     * 解析 DashScope compatible 响应。
     *
     * @param node OCR 运行时节点
     * @param request 图片 OCR 请求
     * @param response HTTP 响应
     * @return 图片 OCR 结果
     * @throws IOException JSON 解析失败
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private ImageOcrResult parseResponse(
            OcrRuntimeNode node,
            ImageOcrRequest request,
            HttpResponse<String> response
    ) throws IOException {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonNode responseJson = objectMapper.readTree(response.body());
            return toResult(request, responseJson);
        } else {
            throw new IllegalStateException(buildErrorMessage(node, response));
        }
    }

    /**
     * 将在线 OCR 响应转换为归一化 OCR 结果。
     *
     * @param request 图片 OCR 请求
     * @param responseJson 在线 OCR JSON 响应
     * @return 图片 OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private ImageOcrResult toResult(ImageOcrRequest request, JsonNode responseJson) {
        String text = responseJson.path("choices").path(0).path("message").path("content").asText("");
        OcrBlock block = new OcrBlock(request.pageNo(), text, ONLINE_CONFIDENCE, List.of(), List.of(),
                CHANNEL_SOURCE);
        return ImageOcrResult.fromBlocks(request.pageNo(), rawOutput(responseJson), List.of(block), List.of());
    }

    /**
     * 将原始 JSON 转为可持久化 Map。
     *
     * @param responseJson 在线 OCR JSON 响应
     * @return 原始输出 Map
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private Map<String, Object> rawOutput(JsonNode responseJson) {
        return objectMapper.convertValue(responseJson, OBJECT_MAP);
    }

    /**
     * 读取在线 OCR 模型名称。
     *
     * @param node OCR 运行时节点
     * @return 在线 OCR 模型名称
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private String providerModel(OcrRuntimeNode node) {
        return node.node().providerModel()
                .orElseThrow(() -> new IllegalStateException("online OCR provider model is not configured"));
    }

    /**
     * 根据文件名推断图片 MIME 类型。
     *
     * @param fileName 文件名
     * @return MIME 类型
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private String mimeType(String fileName) {
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return MIME_IMAGE_JPEG;
        } else if (lowerName.endsWith(".webp")) {
            return MIME_IMAGE_WEBP;
        } else {
            return MIME_IMAGE_PNG;
        }
    }

    /**
     * 记录脱敏后的在线 OCR 请求摘要。
     *
     * @param node OCR 运行时节点
     * @param request 图片 OCR 请求
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private void logRequest(OcrRuntimeNode node, ImageOcrRequest request) {
        LOGGER.debug("[第三方接口调用] 发起请求|DashScopeOCR|{}|POST|-|-|nodeId={}, model={}, fileBytes={}",
                endpoint, node.node().id(), providerModel(node), request.imageContent().length);
    }

    /**
     * 记录脱敏后的在线 OCR 响应摘要。
     *
     * @param node OCR 运行时节点
     * @param response HTTP 响应
     * @param startedAt 请求开始时间
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private void logResponse(OcrRuntimeNode node, HttpResponse<String> response, Instant startedAt) {
        long elapsedMillis = Duration.between(startedAt, Instant.now()).toMillis();
        LOGGER.debug("[第三方接口调用] 收到响应|DashScopeOCR|{}|{}|{}ms|nodeId={}, model={}, body={}",
                endpoint, response.statusCode(), elapsedMillis, node.node().id(), providerModel(node),
                summarizeBody(node, response.body()));
    }

    /**
     * 构建不包含请求密钥的错误消息。
     *
     * @param node OCR 运行时节点
     * @param response HTTP 响应
     * @return 错误消息
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private String buildErrorMessage(OcrRuntimeNode node, HttpResponse<String> response) {
        return "DashScope online OCR returned HTTP " + response.statusCode() + ": "
                + summarizeBody(node, response.body());
    }

    /**
     * 截断响应体，避免日志和错误提示过长。
     *
     * @param node OCR 运行时节点
     * @param responseBody 响应体
     * @return 响应体摘要
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private String summarizeBody(OcrRuntimeNode node, String responseBody) {
        String sanitizedBody = sanitizeBody(node, responseBody);
        if (sanitizedBody.length() <= ERROR_BODY_MAX_LENGTH) {
            return sanitizedBody;
        } else {
            return sanitizedBody.substring(0, ERROR_BODY_MAX_LENGTH);
        }
    }

    /**
     * 对第三方响应体中可能回显的密钥做脱敏。
     *
     * @param node OCR 运行时节点
     * @param responseBody 响应体
     * @return 脱敏后的响应体
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private String sanitizeBody(OcrRuntimeNode node, String responseBody) {
        if (node.node().credentialRef().isPresent()) {
            return responseBody.replace(node.node().credentialRef().get(), "***");
        } else {
            return responseBody;
        }
    }
}
