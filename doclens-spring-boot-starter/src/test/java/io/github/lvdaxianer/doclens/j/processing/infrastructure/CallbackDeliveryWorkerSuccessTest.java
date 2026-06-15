package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.CallbackDeliveryWorker.Options;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * 回调投递 worker 成功场景测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-15
 */
class CallbackDeliveryWorkerSuccessTest extends CallbackDeliveryWorkerTestSupport {

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

        InMemoryCallbackJobRepository repository = repositoryWithJob(endpointFor(server).toString());
        int deliveredCount = runWorker(repository, Options.defaults());

        assertThat(deliveredCount).isEqualTo(1);
        assertRequestBody(requestBody.get());
        assertSucceeded(repository);
    }

    /**
     * 手动重试应立即投递指定回调任务并标记成功。
     *
     * @throws IOException 本地 HTTP 服务启动或 JSON 解析失败
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    @Test
    void retryNowPostsCallbackPayloadAndMarksSuccess() throws IOException {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = startServer(exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            writeResponse(exchange, 200, "{\"ok\":true}");
        });

        InMemoryCallbackJobRepository repository = repositoryWithJob(endpointFor(server).toString());
        int deliveredCount = retryNow(repository, Options.defaults());

        assertThat(deliveredCount).isEqualTo(1);
        assertRequestBody(requestBody.get());
        assertSucceeded(repository);
    }
}
