package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.shared.config.DocLensProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PaddleOCR 原生 HTTP API 客户端。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public class PaddleOcrNativeClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaddleOcrNativeClient.class);
    private static final int IMAGE_FILE_TYPE = 1;
    private static final int ERROR_BODY_MAX_LENGTH = 500;
    private static final String PADDLE_OCR_MODEL_KEY = "paddle_ocr";
    private static final String OCR_PATH = "/ocr";
    private static final String LEGACY_ENDPOINT_NODE_ID = "configured-endpoint";
    private static final String LEGACY_ENDPOINT_HOST = "configured";
    private static final int LEGACY_ENDPOINT_PORT = 0;

    private final DocLensProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    /**
     * 创建 PaddleOCR 原生 API 客户端。
     *
     * @param properties DocLens 配置
     * @param objectMapper JSON 映射器
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public PaddleOcrNativeClient(DocLensProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(properties.paddleOcr().timeoutSeconds()))
                .build();
    }

    /**
     * 识别单张图片。
     *
     * @param imageContent 图片字节
     * @return PaddleOCR JSON 响应
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public JsonNode recognizeImage(byte[] imageContent) {
        return recognizeImage(targetForConfiguredEndpoint(), imageContent);
    }

    /**
     * 通过运行时节点识别单张图片。
     *
     * @param node OCR 运行时节点
     * @param imageContent 图片字节
     * @return PaddleOCR JSON 响应
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public JsonNode recognizeImage(OcrRuntimeNode node, byte[] imageContent) {
        return recognizeImage(targetForNode(node), imageContent);
    }

    /**
     * 拼接运行时节点 OCR 请求地址。
     *
     * @param node OCR 运行时节点
     * @return OCR 请求 URI
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    URI ocrUri(OcrRuntimeNode node) {
        return nodeUri(node, OCR_PATH);
    }

    /**
     * 拼接运行时节点健康检查地址。
     *
     * @param node OCR 运行时节点
     * @return 健康检查 URI
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    URI healthUri(OcrRuntimeNode node) {
        return nodeUri(node, OCR_PATH);
    }

    /**
     * 识别单张图片并记录节点上下文。
     *
     * @param target 请求目标
     * @param imageContent 图片字节
     * @return PaddleOCR JSON 响应
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private JsonNode recognizeImage(RequestTarget target, byte[] imageContent) {
        try {
            String requestBody = buildRequestBody(imageContent);
            HttpRequest request = buildRequest(target.uri(), requestBody);
            Instant startedAt = Instant.now();
            logRequest(target, imageContent);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            logResponse(target, response, startedAt);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(buildErrorMessage(response));
            } else {
                return objectMapper.readTree(response.body());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("PaddleOCR native API request interrupted", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("PaddleOCR native API request failed", ex);
        }
    }

    /**
     * 构建 PaddleOCR JSON 请求体。
     *
     * @param imageContent 图片字节
     * @return JSON 请求体
     * @throws IOException JSON 序列化失败
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private String buildRequestBody(byte[] imageContent) throws IOException {
        return objectMapper.writeValueAsString(Map.of(
                "file", Base64.getEncoder().encodeToString(imageContent),
                "fileType", IMAGE_FILE_TYPE,
                "visualize", properties.paddleOcr().visualize()
        ));
    }

    /**
     * 构建固定 HTTP/1.1 的原生 API 请求。
     *
     * @param requestBody JSON 请求体
     * @return HTTP 请求
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private HttpRequest buildRequest(URI uri, String requestBody) {
        return HttpRequest.newBuilder(uri)
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(properties.paddleOcr().timeoutSeconds()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    /**
     * 记录已脱敏的 PaddleOCR 请求摘要。
     *
     * @param imageContent 图片字节
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private void logRequest(RequestTarget target, byte[] imageContent) {
        LOGGER.debug("[服务间调用] 发起请求|PaddleOCR|{}|POST|-|-|modelKey={}, nodeId={}, host={}, port={}, fileBytes={}, fileType={}, visualize={}",
                target.uri(), target.modelKey(), target.nodeId(), target.host(), target.port(),
                imageContent.length, IMAGE_FILE_TYPE, properties.paddleOcr().visualize());
    }

    /**
     * 记录 PaddleOCR 响应摘要。
     *
     * @param target 请求目标
     * @param response HTTP 响应
     * @param startedAt 请求开始时间
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private void logResponse(RequestTarget target, HttpResponse<String> response, Instant startedAt) {
        long elapsedMillis = Duration.between(startedAt, Instant.now()).toMillis();
        LOGGER.debug("[服务间调用] 收到响应|PaddleOCR|{}|{}|{}ms|modelKey={}, nodeId={}, host={}, port={}, body={}",
                target.uri(), response.statusCode(), elapsedMillis, target.modelKey(), target.nodeId(),
                target.host(), target.port(), summarizeBody(response.body()));
    }

    /**
     * 构建包含响应体摘要的错误消息。
     *
     * @param response PaddleOCR HTTP 响应
     * @return 错误消息
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private String buildErrorMessage(HttpResponse<String> response) {
        return "PaddleOCR native API returned HTTP " + response.statusCode() + ": " + summarizeBody(response.body());
    }

    /**
     * 截断响应体，避免异常消息过长。
     *
     * @param responseBody 响应体
     * @return 响应体摘要
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private String summarizeBody(String responseBody) {
        if (responseBody.length() <= ERROR_BODY_MAX_LENGTH) {
            return responseBody;
        } else {
            return responseBody.substring(0, ERROR_BODY_MAX_LENGTH);
        }
    }

    /**
     * 构建固定 endpoint 兼容请求目标。
     *
     * @return 固定 endpoint 请求目标
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private RequestTarget targetForConfiguredEndpoint() {
        return new RequestTarget(PADDLE_OCR_MODEL_KEY, LEGACY_ENDPOINT_NODE_ID, LEGACY_ENDPOINT_HOST,
                LEGACY_ENDPOINT_PORT, URI.create(properties.paddleOcr().endpoint()));
    }

    /**
     * 构建运行时节点请求目标。
     *
     * @param node OCR 运行时节点
     * @return 运行时节点请求目标
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private RequestTarget targetForNode(OcrRuntimeNode node) {
        return new RequestTarget(node.node().modelKey(), node.node().id(), node.node().host(), node.node().port(),
                ocrUri(node));
    }

    /**
     * 按节点和固定路径拼接 URI。
     *
     * @param node OCR 运行时节点
     * @param path 固定接口路径
     * @return 节点接口 URI
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private URI nodeUri(OcrRuntimeNode node, String path) {
        return URI.create("http://" + node.node().host() + ":" + node.node().port() + path);
    }

    /**
     * PaddleOCR 请求目标。
     *
     * @param modelKey OCR 模型标识
     * @param nodeId 节点 ID
     * @param host 节点主机
     * @param port 节点端口
     * @param uri 请求 URI
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private record RequestTarget(String modelKey, String nodeId, String host, int port, URI uri) {
    }
}
