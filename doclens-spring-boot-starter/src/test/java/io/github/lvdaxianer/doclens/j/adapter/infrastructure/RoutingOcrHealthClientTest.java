package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCreateRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeDeploymentType;
import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/**
 * 路由型 OCR 健康检查客户端测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
class RoutingOcrHealthClientTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-11T10:00:00+08:00");
    private static final URI TEST_ENDPOINT = URI.create("http://127.0.0.1:1/compatible-mode/v1/chat/completions");
    private static final String PADDLE_MODEL_KEY = "paddle_ocr";
    private static final String ONLINE_CHANNEL_KEY = "aliyun_bailian_dashscope";
    private static final String ONLINE_PROVIDER_MODEL = "qwen-vl-ocr-2025-11-20";
    private static final String TEST_CREDENTIAL = "sk-test";
    private static final String OFFLINE_HOST = "127.0.0.1";
    private static final int OFFLINE_PORT = 18081;
    private static final int ONLINE_PORT_UNUSED = 0;
    private static final int DEFAULT_WEIGHT = 100;
    private static final int DEFAULT_MAX_CONCURRENCY = 4;
    private static final int TEST_TIMEOUT_SECONDS = 1;

    /**
     * 在线 OCR 节点应走在线权限探测，不能误用 PaddleOCR 原生 /ocr 探测。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void onlineNodeUsesOnlinePermissionProbeOnly() {
        RecordingOfflineHealthClient offlineClient = new RecordingOfflineHealthClient();
        RecordingDashScopeOnlineOcrClient onlineClient = new RecordingDashScopeOnlineOcrClient(true);
        RoutingOcrHealthClient routingClient = new RoutingOcrHealthClient(offlineClient, onlineClient);

        boolean healthy = routingClient.isHealthy(onlineNode());

        assertThat(healthy).isTrue();
        assertThat(offlineClient.callCount()).isZero();
        assertThat(onlineClient.callCount()).isEqualTo(1);
    }

    /**
     * 离线 PaddleOCR 节点应走原生健康客户端，不调用在线 OCR 权限探测。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void offlinePaddleNodeUsesOfflineHealthClientOnly() {
        RecordingOfflineHealthClient offlineClient = new RecordingOfflineHealthClient();
        RecordingDashScopeOnlineOcrClient onlineClient = new RecordingDashScopeOnlineOcrClient(true);
        RoutingOcrHealthClient routingClient = new RoutingOcrHealthClient(offlineClient, onlineClient);

        boolean healthy = routingClient.isHealthy(offlineNode());

        assertThat(healthy).isTrue();
        assertThat(offlineClient.callCount()).isEqualTo(1);
        assertThat(onlineClient.callCount()).isZero();
    }

    /**
     * 创建在线 OCR 节点。
     *
     * @return 在线 OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private OcrNode onlineNode() {
        return OcrNode.create(new OcrNodeCreateRequest("node-online", PADDLE_MODEL_KEY, OcrNodeDeploymentType.ONLINE,
                "在线 OCR", "", ONLINE_PORT_UNUSED, ONLINE_CHANNEL_KEY, ONLINE_PROVIDER_MODEL, TEST_CREDENTIAL, true,
                true, true, DEFAULT_WEIGHT, DEFAULT_MAX_CONCURRENCY, BASE_TIME));
    }

    /**
     * 创建离线 PaddleOCR 节点。
     *
     * @return 离线 PaddleOCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private OcrNode offlineNode() {
        return OcrNode.create(new OcrNodeCreateRequest("node-offline", PADDLE_MODEL_KEY, "PaddleOCR",
                OFFLINE_HOST, OFFLINE_PORT, true, true, DEFAULT_WEIGHT, DEFAULT_MAX_CONCURRENCY, BASE_TIME));
    }

    /**
     * 记录调用次数的离线健康客户端。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static class RecordingOfflineHealthClient implements OcrHealthClient {

        private int callCount;

        /**
         * 判断 OCR 节点是否健康，并记录离线健康探测调用次数。
         *
         * @param node OCR 节点
         * @return 是否健康
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public boolean isHealthy(OcrNode node) {
            callCount++;
            return true;
        }

        /**
         * 返回离线健康探测调用次数。
         *
         * @return 调用次数
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        int callCount() {
            return callCount;
        }
    }

    /**
     * 记录调用次数的在线 OCR 客户端。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static class RecordingDashScopeOnlineOcrClient extends DashScopeOnlineOcrClient {

        private final boolean healthy;
        private int callCount;

        /**
         * 创建记录调用次数的在线 OCR 客户端。
         *
         * @param healthy 固定健康结果
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        RecordingDashScopeOnlineOcrClient(boolean healthy) {
            super(new ObjectMapper(), TEST_ENDPOINT, Duration.ofSeconds(TEST_TIMEOUT_SECONDS));
            this.healthy = healthy;
        }

        /**
         * 判断在线 OCR 是否具备执行权限，并记录在线探测调用次数。
         *
         * @param node OCR 运行时节点
         * @return 是否具备执行权限
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        boolean hasExecutionPermission(OcrRuntimeNode node) {
            callCount++;
            return healthy;
        }

        /**
         * 返回在线权限探测调用次数。
         *
         * @return 调用次数
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        int callCount() {
            return callCount;
        }
    }
}
