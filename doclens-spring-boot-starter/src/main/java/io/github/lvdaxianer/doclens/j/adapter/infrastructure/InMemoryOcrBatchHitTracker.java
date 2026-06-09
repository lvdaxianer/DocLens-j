package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrBatchHitTracker;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrBatchNodeHit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 进程内批次 OCR 命中节点跟踪器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public class InMemoryOcrBatchHitTracker implements OcrBatchHitTracker {

    private static final int KEY_SEGMENT_COUNT = 3;
    private static final String KEY_SEPARATOR = "|";

    private final Map<String, AtomicLong> hitCounts = new ConcurrentHashMap<>();

    /**
     * 记录某个批次已派发到指定节点的进行中图片。
     *
     * @param batchId 批次 ID
     * @param modelKey 模型标识
     * @param nodeId 节点 ID
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public void recordDispatch(String batchId, String modelKey, String nodeId) {
        hitCounts.computeIfAbsent(hitKey(batchId, modelKey, nodeId), ignored -> new AtomicLong(0L))
                .incrementAndGet();
    }

    /**
     * 记录某个批次在指定节点上的图片已完成处理。
     *
     * @param batchId 批次 ID
     * @param modelKey 模型标识
     * @param nodeId 节点 ID
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public void recordCompletion(String batchId, String modelKey, String nodeId) {
        hitCounts.computeIfPresent(hitKey(batchId, modelKey, nodeId), (ignored, counter) -> {
            long currentValue = counter.decrementAndGet();
            if (currentValue <= 0L) {
                return null;
            } else {
                return counter;
            }
        });
    }

    /**
     * 返回指定批次当前仍在处理中的节点命中快照。
     *
     * @param batchId 批次 ID
     * @return 运行时命中节点快照
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public List<OcrBatchNodeHit> snapshotByBatch(String batchId) {
        return hitCounts.entrySet().stream()
                .filter(entry -> entry.getValue().get() > 0L)
                .map(entry -> toHit(entry.getKey(), entry.getValue().get()))
                .filter(hit -> batchId.equals(hit.batchId()))
                .sorted(Comparator.comparing(OcrBatchNodeHit::nodeId))
                .toList();
    }

    /**
     * 组装批次命中键，避免维护多层并发映射。
     *
     * @param batchId 批次 ID
     * @param modelKey 模型标识
     * @param nodeId 节点 ID
     * @return 命中键
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private String hitKey(String batchId, String modelKey, String nodeId) {
        return batchId + KEY_SEPARATOR + modelKey + KEY_SEPARATOR + nodeId;
    }

    /**
     * 将命中键转换为批次命中快照。
     *
     * @param key 命中键
     * @param imageCount 图片数
     * @return 命中快照
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrBatchNodeHit toHit(String key, long imageCount) {
        String[] segments = key.split("\\|", KEY_SEGMENT_COUNT);
        return new OcrBatchNodeHit(segments[0], segments[1], segments[2], imageCount);
    }
}
