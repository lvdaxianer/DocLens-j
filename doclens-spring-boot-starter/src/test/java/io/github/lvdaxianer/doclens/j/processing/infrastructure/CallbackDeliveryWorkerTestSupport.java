package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackFailureReason;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJob;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobStatus;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.JsonCodec;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;

/**
 * 回调投递 worker 测试支撑。
 *
 * @author lvdaxianerplus
 * @date 2026-06-15
 */
abstract class CallbackDeliveryWorkerTestSupport {

    private static final int SERVER_BACKLOG = 1;
    private static final int SLOW_RESPONSE_MILLIS = 500;
    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-15T10:00:00+08:00");

    protected final ObjectMapper objectMapper = new ObjectMapper();
    private final List<HttpServer> servers = new ArrayList<>(4);

    /**
     * 清理本地 HTTP 测试服务。
     *
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @AfterEach
    void stopServers() {
        for (HttpServer server : servers) {
            server.stop(0);
        }
        servers.clear();
    }

    /**
     * 断言回调请求体。
     *
     * @param requestBody 请求体
     * @throws IOException JSON 解析失败
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    protected void assertRequestBody(String requestBody) throws IOException {
        JsonNode body = objectMapper.readTree(requestBody);
        assertThat(body.path("text").asText()).isEqualTo("ocr text");
    }

    /**
     * 断言任务已投递成功。
     *
     * @param repository 内存仓储
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    protected void assertSucceeded(InMemoryCallbackJobRepository repository) {
        assertThat(repository.findById("callback-1")).get()
                .extracting(CallbackJob::status)
                .isEqualTo(CallbackJobStatus.SUCCESS);
    }

    /**
     * 断言任务已投递失败并记录原因。
     *
     * @param repository 内存仓储
     * @param reason 失败原因
     * @param detailFragment 失败详情片段
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    protected void assertFailed(
            InMemoryCallbackJobRepository repository,
            CallbackFailureReason reason,
            String detailFragment
    ) {
        assertThat(repository.findById("callback-1")).get()
                .satisfies(job -> assertFailure(job, reason, detailFragment));
    }

    /**
     * 断言失败任务详情。
     *
     * @param job 回调任务
     * @param reason 失败原因
     * @param detailFragment 失败详情片段
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    protected void assertFailure(CallbackJob job, CallbackFailureReason reason, String detailFragment) {
        assertThat(job.status()).isEqualTo(CallbackJobStatus.FAILED);
        assertThat(job.failureReason()).contains(reason);
        assertThat(job.failureDetail()).hasValueSatisfying(detail -> assertThat(detail).contains(detailFragment));
    }

    /**
     * 执行一轮 worker 并释放线程池。
     *
     * @param repository 内存仓储
     * @param options worker 配置
     * @return 成功投递数量
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    protected int runWorker(InMemoryCallbackJobRepository repository, CallbackDeliveryWorker.Options options) {
        ExecutorService callbackExecutor = callbackExecutor();
        try {
            return worker(repository, callbackExecutor, options).runOnce();
        } finally {
            callbackExecutor.shutdownNow();
        }
    }

    /**
     * 创建回调投递 worker。
     *
     * @param repository 内存仓储
     * @param callbackExecutor 回调线程池
     * @param options worker 配置
     * @return 回调投递 worker
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    protected CallbackDeliveryWorker worker(
            InMemoryCallbackJobRepository repository,
            ExecutorService callbackExecutor,
            CallbackDeliveryWorker.Options options
    ) {
        CallbackDeliveryProcessor.Dependencies processorDependencies =
                new CallbackDeliveryProcessor.Dependencies(repository, new JsonCodec(objectMapper), callbackExecutor);
        return new CallbackDeliveryWorker(new CallbackDeliveryWorker.Dependencies(repository, processorDependencies),
                options);
    }

    /**
     * 创建包含一个待投递任务的内存仓储。
     *
     * @param callbackUrl 回调地址
     * @return 内存回调任务仓储
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    protected InMemoryCallbackJobRepository repositoryWithJob(String callbackUrl) {
        InMemoryCallbackJobRepository repository = new InMemoryCallbackJobRepository();
        repository.save(CallbackJob.create(new CallbackJobCreateRequest("callback-1", "event-1", "batch-1",
                "doc-1", callbackUrl, Map.of("text", "ocr text"), BASE_TIME)));
        return repository;
    }

    /**
     * 启动本地 HTTP 测试服务。
     *
     * @param handler 请求处理器
     * @return 本地 HTTP 测试服务
     * @throws IOException 本地端口监听失败
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    protected HttpServer startServer(ThrowingExchangeHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), SERVER_BACKLOG);
        server.createContext("/callback", exchange -> handler.handle(exchange));
        server.start();
        servers.add(server);
        return server;
    }

    /**
     * 拼接本地测试回调地址。
     *
     * @param server 本地 HTTP 测试服务
     * @return 回调地址
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    protected URI endpointFor(HttpServer server) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/callback");
    }

    /**
     * 创建未监听的本地回调地址。
     *
     * @return 未监听的本地回调地址
     * @throws IOException 本地端口探测失败
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    protected URI unusedLocalEndpoint() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return URI.create("http://127.0.0.1:" + socket.getLocalPort() + "/callback");
        }
    }

    /**
     * 创建测试回调线程池。
     *
     * @return 回调线程池
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    protected ExecutorService callbackExecutor() {
        return Executors.newSingleThreadExecutor();
    }

    /**
     * 模拟慢响应。
     *
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    protected void sleepSlowly() {
        try {
            Thread.sleep(SLOW_RESPONSE_MILLIS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 写入测试响应。
     *
     * @param exchange HTTP 交换对象
     * @param statusCode 响应状态码
     * @param responseBody 响应体
     * @throws IOException 响应写入失败
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    protected void writeResponse(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }

    /**
     * 允许抛出受检异常的 HTTP 处理器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @FunctionalInterface
    protected interface ThrowingExchangeHandler {

        /**
         * 处理 HTTP 请求。
         *
         * @param exchange HTTP 交换对象
         * @throws IOException 请求处理失败
         * @author lvdaxianerplus
         * @date 2026-06-15
         */
        void handle(HttpExchange exchange) throws IOException;
    }

}
