package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackFailureReason;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.CallbackDeliveryWorker.Options;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * 回调投递 worker 失败场景测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-15
 */
class CallbackDeliveryWorkerFailureTest extends CallbackDeliveryWorkerTestSupport {

    /**
     * worker 应记录 HTTP 非 2xx 失败原因。
     *
     * @throws IOException 本地 HTTP 服务启动失败
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Test
    void recordsHttpFailureReasonWhenCallbackReturnsNonSuccessStatus() throws IOException {
        HttpServer server = startServer(exchange -> writeResponse(exchange, 502, "{\"ok\":false}"));
        InMemoryCallbackJobRepository repository = repositoryWithJob(endpointFor(server).toString());

        int deliveredCount = runWorker(repository, Options.defaults());

        assertThat(deliveredCount).isZero();
        assertFailed(repository, CallbackFailureReason.HTTP_STATUS, "HTTP 502");
    }

    /**
     * worker 应记录网络失败原因。
     *
     * @throws IOException 本地端口探测失败
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Test
    void recordsNetworkFailureReasonWhenCallbackEndpointCannotConnect() throws IOException {
        InMemoryCallbackJobRepository repository = repositoryWithJob(unusedLocalEndpoint().toString());

        int deliveredCount = runWorker(repository, Options.defaults());

        assertThat(deliveredCount).isZero();
        assertFailed(repository, CallbackFailureReason.NETWORK_ERROR, "ConnectException");
    }

    /**
     * worker 应记录超时失败原因。
     *
     * @throws IOException 本地 HTTP 服务启动失败
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Test
    void recordsTimeoutFailureReasonWhenCallbackRequestTimesOut() throws IOException {
        HttpServer server = startServer(exchange -> {
            sleepSlowly();
            writeResponse(exchange, 200, "{\"ok\":true}");
        });
        InMemoryCallbackJobRepository repository = repositoryWithJob(endpointFor(server).toString());

        int deliveredCount = runWorker(repository, new Options(Duration.ofMillis(100), 10));

        assertThat(deliveredCount).isZero();
        assertFailed(repository, CallbackFailureReason.TIMEOUT, "timed out");
    }
}
