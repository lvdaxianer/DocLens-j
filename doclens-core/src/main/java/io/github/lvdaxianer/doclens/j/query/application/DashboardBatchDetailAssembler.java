package io.github.lvdaxianer.doclens.j.query.application;

import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJob;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResult;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dashboard 批次详情读模型组装器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
class DashboardBatchDetailAssembler {

    private static final float HASH_MAP_LOAD_FACTOR = 0.75F;

    private final DetailDependencies dependencies;
    private final DashboardRowAssembler rowAssembler;
    private final DashboardOcrRuntimeCapacityAssembler runtimeCapacityAssembler;

    /**
     * 创建 Dashboard 批次详情读模型组装器。
     *
     * @param dependencies 批次详情依赖集合
     * @param rowAssembler Dashboard 行组装器
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    DashboardBatchDetailAssembler(DetailDependencies dependencies, DashboardRowAssembler rowAssembler) {
        this.dependencies = dependencies;
        this.rowAssembler = rowAssembler;
        this.runtimeCapacityAssembler = new DashboardOcrRuntimeCapacityAssembler();
    }

    /**
     * 构建 Dashboard 批次详情。
     *
     * @param batch 批次聚合
     * @return 批次详情读模型
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    Map<String, Object> assemble(Batch batch) {
        String batchId = batch.batchId();
        List<DocumentJob> documents = dependencies.documentRepository().listByBatchId(batchId);
        List<OcrEvent> events = dependencies.eventRepository().listByBatchId(batchId);
        List<CallbackJob> callbackJobs = dependencies.callbackJobRepository().listByBatchId(batchId);
        return assembleRows(new BatchDetailRows(batch, documents, events, callbackJobs));
    }

    /**
     * 创建批次详情所有行数据。
     *
     * @param rows 批次详情原始行集合
     * @return 批次详情读模型
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private Map<String, Object> assembleRows(BatchDetailRows rows) {
        String batchId = rows.batch().batchId();
        Map<String, Object> routePolicy = rowAssembler.batchRoutePolicy(rows.documents());
        return Map.ofEntries(
                Map.entry("batch", rowAssembler.batchRow(rows.batch(), rows.documents())),
                Map.entry("documents", documentRows(batchId, rows.documents())),
                Map.entry("events", rowAssembler.eventRows(rows.events())),
                Map.entry("callback_jobs", rowAssembler.callbackJobRows(rows.callbackJobs())),
                Map.entry("ocr_route_policy", routePolicy),
                Map.entry("ocr_runtime_capacity", ocrRuntimeCapacity(routePolicy)),
                Map.entry("batch_dispatch_hit_nodes", dependencies.ocrMetricsProvider().dispatchHitNodesByBatch(batchId)),
                Map.entry("failure_summary", rowAssembler.failureSummary(rows.documents()))
        );
    }

    /**
     * 创建文档详情行集合。
     *
     * @param batchId 批次 ID
     * @param documents 文档集合
     * @return 文档详情行集合
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private List<Map<String, Object>> documentRows(String batchId, List<DocumentJob> documents) {
        return rowAssembler.documentRows(documents,
                dependencies.ocrMetricsProvider().finalHitNodesByBatch(batchId), chunkCounts(documents));
    }

    /**
     * 获取批次 OCR 运行态容量快照。
     *
     * @param routePolicy 批次路由策略
     * @return OCR 运行态容量快照
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private Map<String, Object> ocrRuntimeCapacity(Map<String, Object> routePolicy) {
        return runtimeCapacityAssembler.assemble(routePolicy, dependencies.ocrMetricsProvider().ocrResources());
    }

    /**
     * 获取批次内文档对应的 chunk 数。
     *
     * @param documents 文档集合
     * @return 文档 chunk 数映射
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private Map<String, Integer> chunkCounts(List<DocumentJob> documents) {
        List<String> documentIds = documents.stream().map(DocumentJob::documentId).toList();
        Map<String, Integer> chunkCounts = new LinkedHashMap<>(mapCapacity(documentIds.size()));
        for (OcrResult result : dependencies.resultRepository().findByDocumentIds(documentIds)) {
            chunkCounts.put(result.documentId(), chunkCount(result));
        }
        return chunkCounts;
    }

    /**
     * 读取单个文档的 chunk 数。
     *
     * @param result OCR 结果
     * @return chunk 数
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private Integer chunkCount(OcrResult result) {
        Object rawCount = result.rawVendorOutput().get("llm_chunk_count");
        if (rawCount instanceof Number number) {
            // 结果元数据提供数值时，直接转换为整数。
            return number.intValue();
        } else {
            // 元数据缺失或类型不匹配时回退到 0，避免前端展示异常值。
            return 0;
        }
    }

    /**
     * 计算 HashMap 初始容量。
     *
     * @param expectedSize 预期元素数量
     * @return HashMap 初始容量
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private int mapCapacity(int expectedSize) {
        return (int) (Math.max(1, expectedSize) / HASH_MAP_LOAD_FACTOR) + 1;
    }

    /**
     * Dashboard 批次详情依赖集合。
     *
     * @param documentRepository 文档仓储
     * @param eventRepository OCR 事件仓储
     * @param resultRepository OCR 结果仓储
     * @param callbackJobRepository callback 任务仓储
     * @param ocrMetricsProvider OCR 指标提供器
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    record DetailDependencies(
            DocumentJobRepository documentRepository,
            OcrEventRepository eventRepository,
            OcrResultRepository resultRepository,
            CallbackJobRepository callbackJobRepository,
            DashboardOcrMetricsProvider ocrMetricsProvider
    ) {
    }

    /**
     * Dashboard 批次详情原始行集合。
     *
     * @param batch 批次聚合
     * @param documents 文档集合
     * @param events 事件集合
     * @param callbackJobs callback 任务集合
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private record BatchDetailRows(
            Batch batch,
            List<DocumentJob> documents,
            List<OcrEvent> events,
            List<CallbackJob> callbackJobs
    ) {
    }
}
