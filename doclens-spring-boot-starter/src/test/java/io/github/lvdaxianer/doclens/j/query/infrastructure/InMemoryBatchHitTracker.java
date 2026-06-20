package io.github.lvdaxianer.doclens.j.query.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrBatchHitTracker;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrBatchNodeHit;
import java.util.List;

/**
 * 查询侧测试用内存批次运行时命中跟踪器。
 *
 * @param hits 命中快照
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
record InMemoryBatchHitTracker(List<OcrBatchNodeHit> hits) implements OcrBatchHitTracker {

    /**
     * 记录调度命中。
     *
     * @param batchId 批次 ID
     * @param modelKey OCR 模型 key
     * @param nodeId 节点 ID
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Override
    public void recordDispatch(String batchId, String modelKey, String nodeId) {
        throw new UnsupportedOperationException("test tracker is read only");
    }

    /**
     * 记录调度完成。
     *
     * @param batchId 批次 ID
     * @param modelKey OCR 模型 key
     * @param nodeId 节点 ID
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Override
    public void recordCompletion(String batchId, String modelKey, String nodeId) {
        throw new UnsupportedOperationException("test tracker is read only");
    }

    /**
     * 读取批次命中快照。
     *
     * @param batchId 批次 ID
     * @return 命中快照
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Override
    public List<OcrBatchNodeHit> snapshotByBatch(String batchId) {
        // 运行时命中快照只暴露当前批次，模拟生产 tracker 的批次隔离。
        return hits.stream().filter(hit -> hit.batchId().equals(batchId)).toList();
    }
}
