package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import io.github.lvdaxianer.doclens.j.processing.domain.CallbackFailureReason;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJob;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobFailureRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobRepository;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.JsonCodec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 回调投递处理器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-15
 */
public class CallbackDeliveryProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CallbackDeliveryProcessor.class);
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final int HTTP_SUCCESS_MIN = 200;
    private static final int HTTP_SUCCESS_MAX = 300;

    private final CallbackJobRepository repository;
    private final JsonCodec jsonCodec;
    private final HttpClient httpClient;
    private final ExecutorService callbackExecutor;

    /**
     * 创建回调投递处理器。
     *
     * @param dependencies 处理器依赖
     * @param timeout 回调超时时间
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public CallbackDeliveryProcessor(Dependencies dependencies, Duration timeout) {
        Dependencies safeDependencies = Objects.requireNonNull(dependencies, "callback dependencies is required");
        Duration safeTimeout = timeout == null ? Duration.ofSeconds(10) : timeout;
        this.repository = safeDependencies.repository();
        this.jsonCodec = safeDependencies.jsonCodec();
        this.callbackExecutor = safeDependencies.callbackExecutor();
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(safeTimeout)
                .build();
    }

    /**
     * 投递一批回调任务。
     *
     * @param jobs 待投递任务
     * @return 成功投递数量
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public int deliver(List<CallbackJob> jobs) {
        try {
            return sumResults(callbackExecutor.invokeAll(deliveryTasks(jobs)));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("callback delivery interrupted", ex);
        }
    }

    /**
     * 创建批量回调任务。
     *
     * @param jobs 待投递任务
     * @return 可提交任务集合
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private List<Callable<Integer>> deliveryTasks(List<CallbackJob> jobs) {
        List<Callable<Integer>> tasks = new ArrayList<>(jobs.size());
        for (CallbackJob job : jobs) {
            tasks.add(() -> deliverJob(job));
        }
        return tasks;
    }

    /**
     * 汇总批量投递结果。
     *
     * @param futures 回调投递异步结果
     * @return 成功投递数量
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private int sumResults(List<Future<Integer>> futures) {
        return futures.stream().mapToInt(this::deliveryResult).sum();
    }

    /**
     * 读取单个投递结果。
     *
     * @param future 回调投递异步结果
     * @return 成功投递数量
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private int deliveryResult(Future<Integer> future) {
        try {
            return future.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("callback delivery interrupted", ex);
        } catch (ExecutionException ex) {
            throw new IllegalStateException("callback delivery failed", ex);
        }
    }

    /**
     * 投递单个回调任务。
     *
     * @param job 回调任务
     * @return 是否成功投递
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private int deliverJob(CallbackJob job) {
        try {
            return completeDelivery(job, sendCallback(job));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.warn("[第三方接口调用] 回调投递失败|DocLensCallback|{}|callbackJobId={}, callbackUrl={}, error={}",
                    job.callbackUrl(), job.callbackJobId(), job.callbackUrl(), ex.getMessage(), ex);
            return markFailed(job, CallbackFailureReason.UNEXPECTED_EXCEPTION, failureDetail(ex));
        } catch (HttpTimeoutException ex) {
            LOGGER.warn("[第三方接口调用] 回调投递超时|DocLensCallback|{}|callbackJobId={}, callbackUrl={}, error={}",
                    job.callbackUrl(), job.callbackJobId(), job.callbackUrl(), ex.getMessage(), ex);
            return markFailed(job, CallbackFailureReason.TIMEOUT, failureDetail(ex));
        } catch (IOException ex) {
            LOGGER.warn("[第三方接口调用] 回调投递失败|DocLensCallback|{}|callbackJobId={}, callbackUrl={}, error={}",
                    job.callbackUrl(), job.callbackJobId(), job.callbackUrl(), ex.getMessage(), ex);
            return markFailed(job, CallbackFailureReason.NETWORK_ERROR, failureDetail(ex));
        } catch (RuntimeException ex) {
            LOGGER.warn("[第三方接口调用] 回调投递失败|DocLensCallback|{}|callbackJobId={}, callbackUrl={}, error={}",
                    job.callbackUrl(), job.callbackJobId(), job.callbackUrl(), ex.getMessage(), ex);
            return markFailed(job, CallbackFailureReason.UNEXPECTED_EXCEPTION, failureDetail(ex));
        }
    }

    /**
     * 根据回调响应完成投递状态转换。
     *
     * @param job 回调任务
     * @param failure 可选失败结果
     * @return 成功投递数量
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private int completeDelivery(CallbackJob job, Optional<CallbackDeliveryFailure> failure) {
        if (failure.isPresent()) {
            return markFailed(job, failure.get().reason(), failure.get().detail());
        } else {
            repository.markSucceeded(job.callbackJobId());
            return 1;
        }
    }

    /**
     * 标记回调投递失败。
     *
     * @param job 回调任务
     * @param reason 失败原因
     * @param detail 失败详情
     * @return 成功投递数量
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private int markFailed(CallbackJob job, CallbackFailureReason reason, String detail) {
        repository.markFailed(new CallbackJobFailureRequest(job.callbackJobId(), reason, detail,
                job.retryCount(), null, OffsetDateTime.now()));
        return 0;
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
    private Optional<CallbackDeliveryFailure> sendCallback(CallbackJob job) throws IOException, InterruptedException {
        String requestBody = jsonCodec.toJson(job.payload());
        HttpRequest request = request(job.callbackUrl(), requestBody);
        Instant startedAt = Instant.now();
        logRequest(job, requestBody);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logResponse(job, response, startedAt);
        if (!isSuccess(response.statusCode())) {
            LOGGER.warn("[第三方接口调用] 回调响应失败|DocLensCallback|{}|statusCode={}, callbackJobId={}, body={}",
                    job.callbackUrl(), response.statusCode(), job.callbackJobId(), response.body());
            return Optional.of(httpFailure(response));
        } else {
            // 2xx 响应说明外部 callback 已经接收。
        }
        return Optional.empty();
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
     * 构建异常失败详情。
     *
     * @param ex 异常
     * @return 失败详情
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private String failureDetail(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        } else {
            return message;
        }
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
                .timeout(httpClient.connectTimeout().orElse(Duration.ofSeconds(10)))
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
     * 回调投递处理器依赖集合。
     *
     * @param repository 回调任务仓储
     * @param jsonCodec JSON 编解码器
     * @param callbackExecutor 回调投递线程池
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public record Dependencies(
            CallbackJobRepository repository,
            JsonCodec jsonCodec,
            ExecutorService callbackExecutor
    ) {

        /**
         * 创建回调投递处理器依赖集合。
         *
         * @author lvdaxianerplus
         * @date 2026-06-15
         */
        public Dependencies {
            repository = Objects.requireNonNull(repository, "callback job repository is required");
            jsonCodec = Objects.requireNonNull(jsonCodec, "json codec is required");
            callbackExecutor = Objects.requireNonNull(callbackExecutor, "callback executor is required");
        }
    }

    /**
     * 回调投递失败结果。
     *
     * @param reason 失败原因
     * @param detail 失败详情
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private record CallbackDeliveryFailure(CallbackFailureReason reason, String detail) {
    }
}
