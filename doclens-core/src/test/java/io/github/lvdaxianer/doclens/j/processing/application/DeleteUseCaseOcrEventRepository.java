package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档删除测试使用的内存事件仓储。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
final class DeleteUseCaseOcrEventRepository implements OcrEventRepository {

    /** 测试集合初始容量。 */
    private static final int TEST_CAPACITY = 8;
    /** OCR 事件列表。 */
    private final List<OcrEvent> events = new ArrayList<>(TEST_CAPACITY);

    /**
     * 保存 OCR 事件。
     *
     * @param event OCR 事件
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void save(OcrEvent event) {
        events.add(event);
    }

    /**
     * 批量保存 OCR 事件。
     *
     * @param events OCR 事件列表
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void saveAll(List<OcrEvent> events) {
        this.events.addAll(events);
    }

    /**
     * 查询批次下的 OCR 事件。
     *
     * @param batchId 批次 ID
     * @return OCR 事件列表
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public List<OcrEvent> listByBatchId(String batchId) {
        return events.stream().filter(event -> batchId.equals(event.batchId())).toList();
    }

    /**
     * 查询最近 OCR 事件。
     *
     * @param limit 查询上限
     * @return OCR 事件列表
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public List<OcrEvent> listRecent(int limit) {
        return events.stream().limit(limit).toList();
    }

    /**
     * 根据文档 ID 删除 OCR 事件。
     *
     * @param documentId 文档 ID
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void deleteByDocumentId(String documentId) {
        events.removeIf(event -> event.documentId().filter(documentId::equals).isPresent());
    }

    /**
     * 批量删除 OCR 事件。
     *
     * @param documentIds 文档 ID 列表
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void deleteByDocumentIds(List<String> documentIds) {
        events.removeIf(event -> event.documentId().filter(documentIds::contains).isPresent());
    }
}
