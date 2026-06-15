package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJob;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobRepository;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.JsonCodec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 回调投递 worker。
 *
 * @author lvdaxianerplus
 * @date 2026-06-15
 */
public class CallbackDeliveryWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(CallbackDeliveryWorker.class);
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final int HTTP_SUCCESS_MIN = 200;
    private static final int HTTP_SUCCESS_MAX = 300;

    private final CallbackJobRepository repository;
    private final JsonCodec jsonCodec;
    private final HttpClient httpClient;
    private final ExecutorService callbackExecutor;
    private final int batchSize;

    /**
     * 创建回调投递 worker。
     *
     * @param dependencies worker 依赖集合
     * @param options worker 配置
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public CallbackDeliveryWorker(
            Dependencies dependencies,
            Options options
    ) {
        Options safeOptions = options == null ? Options.defaults() : options;
        Dependencies safeDependencies = Objects.requireNonNull(dependencies, "callback dependencies is required");
        this.repository = safeDependencies.repository();
        this.jsonCodec = safeDependencies.jsonCodec();
        this.callbackExecutor = safeDependencies.callbackExecutor();
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(safeOptions.timeout())
                .build();
        this.batchSize = safeOptions.batchSize();
    }

    /**
     * 扫描并投递一轮待回调任务。
     *
     * @return 成功投递数量
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public int runOnce() {
        return deliverBatch(pendingJobs());
    }

    /**
     * 查询待投递任务。
     *
     * @return 待投递任务
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private List<CallbackJob> pendingJobs() {
        return repository.listPending(batchSize);
    }

    /**
     * 批量提交回调投递任务。
     *
     * @param jobs 待投递任务
     * @return 成功投递数量
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private int deliverBatch(List<CallbackJob> jobs) {
        try {
            return sumResults(callbackExecutor.invokeAll(deliveryTasks(jobs)));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("callback delivery interrupted", ex);
        }
    }

    /**
     * 创建批量回调投递任务。
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
            sendCallback(job);
            repository.markSucceeded(job.callbackJobId());
            return 1;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.warn("[第三方接口调用] 回调投递失败|DocLensCallback|{}|callbackJobId={}, callbackUrl={}, error={}",
                    job.callbackUrl(), job.callbackJobId(), job.callbackUrl(), ex.getMessage(), ex);
            throw new IllegalStateException("callback delivery interrupted", ex);
        } catch (IOException ex) {
            LOGGER.warn("[第三方接口调用] 回调投递失败|DocLensCallback|{}|callbackJobId={}, callbackUrl={}, error={}",
                    job.callbackUrl(), job.callbackJobId(), job.callbackUrl(), ex.getMessage(), ex);
            throw new IllegalStateException("callback delivery failed", ex);
        } catch (IllegalStateException ex) {
            LOGGER.warn("[第三方接口调用] 回调投递失败|DocLensCallback|{}|callbackJobId={}, callbackUrl={}, error={}",
                    job.callbackUrl(), job.callbackJobId(), job.callbackUrl(), ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * 发送回调请求。
     *
     * @param job 回调任务
     * @throws IOException 请求失败
     * @throws InterruptedException 线程中断
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private void sendCallback(CallbackJob job) throws IOException, InterruptedException {
        String requestBody = jsonCodec.toJson(job.payload());
        HttpRequest request = request(job.callbackUrl(), requestBody);
        Instant startedAt = Instant.now();
        logRequest(job, requestBody);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logResponse(job, response, startedAt);
        if (!isSuccess(response.statusCode())) {
            LOGGER.warn("[第三方接口调用] 回调响应失败|DocLensCallback|{}|statusCode={}, callbackJobId={}, body={}",
                    job.callbackUrl(), response.statusCode(), job.callbackJobId(), response.body());
            throw new IllegalStateException("callback returned HTTP " + response.statusCode());
        } else {
            // 2xx 响应说明外部 callback 已经接收。
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
     * 回调投递 worker 依赖集合。
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
         * 创建回调投递 worker 依赖集合。
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
     * 回调投递 worker 配置。
     *
     * @param timeout 回调超时时间
     * @param batchSize 单轮扫描数量
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public record Options(Duration timeout, int batchSize) {

        /**
         * 创建带安全默认值的配置。
         *
         * @author lvdaxianerplus
         * @date 2026-06-15
         */
        public Options {
            timeout = timeout == null ? Duration.ofSeconds(10) : timeout;
            batchSize = Math.max(1, batchSize);
        }

        /**
         * 创建默认配置。
         *
         * @return 默认配置
         * @author lvdaxianerplus
         * @date 2026-06-15
         */
        public static Options defaults() {
            return new Options(Duration.ofSeconds(10), 100);
        }
    }
}
