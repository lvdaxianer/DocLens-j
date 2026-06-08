package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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
    private static final String HEALTH_PATH = "/health";
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

    @Override
    public boolean isHealthy(OcrNode node) {
        URI uri = healthUri(node);
        try {
            HttpResponse<String> response = httpClient.send(request(uri), HttpResponse.BodyHandlers.ofString());
            LOGGER.debug("[服务间调用] 收到响应|PaddleOCR健康检查|{}|{}|-|body={}",
                    uri, response.statusCode(), response.body());
            return isSuccess(response.statusCode());
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
     * @date 2026-06-08
     */
    private HttpRequest request(URI uri) {
        LOGGER.debug("[服务间调用] 发起请求|PaddleOCR健康检查|{}|GET|-|-|headers={{}}", uri);
        return HttpRequest.newBuilder(uri)
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(timeout)
                .GET()
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
    private URI healthUri(OcrNode node) {
        return URI.create("http://" + node.host() + ":" + node.port() + HEALTH_PATH);
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
}
