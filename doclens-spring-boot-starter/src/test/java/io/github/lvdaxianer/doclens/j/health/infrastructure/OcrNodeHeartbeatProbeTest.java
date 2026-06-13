package io.github.lvdaxianer.doclens.j.health.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCreateRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrHealthClient;
import io.github.lvdaxianer.doclens.j.health.application.ModelHeartbeatProbeResult;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthFailureType;
import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/**
 * OCR 节点心跳探针测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
class OcrNodeHeartbeatProbeTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-10T12:00:00+08:00");
    private static final String NODE_ID = "node-1";
    private static final String MODEL_KEY = "paddle_ocr";

    /**
     * OCR 健康客户端返回健康时探针应返回成功。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void healthyNodeReturnsSuccess() {
        OcrNodeHeartbeatProbe probe = new OcrNodeHeartbeatProbe(node(), healthyClient());

        ModelHeartbeatProbeResult result = probe.probe();

        assertThat(result.isSuccessful()).isTrue();
    }

    /**
     * OCR 健康客户端返回不健康时探针应归一化为坏响应。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void unhealthyNodeReturnsBadResponseFailure() {
        OcrNodeHeartbeatProbe probe = new OcrNodeHeartbeatProbe(node(), unhealthyClient());

        ModelHeartbeatProbeResult result = probe.probe();

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.failureType()).isEqualTo(ModelHealthFailureType.BAD_RESPONSE);
    }

    /**
     * OCR 超时异常应归一化为心跳超时。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void timeoutExceptionReturnsTimeoutFailure() {
        OcrNodeHeartbeatProbe probe = new OcrNodeHeartbeatProbe(node(),
                failedClient(new HttpTimeoutException("timeout")));

        ModelHeartbeatProbeResult result = probe.probe();

        assertThat(result.failureType()).isEqualTo(ModelHealthFailureType.TIMEOUT);
    }

    /**
     * OCR 连接拒绝异常应归一化为连接拒绝。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void connectExceptionReturnsConnectionRefusedFailure() {
        OcrNodeHeartbeatProbe probe = new OcrNodeHeartbeatProbe(node(),
                failedClient(new ConnectException("connection refused")));

        ModelHeartbeatProbeResult result = probe.probe();

        assertThat(result.failureType()).isEqualTo(ModelHealthFailureType.CONNECTION_REFUSED);
    }

    /**
     * 未识别异常应归一化为未知失败。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void unknownExceptionReturnsUnknownFailure() {
        OcrNodeHeartbeatProbe probe = new OcrNodeHeartbeatProbe(node(),
                failedClient(new IllegalStateException("unexpected")));

        ModelHeartbeatProbeResult result = probe.probe();

        assertThat(result.failureType()).isEqualTo(ModelHealthFailureType.UNKNOWN);
    }

    /**
     * 创建抛出指定异常的 OCR 健康客户端。
     *
     * @param cause 异常原因
     * @return OCR 健康客户端
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrHealthClient failedClient(Throwable cause) {
        return current -> {
            throw new IllegalStateException("ocr heartbeat failed", cause);
        };
    }

    /**
     * 创建返回健康的 OCR 健康客户端。
     *
     * @return OCR 健康客户端
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrHealthClient healthyClient() {
        return current -> true;
    }

    /**
     * 创建返回不健康的 OCR 健康客户端。
     *
     * @return OCR 健康客户端
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrHealthClient unhealthyClient() {
        return current -> false;
    }

    /**
     * 创建 OCR 测试节点。
     *
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrNode node() {
        OcrNode created = OcrNode.create(new OcrNodeCreateRequest(NODE_ID, MODEL_KEY, NODE_ID,
                "127.0.0.1", 8080, true, true, 100, 4, BASE_TIME));
        return new OcrNode(created.id(), created.modelKey(), created.name(), created.host(), created.port(),
                created.enabled(), created.participateGlobal(), created.weight(), created.maxConcurrency(),
                OcrNodeStatus.UP, 0L, 0L, 0L, 0L, created.lastHealthAt(), created.lastSuccessAt(),
                created.lastFailureAt(), created.lastError(), created.createdAt(), created.updatedAt());
    }
}
