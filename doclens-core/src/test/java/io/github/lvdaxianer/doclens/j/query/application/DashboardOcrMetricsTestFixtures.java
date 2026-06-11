package io.github.lvdaxianer.doclens.j.query.application;

import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.batch;

import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.InMemoryBatchRepository;
import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.InMemoryDocumentJobRepository;
import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.InMemoryOcrEventRepository;
import java.util.List;
import java.util.Map;

/**
 * Dashboard OCR 指标测试夹具。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
final class DashboardOcrMetricsTestFixtures {

    /*
     * 本夹具只承载 Dashboard OCR 资源和命中节点指标。
     * 普通批次、文档、仓储桩仍放在 DashboardQueryServiceFixtures。
     * 这样指标字段变化时，只需要调整这里的 provider。
     */

    private DashboardOcrMetricsTestFixtures() {
    }

    /**
     * 创建带测试 OCR 指标的 Dashboard 服务。
     *
     * @return Dashboard 查询服务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static DashboardQueryService dashboardServiceWithOcrMetrics() {
        return new DashboardQueryService(new InMemoryBatchRepository(List.of(batch())),
                new InMemoryDocumentJobRepository(List.of()), new InMemoryOcrEventRepository(),
                new TestDashboardOcrMetricsProvider());
    }

    /**
     * 测试 OCR 指标提供器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static class TestDashboardOcrMetricsProvider implements DashboardOcrMetricsProvider {

        /*
         * 该 provider 返回固定 OCR 资源、批次调度命中和文档最终命中。
         * 固定数据让 DashboardQueryService 的读模型转换断言保持稳定，
         * 同时避免测试依赖真实 OCR 节点运行态。
         */

        @Override
        public Map<String, Object> ocrResources() {
            return Map.ofEntries(
                    Map.entry("healthy_node_count", 2L),
                    Map.entry("down_node_count", 1L),
                    Map.entry("recovering_node_count", 1L),
                    Map.entry("global_inflight_images", 7L),
                    Map.entry("busiest_node", Map.of("node_id", "node-1", "inflight_images", 5L)),
                    Map.entry("thread_pools", Map.of(
                            "ocr_request", Map.of("active_count", 2, "queue_size", 3),
                            "ocr_health", Map.of("active_count", 1, "queue_size", 0)))
            );
        }

        @Override
        public List<Map<String, Object>> dispatchHitNodesByBatch(String batchId) {
            return List.of(Map.of("model_key", "paddle_ocr", "node_id", "node-1", "image_count", 2L));
        }

        /**
         * 返回测试文档的最终分配节点。
         *
         * @param batchId 批次 ID
         * @return 最终分配节点列表
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public Map<String, List<Map<String, Object>>> finalHitNodesByBatch(String batchId) {
            return Map.of("doc-routed",
                    List.of(Map.of("model_key", "paddle_ocr", "node_id", "node-1", "image_count", 2L)));
        }
    }
}
