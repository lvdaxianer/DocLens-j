package io.github.lvdaxianer.doclens.j.processing.domain;

import java.util.List;

/**
 * OCR 生命周期事件仓储接口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public interface OcrEventRepository {

    /**
     * 保存事件。
     *
     * @param event OCR 事件
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    void save(OcrEvent event);

    /**
     * 批量保存事件。
     *
     * @param events OCR 事件集合
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    void saveAll(List<OcrEvent> events);

    /**
     * 根据批次 ID 列出事件。
     *
     * @param batchId 批次 ID
     * @return 有序事件集合
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    List<OcrEvent> listByBatchId(String batchId);

    /**
     * 按发生时间倒序列出最近事件。
     *
     * @param limit 最大返回数量
     * @return 最近事件集合
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    List<OcrEvent> listRecent(int limit);
}
