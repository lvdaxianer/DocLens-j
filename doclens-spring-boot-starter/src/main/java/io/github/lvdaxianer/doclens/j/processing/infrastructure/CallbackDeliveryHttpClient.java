package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import io.github.lvdaxianer.doclens.j.processing.domain.CallbackFailureReason;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJob;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.JsonCodec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 回调投递 HTTP 客户端。
 *
 * @author lvdaxianerplus
 * @date 2026-06-15
 */
final class CallbackDeliveryHttpClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CallbackDeliveryHttpClient.class);
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final int HTTP_SUCCESS_MIN = 200;
    private static final int HTTP_SUCCESS_MAX = 300;
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;

    private final JsonCodec jsonCodec;
    private final HttpClient httpClient;

    /**
     * 创建回调投递 HTTP 客户端。
     *
     * @param jsonCodec JSON 编解码器
     * @param timeout 回调超时时间
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    CallbackDeliveryHttpClient(JsonCodec jsonCodec, Duration timeout) {
        Duration safeTimeout = timeout == null ? Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS) : timeout;
        this.jsonCodec = jsonCodec;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(safeTimeout)
                .build();
    }

    /**
     * 发送回调请求。
     *
     * @param job 回调任务
     * @return 可选失败结果
     * @throws IOException 请求失败
     * @throws InterruptedException 线程中断
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    Optional<CallbackDeliveryFailure> sendCallback(CallbackJob job) throws IOException, InterruptedException {
        String requestBody = jsonCodec.toJson(job.payload());
        HttpRequest request = request(job.callbackUrl(), requestBody);
        Instant startedAt = Instant.now();
        logRequest(job, requestBody);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logResponse(job, response, startedAt);
        return responseFailure(job, response);
    }

    /**
     * 构建回调请求。
     *
     * @param callbackUrl 回调地址
     * @param requestBody 请求体
     * @return HTTP 请求
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private HttpRequest request(String callbackUrl, String requestBody) {
        return HttpRequest.newBuilder(URI.create(callbackUrl))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(httpClient.connectTimeout().orElse(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS)))
                .header(HEADER_CONTENT_TYPE, APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    /**
     * 记录回调请求。
     *
     * @param job 回调任务
     * @param requestBody 请求体
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private void logRequest(CallbackJob job, String requestBody) {
        LOGGER.debug("[第三方接口调用] 发起请求|DocLensCallback|{}|POST|Content-Type={}|-|callbackJobId={}, callbackUrl={}, body={}",
                job.callbackUrl(), APPLICATION_JSON, job.callbackJobId(), job.callbackUrl(), requestBody);
    }

    /**
     * 记录回调响应。
     *
     * @param job 回调任务
     * @param response HTTP 响应
     * @param startedAt 请求开始时间
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private void logResponse(CallbackJob job, HttpResponse<String> response, Instant startedAt) {
        long elapsedMillis = Duration.between(startedAt, Instant.now()).toMillis();
        LOGGER.debug("[第三方接口调用] 收到响应|DocLensCallback|{}|{}|{}ms|callbackJobId={}, body={}",
                job.callbackUrl(), response.statusCode(), elapsedMillis, job.callbackJobId(), response.body());
    }

    /**
     * 根据 HTTP 响应生成失败结果。
     *
     * @param job 回调任务
     * @param response HTTP 响应
     * @return 可选失败结果
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private Optional<CallbackDeliveryFailure> responseFailure(CallbackJob job, HttpResponse<String> response) {
        // 非 2xx 响应需要记录 HTTP 状态失败原因。
        if (!isSuccess(response.statusCode())) {
            LOGGER.warn("[第三方接口调用] 回调响应失败|DocLensCallback|{}|statusCode={}, callbackJobId={}, body={}",
                    job.callbackUrl(), response.statusCode(), job.callbackJobId(), response.body());
            return Optional.of(httpFailure(response));
        } else {
            // 2xx 响应说明外部 callback 已接收，无失败结果。
            return Optional.empty();
        }
    }

    /**
     * 创建 HTTP 状态失败结果。
     *
     * @param response HTTP 响应
     * @return 回调投递失败结果
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private CallbackDeliveryFailure httpFailure(HttpResponse<String> response) {
        return new CallbackDeliveryFailure(CallbackFailureReason.HTTP_STATUS,
                "HTTP " + response.statusCode() + ": " + response.body());
    }

    /**
     * 判断 HTTP 状态码是否成功。
     *
     * @param statusCode HTTP 状态码
     * @return 是否成功
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private boolean isSuccess(int statusCode) {
        return statusCode >= HTTP_SUCCESS_MIN && statusCode < HTTP_SUCCESS_MAX;
    }

    /**
     * 回调投递失败结果。
     *
     * @param reason 失败原因
     * @param detail 失败详情
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    record CallbackDeliveryFailure(CallbackFailureReason reason, String detail) {
    }
}
