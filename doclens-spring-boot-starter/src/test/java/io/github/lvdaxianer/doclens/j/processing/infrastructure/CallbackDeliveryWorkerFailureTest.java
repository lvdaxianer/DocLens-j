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

    private static final int HTTP_BAD_GATEWAY = 502;
    private static final int HTTP_OK = 200;
    private static final int REQUEST_TIMEOUT_MILLIS = 100;
    private static final int NO_RETRY_LIMIT = 0;
    private static final int TEST_BATCH_SIZE = 10;
    private static final int SINGLE_RETRY_LIMIT = 1;

    /**
     * worker 应记录 HTTP 非 2xx 失败原因。
     *
     * @throws IOException 本地 HTTP 服务启动失败
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Test
    void recordsHttpFailureReasonWhenCallbackReturnsNonSuccessStatus() throws IOException {
        HttpServer server = startServer(exchange -> writeResponse(exchange, HTTP_BAD_GATEWAY, "{\"ok\":false}"));
        InMemoryCallbackJobRepository repository = repositoryWithJob(endpointFor(server).toString());

        int deliveredCount = runWorker(repository, terminalFailureOptions());

        assertThat(deliveredCount).isZero();
        assertFailed(repository, CallbackFailureReason.HTTP_STATUS, "HTTP 502");
    }

    /**
     * worker 应在未达到上限时调度下一次重试。
     *
     * @throws IOException 本地 HTTP 服务启动失败
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Test
    void schedulesRetryWhenFailureOccursBelowRetryLimit() throws IOException {
        HttpServer server = startServer(exchange -> writeResponse(exchange, HTTP_BAD_GATEWAY, "{\"ok\":false}"));
        InMemoryCallbackJobRepository repository = repositoryWithJob(endpointFor(server).toString());

        int deliveredCount = runWorker(repository, Options.defaults());

        assertThat(deliveredCount).isZero();
        assertRetrying(repository, CallbackFailureReason.HTTP_STATUS);
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

        int deliveredCount = runWorker(repository, terminalFailureOptions());

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
            writeResponse(exchange, HTTP_OK, "{\"ok\":true}");
        });
        InMemoryCallbackJobRepository repository = repositoryWithJob(endpointFor(server).toString());

        int deliveredCount = runWorker(repository, timeoutTerminalFailureOptions());

        assertThat(deliveredCount).isZero();
        assertFailed(repository, CallbackFailureReason.TIMEOUT, "timed out");
    }

    /**
     * worker 应在重试到期后再次投递并在达到上限后标记终态失败。
     *
     * @throws IOException 本地 HTTP 服务启动失败
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Test
    void marksTerminalFailureWhenDueRetryReachesLimit() throws IOException {
        HttpServer server = startServer(exchange -> writeResponse(exchange, HTTP_BAD_GATEWAY, "{\"ok\":false}"));
        InMemoryCallbackJobRepository repository = repositoryWithJob(endpointFor(server).toString());

        runWorker(repository, singleRetryOptions());
        int deliveredCount = runWorker(repository, singleRetryOptions());

        assertThat(deliveredCount).isZero();
        assertFailed(repository, CallbackFailureReason.HTTP_STATUS, "HTTP 502");
    }

    /**
     * 创建终态失败配置。
     *
     * @return 终态失败配置
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private Options terminalFailureOptions() {
        return new Options(Duration.ofSeconds(10), TEST_BATCH_SIZE, NO_RETRY_LIMIT, Duration.ZERO);
    }

    /**
     * 创建超时终态失败配置。
     *
     * @return 超时终态失败配置
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private Options timeoutTerminalFailureOptions() {
        return new Options(Duration.ofMillis(REQUEST_TIMEOUT_MILLIS), TEST_BATCH_SIZE, NO_RETRY_LIMIT,
                Duration.ZERO);
    }

    /**
     * 创建单次重试配置。
     *
     * @return 单次重试配置
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private Options singleRetryOptions() {
        return new Options(Duration.ofSeconds(10), TEST_BATCH_SIZE, SINGLE_RETRY_LIMIT, Duration.ZERO);
    }
}
