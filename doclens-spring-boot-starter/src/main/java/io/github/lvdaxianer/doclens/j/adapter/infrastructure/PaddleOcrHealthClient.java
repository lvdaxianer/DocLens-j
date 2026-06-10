package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PaddleOCR 健康检查客户端。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public class PaddleOcrHealthClient implements OcrHealthClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaddleOcrHealthClient.class);
    private static final String OCR_PATH = "/ocr";
    private static final String HEALTH_CHECK_IMAGE_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=";
    private static final String HEALTH_CHECK_REQUEST_BODY =
            "{\"file\":\"%s\",\"fileType\":1,\"visualize\":false}";
    private static final int HTTP_SUCCESS_MIN = 200;
    private static final int HTTP_SUCCESS_MAX = 300;

    private final HttpClient httpClient;
    private final Duration timeout;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建 PaddleOCR 健康检查客户端。
     *
     * @param timeoutSeconds 超时秒数
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public PaddleOcrHealthClient(int timeoutSeconds) {
        this.timeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(timeout)
                .build();
    }

    /**
     * 通过 PaddleOCR 原生 /ocr 接口判断节点是否健康。
     *
     * @param node OCR 节点
     * @return 是否健康
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public boolean isHealthy(OcrNode node) {
        URI uri = ocrUri(node);
        try {
            HttpResponse<String> response = httpClient.send(request(uri), HttpResponse.BodyHandlers.ofString());
            LOGGER.debug("[服务间调用] 收到响应|PaddleOCR健康检查|{}|{}|-|body={}",
                    uri, response.statusCode(), response.body());
            return isSuccess(response);
        } catch (IOException ex) {
            LOGGER.warn("[服务间调用] 调用失败|PaddleOCR健康检查|{}|-|-|error={}", uri, ex.getMessage(), ex);
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.warn("[服务间调用] 调用失败|PaddleOCR健康检查|{}|-|-|error={}", uri, ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * 构建健康检查请求。
     *
     * @param uri 健康检查 URI
     * @return HTTP 请求
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private HttpRequest request(URI uri) {
        String requestBody = healthCheckRequestBody();
        LOGGER.debug("[服务间调用] 发起请求|PaddleOCR健康检查|{}|POST|-|-|headers={{Content-Type=application/json}}, body={}",
                uri, requestBody);
        return HttpRequest.newBuilder(uri)
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    /**
     * 拼接 PaddleOCR OCR 健康探测 URI。
     *
     * @param node OCR 节点
     * @return OCR 健康探测 URI
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private URI ocrUri(OcrNode node) {
        return URI.create("http://" + node.host() + ":" + node.port() + OCR_PATH);
    }

    /**
     * 构建 PaddleOCR 原生 JSON/Base64 健康探测请求体。
     *
     * @return 健康探测请求体
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private String healthCheckRequestBody() {
        return HEALTH_CHECK_REQUEST_BODY.formatted(normalizedHealthCheckImage());
    }

    /**
     * 规范化健康探测图片 Base64 文本。
     *
     * @return 健康探测图片 Base64
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private String normalizedHealthCheckImage() {
        byte[] imageBytes = Base64.getDecoder().decode(HEALTH_CHECK_IMAGE_BASE64);
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    /**
     * 判断 HTTP 响应和 PaddleOCR 业务码是否成功。
     *
     * @param response HTTP 响应
     * @return 是否成功
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private boolean isSuccess(HttpResponse<String> response) throws IOException {
        return isHttpSuccess(response.statusCode()) && isOcrBusinessSuccess(response.body());
    }

    /**
     * 判断 HTTP 状态码是否成功。
     *
     * @param statusCode HTTP 状态码
     * @return 是否成功
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private boolean isHttpSuccess(int statusCode) {
        return statusCode >= HTTP_SUCCESS_MIN && statusCode < HTTP_SUCCESS_MAX;
    }

    /**
     * 判断 PaddleOCR 响应业务码是否成功。
     *
     * @param responseBody 响应体
     * @return 是否成功
     * @throws IOException JSON 解析失败
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private boolean isOcrBusinessSuccess(String responseBody) throws IOException {
        JsonNode body = objectMapper.readTree(responseBody);
        return body.path("errorCode").asInt(-1) == 0;
    }
}
