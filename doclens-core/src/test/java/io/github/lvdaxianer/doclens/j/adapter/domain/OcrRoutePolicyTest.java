package io.github.lvdaxianer.doclens.j.adapter.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * OCR 路由策略值对象测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
class OcrRoutePolicyTest {

    /**
     * 全局负载均衡策略不应要求模型或节点。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void globalLoadBalanceDoesNotRequireModelOrNode() {
        OcrRoutePolicy policy = OcrRoutePolicy.globalLoadBalance("least-inflight");

        assertThat(policy.routingMode()).isEqualTo(OcrRoutingMode.GLOBAL_LOAD_BALANCE);
        assertThat(policy.modelKey()).isEmpty();
        assertThat(policy.nodeId()).isEmpty();
        assertThat(policy.loadBalanceStrategy()).contains("least-inflight");
    }

    /**
     * 指定 OCR 负载均衡策略必须提供模型标识。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void modelLoadBalanceRequiresModelKey() {
        assertThatThrownBy(() -> OcrRoutePolicy.modelLoadBalance("", "least-inflight"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ocr model key is required");
    }

    /**
     * 固定节点策略必须同时提供模型标识和节点标识。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void specificNodeRequiresModelAndNode() {
        assertThatThrownBy(() -> OcrRoutePolicy.specificNode("paddle_ocr", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ocr node id is required");
    }
}
