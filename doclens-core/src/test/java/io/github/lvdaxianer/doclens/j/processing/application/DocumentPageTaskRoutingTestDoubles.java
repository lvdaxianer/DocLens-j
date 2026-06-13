package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrBatchHitTracker;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrBatchNodeHit;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrCallIdGenerator;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrDispatchCoordinator;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodeImageExecutor;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodeSelector;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrPendingRequest;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrPendingRequestQueue;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingDependencies;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingServiceProperties;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRuntimeNodeProvider;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRuntimeNodeView;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCall;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 文档页任务路由测试替身集合。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
final class DocumentPageTaskRoutingTestDoubles {

    private static final String CALL_ID = "call-test";

    /**
     * 禁止实例化测试替身工具类。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentPageTaskRoutingTestDoubles() {
    }

    /**
     * 创建不会被测试调用的 OCR 路由依赖。
     *
     * @return OCR 路由依赖
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static OcrRoutingDependencies routingDependencies() {
        OcrRuntimeNodeProvider provider = new EmptyRuntimeNodeProvider();
        OcrNodeSelector selector = (nodes, policy) -> Optional.empty();
        OcrDispatchCoordinator coordinator = new OcrDispatchCoordinator(provider, selector,
                new EmptyPendingRequestQueue());
        return new OcrRoutingDependencies(provider, selector, coordinator, unavailableExecutor(),
                new EmptyOcrNodeCallRepository(), new FixedOcrCallIdGenerator(), new EmptyBatchHitTracker(),
                new OcrRoutingServiceProperties(OcrRoutePolicy.defaultPolicy(), 1, true));
    }

    /**
     * 创建不应被测试调用的 OCR 执行器。
     *
     * @return OCR 节点执行器
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static OcrNodeImageExecutor unavailableExecutor() {
        return (node, request) -> {
            throw new IllegalStateException("test routing service should override recognize");
        };
    }

    /**
     * 空 OCR 待派发队列。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static class EmptyPendingRequestQueue implements OcrPendingRequestQueue {

        /**
         * 忽略测试队列入队。
         *
         * @param request 待处理请求
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public void enqueue(OcrPendingRequest request) {
        }

        /**
         * 返回空测试队列。
         *
         * @return 空待处理请求
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public Optional<OcrPendingRequest> poll() {
            return Optional.empty();
        }
    }

    /**
     * 空运行时节点提供器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static class EmptyRuntimeNodeProvider implements OcrRuntimeNodeProvider {

        /**
         * 返回空节点快照。
         *
         * @return 空节点列表
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public List<OcrRuntimeNodeView> snapshot() {
            return List.of();
        }

        /**
         * 不增加测试 inflight。
         *
         * @param nodeId 节点 ID
         * @return 空节点视图
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public Optional<OcrRuntimeNodeView> incrementInflight(String nodeId) {
            return Optional.empty();
        }

        /**
         * 不减少测试 inflight。
         *
         * @param nodeId 节点 ID
         * @return 空节点视图
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public Optional<OcrRuntimeNodeView> decrementInflight(String nodeId) {
            return Optional.empty();
        }

        /**
         * 不抢占测试槽位。
         *
         * @param nodeId 节点 ID
         * @return 空节点视图
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public Optional<OcrRuntimeNodeView> tryAcquireSlot(String nodeId) {
            return Optional.empty();
        }

        /**
         * 不释放测试槽位。
         *
         * @param nodeId 节点 ID
         * @return 空节点视图
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public Optional<OcrRuntimeNodeView> releaseSlot(String nodeId) {
            return Optional.empty();
        }

        /**
         * 不增加测试排队数。
         *
         * @param nodeId 节点 ID
         * @return 空节点视图
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public Optional<OcrRuntimeNodeView> incrementQueued(String nodeId) {
            return Optional.empty();
        }

        /**
         * 不减少测试排队数。
         *
         * @param nodeId 节点 ID
         * @return 空节点视图
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public Optional<OcrRuntimeNodeView> decrementQueued(String nodeId) {
            return Optional.empty();
        }
    }

    /**
     * 空 OCR 调用记录仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static class EmptyOcrNodeCallRepository implements OcrNodeCallRepository {

        /**
         * 忽略测试调用记录。
         *
         * @param call OCR 调用记录
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public void save(OcrNodeCall call) {
        }

        /**
         * 返回空文档调用记录。
         *
         * @param documentId 文档 ID
         * @return 空调用记录列表
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public List<OcrNodeCall> listByDocumentId(String documentId) {
            return List.of();
        }

        /**
         * 返回空批次调用记录。
         *
         * @param batchId 批次 ID
         * @return 空调用记录列表
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public List<OcrNodeCall> listByBatchId(String batchId) {
            return List.of();
        }

        /**
         * 返回空节点调用记录。
         *
         * @param nodeId 节点 ID
         * @param limit 最大数量
         * @return 空调用记录列表
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public List<OcrNodeCall> listRecentByNodeId(String nodeId, int limit) {
            return List.of();
        }

        /**
         * 返回空节点日调用记录。
         *
         * @param nodeIds 节点 ID 列表
         * @param day 日期
         * @return 空调用记录列表
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public List<OcrNodeCall> listByNodeIdsAndDay(List<String> nodeIds, LocalDate day) {
            return List.of();
        }
    }

    /**
     * 固定 OCR 调用 ID 生成器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static class FixedOcrCallIdGenerator implements OcrCallIdGenerator {

        /**
         * 返回固定 OCR 调用 ID。
         *
         * @return OCR 调用 ID
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public String newOcrCallId() {
            return CALL_ID;
        }
    }

    /**
     * 空批次命中跟踪器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static class EmptyBatchHitTracker implements OcrBatchHitTracker {

        /**
         * 忽略测试派发记录。
         *
         * @param batchId 批次 ID
         * @param modelKey 模型 Key
         * @param nodeId 节点 ID
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public void recordDispatch(String batchId, String modelKey, String nodeId) {
        }

        /**
         * 忽略测试完成记录。
         *
         * @param batchId 批次 ID
         * @param modelKey 模型 Key
         * @param nodeId 节点 ID
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public void recordCompletion(String batchId, String modelKey, String nodeId) {
        }

        /**
         * 返回空批次命中快照。
         *
         * @param batchId 批次 ID
         * @return 空命中列表
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public List<OcrBatchNodeHit> snapshotByBatch(String batchId) {
            return List.of();
        }
    }
}
