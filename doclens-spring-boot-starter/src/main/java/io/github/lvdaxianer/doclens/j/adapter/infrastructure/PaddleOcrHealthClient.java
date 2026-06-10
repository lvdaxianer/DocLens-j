package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;
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
    private static final String REQUEST_BODY_LOG =
            "{\"file\":\"<base64-health-check-image>\",\"fileType\":1,\"visualize\":false}";
    private static final String HEALTH_CHECK_IMAGE_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVQI12P4//8/AAX+Av6nNYGEAAAAAElFTkSuQmCC";
    private static final Pattern NATIVE_SUCCESS_PATTERN = Pattern.compile("\"errorCode\"\\s*:\\s*0");
    private static final int HTTP_SUCCESS_MIN = 200;
    private static final int HTTP_SUCCESS_MAX = 300;

    private final HttpClient httpClient;
    private final Duration timeout;

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
     * 使用 PaddleOCR 原生识别接口判断节点是否健康。
     *
     * @param node OCR 节点
     * @return 节点是否健康
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public boolean isHealthy(OcrNode node) {
        URI uri = ocrUri(node);
        long startMillis = System.currentTimeMillis();
        try {
            HttpResponse<String> response = httpClient.send(request(uri), HttpResponse.BodyHandlers.ofString());
            return handleResponse(uri, startMillis, response);
        } catch (IOException ex) {
            logFailure(uri, startMillis, ex);
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logFailure(uri, startMillis, ex);
            return false;
        }
    }

    /**
     * 构建健康检查请求。
     *
     * @param uri 健康检查 URI
     * @return HTTP 请求
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private HttpRequest request(URI uri) {
        LOGGER.debug("[服务间调用] 发起请求|PaddleOCR健康检查|{}|POST|-|body={}, headers={{Content-Type=application/json}}",
                uri, REQUEST_BODY_LOG);
        return HttpRequest.newBuilder(uri)
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody()))
                .build();
    }

    /**
     * 拼接 PaddleOCR 健康检查 URI。
     *
     * @param node OCR 节点
     * @return 健康检查 URI
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private URI ocrUri(OcrNode node) {
        return URI.create("http://" + node.host() + ":" + node.port() + OCR_PATH);
    }

    /**
     * 判断 HTTP 状态码是否成功。
     *
     * @param statusCode HTTP 状态码
     * @return 是否成功
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private boolean isSuccess(int statusCode) {
        return statusCode >= HTTP_SUCCESS_MIN && statusCode < HTTP_SUCCESS_MAX;
    }

    /**
     * 构建 PaddleOCR 原生接口探测请求体。
     *
     * @return JSON 请求体
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private String requestBody() {
        return "{\"file\":\"" + HEALTH_CHECK_IMAGE_BASE64 + "\",\"fileType\":1,\"visualize\":false}";
    }

    /**
     * 处理 PaddleOCR 健康检查响应。
     *
     * @param uri 请求 URI
     * @param startMillis 开始时间戳
     * @param response HTTP 响应
     * @return 节点是否健康
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private boolean handleResponse(URI uri, long startMillis, HttpResponse<String> response) {
        long elapsedMillis = elapsedMillis(startMillis);
        LOGGER.debug("[服务间调用] 收到响应|PaddleOCR健康检查|{}|{}|{}ms|body={}",
                uri, response.statusCode(), elapsedMillis, response.body());
        return isSuccess(response.statusCode()) && isNativeSuccess(response.body());
    }

    /**
     * 记录 PaddleOCR 健康检查异常。
     *
     * @param uri 请求 URI
     * @param startMillis 开始时间戳
     * @param ex 调用异常
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void logFailure(URI uri, long startMillis, Exception ex) {
        LOGGER.warn("[服务间调用] 调用失败|PaddleOCR健康检查|{}|-|{}ms|error={}",
                uri, elapsedMillis(startMillis), ex.getMessage(), ex);
    }

    /**
     * 计算调用耗时。
     *
     * @param startMillis 开始时间戳
     * @return 耗时毫秒数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private long elapsedMillis(long startMillis) {
        return System.currentTimeMillis() - startMillis;
    }

    /**
     * 判断 PaddleOCR 原生响应是否成功。
     *
     * @param body 响应体
     * @return 是否成功
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private boolean isNativeSuccess(String body) {
        // 原生接口会返回格式化 JSON，匹配时允许字段和值之间存在空白字符。
        return body != null && NATIVE_SUCCESS_PATTERN.matcher(body).find();
    }
}
