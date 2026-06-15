package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJob;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobStatus;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.CallbackDeliveryWorker.Dependencies;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.CallbackDeliveryWorker.Options;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.JsonCodec;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * 回调投递 worker 测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-15
 */
class CallbackDeliveryWorkerTest {

    private static final int SERVER_BACKLOG = 1;
    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-15T10:00:00+08:00");

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * worker 应将待投递任务 POST 到回调地址并标记成功。
     *
     * @throws IOException 本地 HTTP 服务启动或 JSON 解析失败
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Test
    void postsPendingCallbackPayloadAndMarksSuccess() throws IOException {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = startServer(exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            writeResponse(exchange, 200, "{\"ok\":true}");
        });

        try {
            InMemoryCallbackJobRepository repository = repositoryWithJob(endpointFor(server).toString());
            ExecutorService callbackExecutor = Executors.newSingleThreadExecutor();
            try {
                CallbackDeliveryWorker worker = new CallbackDeliveryWorker(
                        new Dependencies(repository, new JsonCodec(objectMapper), callbackExecutor),
                        new Options(Duration.ofSeconds(5), 10));

                int deliveredCount = worker.runOnce();

                assertThat(deliveredCount).isEqualTo(1);
                assertRequestBody(requestBody.get());
                assertSucceeded(repository);
            } finally {
                callbackExecutor.shutdownNow();
            }
        } finally {
            server.stop(0);
        }
    }

    /**
     * 断言回调请求体。
     *
     * @param requestBody 请求体
     * @throws IOException JSON 解析失败
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private void assertRequestBody(String requestBody) throws IOException {
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
    private void assertSucceeded(InMemoryCallbackJobRepository repository) {
        assertThat(repository.findById("callback-1")).get()
                .extracting(CallbackJob::status)
                .isEqualTo(CallbackJobStatus.SUCCESS);
    }

    /**
     * 创建包含一个待投递任务的内存仓储。
     *
     * @param callbackUrl 回调地址
     * @return 内存回调任务仓储
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private InMemoryCallbackJobRepository repositoryWithJob(String callbackUrl) {
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
    private HttpServer startServer(ThrowingExchangeHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), SERVER_BACKLOG);
        server.createContext("/callback", exchange -> handler.handle(exchange));
        server.start();
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
    private URI endpointFor(HttpServer server) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/callback");
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
    private void writeResponse(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
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
    private interface ThrowingExchangeHandler {

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

    /**
     * 回调任务内存仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private static final class InMemoryCallbackJobRepository implements CallbackJobRepository {

        private final List<CallbackJob> jobs = new ArrayList<>(1);

        /**
         * 保存回调任务。
         *
         * @param job 回调任务
         * @author lvdaxianerplus
         * @date 2026-06-15
         */
        @Override
        public void save(CallbackJob job) {
            jobs.add(job);
        }

        /**
         * 根据 ID 查询回调任务。
         *
         * @param callbackJobId 回调任务 ID
         * @return 回调任务
         * @author lvdaxianerplus
         * @date 2026-06-15
         */
        @Override
        public Optional<CallbackJob> findById(String callbackJobId) {
            return jobs.stream().filter(job -> job.callbackJobId().equals(callbackJobId)).findFirst();
        }

        /**
         * 查询待投递任务。
         *
         * @param limit 最大数量
         * @return 待投递任务
         * @author lvdaxianerplus
         * @date 2026-06-15
         */
        @Override
        public List<CallbackJob> listPending(int limit) {
            return jobs.stream().filter(job -> job.status() == CallbackJobStatus.PENDING).limit(limit).toList();
        }

        /**
         * 标记任务投递成功。
         *
         * @param callbackJobId 回调任务 ID
         * @author lvdaxianerplus
         * @date 2026-06-15
         */
        @Override
        public void markSucceeded(String callbackJobId) {
            CallbackJob job = findById(callbackJobId).orElseThrow();
            jobs.set(jobs.indexOf(job), job.markSucceeded(BASE_TIME.plusSeconds(1)));
        }

        /**
         * 标记任务投递失败。
         *
         * @param request 失败更新请求
         * @author lvdaxianerplus
         * @date 2026-06-15
         */
        @Override
        public void markFailed(io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobFailureRequest request) {
            CallbackJob job = findById(request.callbackJobId()).orElseThrow();
            jobs.set(jobs.indexOf(job), job.markFailed(request));
        }
    }
}
