package io.github.lvdaxianer.doclens.j.query.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrBatchNodeHit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * OCR Dashboard 命中节点指标提供器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
class OcrDashboardHitNodesMetricsProviderTest extends OcrDashboardMetricsProviderTestSupport {

    /*
     * 该类只验证批次命中节点和最终节点分配。
     * 它关注 Dashboard 展示“哪个节点处理了哪些页”的事实，
     * 不再混入资源面板聚合指标。
     *
     * 批次命中节点代表实时或已完成的调度可视化，
     * 最终命中节点代表每页最终成功归属。
     * 两类指标语义不同，拆分后断言会更聚焦。
     */

    /**
     * 批次命中节点应返回节点别名，避免前端退回显示内部 ID。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void hitNodesByBatchIncludesNodeName() {
        InMemoryOcrNodeRepository nodeRepository = new InMemoryOcrNodeRepository(List.of(
                offlineNode(FINANCE_NODE_ID, FINANCE_NODE_NAME)
        ));
        InMemoryOcrNodeCallRepository callRepository = new InMemoryOcrNodeCallRepository(List.of(
                successfulCall(callSeed("call-1", "batch-1", "doc-1", 1), FINANCE_NODE_ID, 320),
                successfulCall(callSeed("call-2", "batch-1", "doc-1", 2), FINANCE_NODE_ID, 480)
        ));
        OcrDashboardMetricsProvider provider = provider(nodeRepository, callRepository);

        List<Map<String, Object>> hitNodes = provider.dispatchHitNodesByBatch("batch-1");

        assertThat(hitNodes).singleElement().satisfies(row -> assertFinanceHitNode(row, 2L));
    }

    /**
     * 批次命中节点应合并进行中的运行时命中，避免处理中遗漏尚未落库的节点。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void hitNodesByBatchMergesCompletedCallsAndRuntimeHits() {
        InMemoryOcrNodeRepository nodeRepository = twoNodeRepository();
        InMemoryOcrNodeCallRepository callRepository = new InMemoryOcrNodeCallRepository(List.of(
                successfulCall(callSeed("call-1", "batch-1", "doc-1", 1), FINANCE_NODE_ID, 320)
        ));
        InMemoryBatchHitTracker batchHitTracker = new InMemoryBatchHitTracker(List.of(
                new OcrBatchNodeHit("batch-1", DEFAULT_MODEL_KEY, FINANCE_NODE_ID, 1L),
                new OcrBatchNodeHit("batch-1", DEFAULT_MODEL_KEY, INVOICE_NODE_ID, 2L)
        ));
        OcrDashboardMetricsProvider provider = provider(nodeRepository, callRepository, batchHitTracker);

        List<Map<String, Object>> hitNodes = provider.dispatchHitNodesByBatch("batch-1");

        assertThat(hitNodes)
                .anySatisfy(row -> assertFinanceHitNode(row, 2L))
                .anySatisfy(row -> assertInvoiceHitNode(row, 2L));
    }

    /**
     * 文档最终分配节点应只统计成功节点，失败重试节点不能重复计入。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void finalHitNodesByDocumentCountsOnlySuccessfulNodePerPage() {
        InMemoryOcrNodeCallRepository callRepository = new InMemoryOcrNodeCallRepository(List.of(
                failedCall(callSeed("call-1", "batch-1", "doc-1", 1), FINANCE_NODE_ID, 100),
                successfulCall(callSeed("call-2", "batch-1", "doc-1", 1), INVOICE_NODE_ID, 280),
                successfulCall(callSeed("call-3", "batch-1", "doc-1", 2), INVOICE_NODE_ID, 320)
        ));
        OcrDashboardMetricsProvider provider = provider(twoNodeRepository(), callRepository);

        List<Map<String, Object>> finalHitNodes = provider.finalHitNodesByBatch("batch-1").get("doc-1");

        assertThat(finalHitNodes).singleElement().satisfies(row -> assertInvoiceHitNode(row, 2L));
    }

    /**
     * 文档最终分配节点应只返回当前文档自身的成功分配，不混入同批次其他文档。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void finalHitNodesByDocumentKeepsOtherDocumentsOutOfScope() {
        InMemoryOcrNodeCallRepository callRepository = new InMemoryOcrNodeCallRepository(List.of(
                successfulCall(callSeed("call-1", "batch-1", "doc-1", 1), FINANCE_NODE_ID, 180),
                successfulCall(callSeed("call-2", "batch-1", "doc-1", 2), INVOICE_NODE_ID, 220),
                successfulCall(callSeed("call-3", "batch-1", "doc-2", 1), CONTRACT_NODE_ID, 260)
        ));
        OcrDashboardMetricsProvider provider = provider(threeNodeRepository(), callRepository);

        List<Map<String, Object>> finalHitNodes = provider.finalHitNodesByBatch("batch-1").get("doc-1");

        assertThat(finalHitNodes)
                .hasSize(2)
                .allSatisfy(row -> assertThat(row).doesNotContainEntry("node_id", CONTRACT_NODE_ID));
    }

    /**
     * 断言财务节点命中指标。
     *
     * @param row 节点指标行
     * @param imageCount 图片数量
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertFinanceHitNode(Map<String, Object> row, long imageCount) {
        assertThat(row)
                .containsEntry("node_id", FINANCE_NODE_ID)
                .containsEntry("node_name", FINANCE_NODE_NAME)
                .containsEntry("image_count", imageCount);
    }

    /**
     * 断言票据节点命中指标。
     *
     * @param row 节点指标行
     * @param imageCount 图片数量
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertInvoiceHitNode(Map<String, Object> row, long imageCount) {
        assertThat(row)
                .containsEntry("node_id", INVOICE_NODE_ID)
                .containsEntry("node_name", INVOICE_NODE_NAME)
                .containsEntry("image_count", imageCount);
    }

    /**
     * 创建双节点仓储。
     *
     * @return 内存节点仓储
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private InMemoryOcrNodeRepository twoNodeRepository() {
        /*
         * 两节点仓储用于验证正常分摊和重试归属。
         * 不包含合同节点，避免跨文档隔离场景误判。
         */
        return new InMemoryOcrNodeRepository(List.of(
                offlineNode(FINANCE_NODE_ID, FINANCE_NODE_NAME),
                offlineNode(INVOICE_NODE_ID, INVOICE_NODE_NAME)
        ));
    }

    /**
     * 创建三节点仓储。
     *
     * @return 内存节点仓储
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private InMemoryOcrNodeRepository threeNodeRepository() {
        /*
         * 三节点仓储专门覆盖同批次多文档隔离。
         * 合同节点只服务 doc-2，用来证明 doc-1 不会混入其它文档。
         */
        return new InMemoryOcrNodeRepository(List.of(
                offlineNode(FINANCE_NODE_ID, FINANCE_NODE_NAME),
                offlineNode(INVOICE_NODE_ID, INVOICE_NODE_NAME),
                offlineNode(CONTRACT_NODE_ID, CONTRACT_NODE_NAME)
        ));
    }
}
