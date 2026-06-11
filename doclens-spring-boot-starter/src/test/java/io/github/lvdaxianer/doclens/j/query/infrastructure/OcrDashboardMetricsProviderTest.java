package io.github.lvdaxianer.doclens.j.query.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

/**
 * OCR Dashboard 资源指标提供器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
class OcrDashboardMetricsProviderTest extends OcrDashboardMetricsProviderTestSupport {

    /*
     * 该类只保留 OCR 资源面板指标。
     * 批次命中和最终命中节点分配场景，
     * 已拆到 OcrDashboardHitNodesMetricsProviderTest。
     */

    /**
     * OCR 资源指标应基于调用记录计算今日处理、平均耗时和 P95。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void ocrResourcesIncludesLatencyMetricsFromCalls() {
        InMemoryOcrNodeRepository nodeRepository = new InMemoryOcrNodeRepository(List.of(
                offlineNode(FINANCE_NODE_ID, FINANCE_NODE_NAME),
                offlineNode(INVOICE_NODE_ID, INVOICE_NODE_NAME)
        ));
        InMemoryOcrNodeCallRepository callRepository = new InMemoryOcrNodeCallRepository(List.of(
                successfulCall(callSeed("call-1", "batch-1", "doc-1", 1), FINANCE_NODE_ID, 100),
                successfulCall(callSeed("call-2", "batch-1", "doc-1", 2), FINANCE_NODE_ID, 200),
                successfulCall(callSeed("call-3", "batch-2", "doc-2", 1), INVOICE_NODE_ID, 600)
        ));
        OcrDashboardMetricsProvider provider = provider(nodeRepository, callRepository);

        Map<String, Object> resources = provider.ocrResources();

        assertThat(resources).containsEntry("healthy_node_count", 2L);
        assertResourceNodes(resources);
    }

    /**
     * 断言资源节点指标。
     *
     * @param resources 资源指标响应
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertResourceNodes(Map<String, Object> resources) {
        assertThat(resources.get("nodes")).asInstanceOf(InstanceOfAssertFactories.LIST)
                .anySatisfy(row -> assertFinanceNodeMetrics(row))
                .anySatisfy(row -> assertInvoiceNodeMetrics(row));
    }

    /**
     * 断言财务节点指标。
     *
     * @param row 节点指标行
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertFinanceNodeMetrics(Object row) {
        assertThat(row).asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsEntry("node_id", FINANCE_NODE_ID)
                .containsEntry("node_name", FINANCE_NODE_NAME)
                .containsEntry("processed_images_today", 2L)
                .containsEntry("avg_latency_ms", 150L)
                .containsEntry("p95_latency_ms", 200L);
    }

    /**
     * 断言票据节点指标。
     *
     * @param row 节点指标行
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertInvoiceNodeMetrics(Object row) {
        assertThat(row).asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsEntry("node_id", INVOICE_NODE_ID)
                .containsEntry("node_name", INVOICE_NODE_NAME)
                .containsEntry("processed_images_today", 1L)
                .containsEntry("avg_latency_ms", 600L)
                .containsEntry("p95_latency_ms", 600L);
    }
}
