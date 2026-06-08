package io.github.lvdaxianer.doclens.j.adapter.domain;

import java.util.List;

/**
 * 图片级 OCR 调用记录仓储接口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public interface OcrNodeCallRepository {

    /**
     * 保存图片级 OCR 调用记录。
     *
     * @param call OCR 调用记录
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    void save(OcrNodeCall call);

    /**
     * 按文档 ID 查询 OCR 调用记录。
     *
     * @param documentId 文档 ID
     * @return OCR 调用记录集合
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    List<OcrNodeCall> listByDocumentId(String documentId);

    /**
     * 按批次 ID 查询 OCR 调用记录。
     *
     * @param batchId 批次 ID
     * @return OCR 调用记录集合
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    List<OcrNodeCall> listByBatchId(String batchId);

    /**
     * 按节点 ID 查询最近 OCR 调用记录。
     *
     * @param nodeId OCR 节点 ID
     * @param limit 最大返回数量
     * @return OCR 调用记录集合
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    List<OcrNodeCall> listRecentByNodeId(String nodeId, int limit);
}
