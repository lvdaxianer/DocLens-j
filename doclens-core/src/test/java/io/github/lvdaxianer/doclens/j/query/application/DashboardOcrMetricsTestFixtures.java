package io.github.lvdaxianer.doclens.j.query.application;

import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.batch;

import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.InMemoryBatchRepository;
import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.InMemoryDocumentJobRepository;
import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.InMemoryOcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.EmptyCallbackJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.EmptyOcrResultRepository;
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
        return new DashboardQueryService(new DashboardQueryService.Dependencies(
                new DashboardQueryService.Dependencies.Repositories(new InMemoryBatchRepository(List.of(batch())),
                        new InMemoryDocumentJobRepository(List.of()), new InMemoryOcrEventRepository(),
                        new EmptyOcrResultRepository()),
                new DashboardQueryService.Dependencies.Services(new TestDashboardOcrMetricsProvider(),
                        new EmptyCallbackJobRepository())));
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
                            "ocr_health", Map.of("active_count", 1, "queue_size", 0),
                            "llm_markdown_chunk", Map.of("active_count", 4, "queue_size", 6)))
            );
        }

        @Override
        public List<Map<String, Object>> dispatchHitNodesByBatch(String batchId) {
            return List.of(Map.of("model_key", "paddle_ocr", "node_id", "node-1", "image_count", 2L));
        }

        /**
         * 返回空文档运行中分配节点。
         *
         * @param batchId 批次 ID
         * @return 空运行中分配节点
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        @Override
        public Map<String, List<Map<String, Object>>> runningHitNodesByBatch(String batchId) {
            return Map.of();
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

    /**
     * 限定文档维度最终命中节点的测试指标提供器。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    static class ScopedDashboardOcrMetricsProvider implements DashboardOcrMetricsProvider {

        /*
         * 这个测试提供器刻意返回两个文档维度的最终命中节点。
         * 目标是验证 DashboardQueryService 不会把 batch 级命中数据
         * 误用到每个 document 的 ocr_final_hit_nodes 字段上。
         */

        /**
         * 返回空 OCR 资源，避免本用例关注无关字段。
         *
         * @return 空 OCR 资源
         * @author lvdaxianer@yeah.net
         * @date 2026-07-02
         */
        @Override
        public Map<String, Object> ocrResources() {
            return Map.of();
        }

        /**
         * 返回批次调度命中节点。
         *
         * @param batchId 批次 ID
         * @return 批次调度命中节点
         * @author lvdaxianer@yeah.net
         * @date 2026-07-02
         */
        @Override
        public List<Map<String, Object>> dispatchHitNodesByBatch(String batchId) {
            return List.of(
                    Map.of("model_key", "paddle_ocr", "node_id", "node-1", "image_count", 2L),
                    Map.of("model_key", "paddle_ocr", "node_id", "node-2", "image_count", 1L));
        }

        /**
         * 返回空文档运行中分配节点。
         *
         * @param batchId 批次 ID
         * @return 空运行中分配节点
         * @author lvdaxianer@yeah.net
         * @date 2026-07-02
         */
        @Override
        public Map<String, List<Map<String, Object>>> runningHitNodesByBatch(String batchId) {
            return Map.of();
        }

        /**
         * 返回文档最终命中节点。
         *
         * @param batchId 批次 ID
         * @return 文档最终命中节点
         * @author lvdaxianer@yeah.net
         * @date 2026-07-02
         */
        @Override
        public Map<String, List<Map<String, Object>>> finalHitNodesByBatch(String batchId) {
            return Map.of(
                    "doc-1", List.of(Map.of("model_key", "paddle_ocr", "node_id", "node-1", "image_count", 2L)),
                    "doc-2", List.of(Map.of("model_key", "paddle_ocr", "node_id", "node-2", "image_count", 1L))
            );
        }
    }

    /**
     * 提供并发容量快照的测试指标提供器。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    static class RuntimeCapacityDashboardOcrMetricsProvider extends TestDashboardOcrMetricsProvider {

        /**
         * 返回包含模型和节点并发容量的 OCR 资源快照。
         *
         * @return OCR 资源快照
         * @author lvdaxianer@yeah.net
         * @date 2026-07-02
         */
        @Override
        public Map<String, Object> ocrResources() {
            return Map.ofEntries(
                    Map.entry("models", List.of(Map.of(
                            "model_key", "paddle_ocr",
                            "model_name", "PaddleOCR",
                            "inflight_images", 6,
                            "max_concurrency", 30
                    ))),
                    Map.entry("nodes", List.of(
                            Map.of("model_key", "paddle_ocr", "node_id", "node-1", "node_name", "节点一",
                                    "inflight_images", 2, "max_concurrency", 10),
                            Map.of("model_key", "paddle_ocr", "node_id", "node-2", "node_name", "节点二",
                                    "inflight_images", 4, "max_concurrency", 20)
                    ))
            );
        }
    }

    /**
     * 提供文档级运行中分配节点的测试指标提供器。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    static class RunningDashboardOcrMetricsProvider extends TestDashboardOcrMetricsProvider {

        /**
         * 返回文档级 OCR 运行中命中节点。
         *
         * @param batchId 批次 ID
         * @return 文档级运行中命中节点
         * @author lvdaxianer@yeah.net
         * @date 2026-07-02
         */
        @Override
        public Map<String, List<Map<String, Object>>> runningHitNodesByBatch(String batchId) {
            return Map.of(
                    "doc-running", List.of(Map.of("model_key", "paddle_ocr", "node_id", "node-running",
                            "image_count", 2L)),
                    "doc-done", List.of(Map.of("model_key", "paddle_ocr", "node_id", "node-done",
                            "image_count", 1L))
            );
        }
    }

    /**
     * 提供运行中图片页任务的测试指标提供器。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    static class RunningPageTaskDashboardOcrMetricsProvider extends TestDashboardOcrMetricsProvider {

        /**
         * 返回运行中图片页任务。
         *
         * @param batchId 批次 ID
         * @return 运行中图片页任务
         * @author lvdaxianer@yeah.net
         * @date 2026-07-02
         */
        @Override
        public List<Map<String, Object>> runningPageTasksByBatch(String batchId) {
            return List.of(
                    Map.of("task_id", "task-1", "document_id", "doc-running", "page_no", 1,
                            "worker_id", "worker-a", "thread_name", "doclens-page-task-ocr-1",
                            "started_at", "2026-06-21T10:15:30+08:00", "running_ms", 1000L,
                            "model_key", "paddle_ocr", "node_id", "node-1"),
                    Map.of("task_id", "task-2", "document_id", "doc-running", "page_no", 2,
                            "worker_id", "worker-a", "thread_name", "doclens-page-task-ocr-2",
                            "started_at", "2026-06-21T10:15:31+08:00", "running_ms", 900L,
                            "model_key", "paddle_ocr", "node_id", "node-1")
            );
        }
    }

}
