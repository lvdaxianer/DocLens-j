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
        try {
            String requestBody = buildRequestBody(imageContent);
            HttpRequest request = buildRequest(requestBody);
            Instant startedAt = Instant.now();
            logRequest(imageContent);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            logResponse(response, startedAt);
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
    private HttpRequest buildRequest(String requestBody) {
        return HttpRequest.newBuilder(URI.create(properties.paddleOcr().endpoint()))
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
    private void logRequest(byte[] imageContent) {
        LOGGER.debug("[服务间调用] 发起请求|PaddleOCR|{}|POST|-|-|fileBytes={}, fileType={}, visualize={}",
                properties.paddleOcr().endpoint(), imageContent.length, IMAGE_FILE_TYPE,
                properties.paddleOcr().visualize());
    }

    /**
     * 记录 PaddleOCR 响应摘要。
     *
     * @param response HTTP 响应
     * @param startedAt 请求开始时间
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private void logResponse(HttpResponse<String> response, Instant startedAt) {
        long elapsedMillis = Duration.between(startedAt, Instant.now()).toMillis();
        LOGGER.debug("[服务间调用] 收到响应|PaddleOCR|{}|{}|{}ms|body={}",
                properties.paddleOcr().endpoint(), response.statusCode(), elapsedMillis,
                summarizeBody(response.body()));
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
}
