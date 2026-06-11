package io.github.lvdaxianer.doclens.j.processing.application;

import static io.github.lvdaxianer.doclens.j.processing.application.BatchProcessingUseCaseTestSupport.TEST_DOCUMENT_CAPACITY;
import static io.github.lvdaxianer.doclens.j.processing.application.BatchProcessingUseCaseTestSupport.TEST_EVENT_CAPACITY;

import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 批次处理事件类内存仓储集合。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
final class BatchProcessingUseCaseEventRepositories {

    /**
     * 禁止实例化事件类内存仓储集合。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private BatchProcessingUseCaseEventRepositories() {
    }
}

/**
 * 内存 OCR 事件仓储。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
class InMemoryOcrEventRepository implements OcrEventRepository {

    private final List<OcrEvent> events = new ArrayList<>(TEST_EVENT_CAPACITY);

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
     * @param events OCR 事件集合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void saveAll(List<OcrEvent> events) {
        this.events.addAll(events);
    }

    /**
     * 按批次 ID 查询 OCR 事件。
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
     * @param limit 最大数量
     * @return OCR 事件列表
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public List<OcrEvent> listRecent(int limit) {
        return events.stream().limit(limit).toList();
    }

    /**
     * 返回已记录事件。
     *
     * @return 已记录事件
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    List<OcrEvent> events() {
        return events;
    }
}

/**
 * 内存批次仓储。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
class InMemoryBatchRepository implements BatchRepository {

    private final Map<String, Batch> batches = new HashMap<>(TEST_DOCUMENT_CAPACITY);

    /**
     * 保存批次。
     *
     * @param batch 批次
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void save(Batch batch) {
        batches.put(batch.batchId(), batch);
    }

    /**
     * 按 ID 查询批次。
     *
     * @param batchId 批次 ID
     * @return 批次
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public Optional<Batch> findById(String batchId) {
        return Optional.ofNullable(batches.get(batchId));
    }

    /**
     * 按幂等键查询批次。
     *
     * @param idempotencyKey 幂等键
     * @return 批次
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public Optional<Batch> findByIdempotencyKey(String idempotencyKey) {
        return Optional.empty();
    }

    /**
     * 查询最近批次。
     *
     * @param limit 最大数量
     * @return 批次列表
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public List<Batch> listRecent(int limit) {
        return List.of();
    }

    /**
     * 更新批次汇总。
     *
     * @param batchId 批次 ID
     * @param completedFiles 完成文件数
     * @param failedFiles 失败文件数
     * @param status 批次状态
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void updateSummary(String batchId, int completedFiles, int failedFiles, BatchStatus status) {
        // 当前测试只验证文档级持久化。
    }
}
