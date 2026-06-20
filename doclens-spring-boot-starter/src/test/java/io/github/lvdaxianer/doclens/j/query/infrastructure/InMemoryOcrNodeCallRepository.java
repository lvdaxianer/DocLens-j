package io.github.lvdaxianer.doclens.j.query.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCall;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallRepository;
import java.time.LocalDate;
import java.util.List;

/**
 * 查询侧测试用内存 OCR 调用记录仓储。
 *
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
final class InMemoryOcrNodeCallRepository implements OcrNodeCallRepository {

    private final List<OcrNodeCall> calls;

    /**
     * 创建内存调用记录仓储。
     *
     * @param calls 初始调用记录
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    InMemoryOcrNodeCallRepository(List<OcrNodeCall> calls) {
        this.calls = List.copyOf(calls);
    }

    /**
     * 保存 OCR 调用记录。
     *
     * @param call OCR 调用记录
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Override
    public void save(OcrNodeCall call) {
        throw new UnsupportedOperationException("test repository is read only");
    }

    /**
     * 按文档 ID 查询调用记录。
     *
     * @param documentId 文档 ID
     * @return OCR 调用记录集合
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Override
    public List<OcrNodeCall> listByDocumentId(String documentId) {
        return calls.stream().filter(call -> call.documentId().equals(documentId)).toList();
    }

    /**
     * 按批次 ID 查询调用记录。
     *
     * @param batchId 批次 ID
     * @return OCR 调用记录集合
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Override
    public List<OcrNodeCall> listByBatchId(String batchId) {
        return calls.stream().filter(call -> call.batchId().equals(batchId)).toList();
    }

    /**
     * 按节点 ID 查询最近调用记录。
     *
     * @param nodeId 节点 ID
     * @param limit 查询数量上限
     * @return OCR 调用记录集合
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Override
    public List<OcrNodeCall> listRecentByNodeId(String nodeId, int limit) {
        return calls.stream().filter(call -> call.nodeId().equals(nodeId)).limit(limit).toList();
    }

    /**
     * 按节点集合和日期查询调用记录。
     *
     * @param nodeIds 节点 ID 集合
     * @param day 日期
     * @return OCR 调用记录集合
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Override
    public List<OcrNodeCall> listByNodeIdsAndDay(List<String> nodeIds, LocalDate day) {
        // 资源指标按节点集合和自然日批量查询，测试仓储在内存中等价过滤。
        return calls.stream()
                .filter(call -> nodeIds.contains(call.nodeId()) && call.startedAt().toLocalDate().equals(day))
                .toList();
    }
}
