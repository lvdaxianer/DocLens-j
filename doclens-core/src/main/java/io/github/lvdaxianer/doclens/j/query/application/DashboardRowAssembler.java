package io.github.lvdaxianer.doclens.j.query.application;

import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dashboard 行读模型组装器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
class DashboardRowAssembler {

    private final DashboardBatchRowAssembler batchRowAssembler;
    private final ProcessingTrackAssembler processingTrackAssembler;

    /**
     * 创建 Dashboard 行读模型组装器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    DashboardRowAssembler() {
        this.batchRowAssembler = new DashboardBatchRowAssembler();
        this.processingTrackAssembler = new ProcessingTrackAssembler();
    }

    /**
     * 创建批次行集合。
     *
     * @param batches 批次集合
     * @param documents 文档集合
     * @return 批次行集合
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    List<Map<String, Object>> batchRows(List<Batch> batches, List<DocumentJob> documents) {
        return batchRowAssembler.batchRows(batches, documents);
    }

    /**
     * 创建批次行。
     *
     * @param batch 批次
     * @param documents 批次文档集合
     * @return 批次行
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    Map<String, Object> batchRow(Batch batch, List<DocumentJob> documents) {
        return batchRowAssembler.batchRow(batch, documents);
    }

    /**
     * 创建文档行集合。
     *
     * @param documents 文档集合
     * @return 文档行集合
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    List<Map<String, Object>> documentRows(List<DocumentJob> documents) {
        return documents.stream().map(document -> documentRow(document, List.of())).toList();
    }

    /**
     * 创建带 OCR 最终分配信息的文档行集合。
     *
     * @param documents 文档集合
     * @param finalHitNodesByDocument 文档级最终分配映射
     * @return 文档行集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    List<Map<String, Object>> documentRows(
            List<DocumentJob> documents,
            Map<String, List<Map<String, Object>>> finalHitNodesByDocument
    ) {
        return documents.stream().map(document -> documentRow(document,
                finalHitNodesByDocument.getOrDefault(document.documentId(), List.of()))).toList();
    }

    /**
     * 创建 OCR 事件行集合。
     *
     * @param events OCR 事件集合
     * @return OCR 事件行集合
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    List<Map<String, Object>> eventRows(List<OcrEvent> events) {
        return events.stream().map(this::eventRow).toList();
    }

    /**
     * 创建失败文档行集合。
     *
     * @param documents 文档集合
     * @return 失败文档行集合
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    List<Map<String, Object>> failureRows(List<DocumentJob> documents) {
        return documents.stream().filter(document -> document.status() == DocumentStatus.FAILED)
                .map(document -> documentRow(document, List.of())).toList();
    }

    /**
     * 创建失败摘要。
     *
     * @param documents 文档集合
     * @return 失败摘要
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    Map<String, Long> failureSummary(List<DocumentJob> documents) {
        return documents.stream().filter(document -> document.status() == DocumentStatus.FAILED)
                .collect(Collectors.groupingBy(document -> document.errorCode().orElse("UNKNOWN"),
                        Collectors.counting()));
    }

    /**
     * 获取批次 OCR 路由策略快照。
     *
     * @param documents 批次文档集合
     * @return OCR 路由策略读模型
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    Map<String, Object> batchRoutePolicy(List<DocumentJob> documents) {
        if (documents.isEmpty()) {
            return routePolicyRow(null);
        } else {
            return routePolicyRow(documents.getFirst());
        }
    }

    /**
     * 创建文档行。
     *
     * @param document 文档任务
     * @return 文档行
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private Map<String, Object> documentRow(DocumentJob document, List<Map<String, Object>> finalHitNodes) {
        return Map.ofEntries(
                Map.entry("document_id", document.documentId()),
                Map.entry("batch_id", document.batchId()),
                Map.entry("file_name", document.fileName()),
                Map.entry("file_type", document.fileType().name().toLowerCase()),
                Map.entry("status", document.status().name().toLowerCase()),
                Map.entry("stage", document.stage().name().toLowerCase()),
                Map.entry("progress_percent", document.progressPercent()),
                Map.entry("current_page", document.currentPage()),
                Map.entry("total_pages", document.totalPages()),
                Map.entry("duration_ms", durationMillis(document)),
                Map.entry("track", processingTrackAssembler.assemble(document)),
                Map.entry("ocr_final_hit_nodes", finalHitNodes),
                Map.entry("error_code", document.errorCode().orElse(DocLensConstants.EMPTY_VALUE)),
                Map.entry("error_message", document.errorMessage().orElse(DocLensConstants.EMPTY_VALUE)),
                Map.entry("updated_at", document.updatedAt().toString())
        );
    }

    /**
     * 创建 OCR 事件行。
     *
     * @param event OCR 事件
     * @return OCR 事件行
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private Map<String, Object> eventRow(OcrEvent event) {
        return Map.ofEntries(
                Map.entry("event_id", event.eventId()),
                Map.entry("event_type", event.eventType()),
                Map.entry("batch_id", event.batchId()),
                Map.entry("document_id", event.documentId().orElse(DocLensConstants.EMPTY_VALUE)),
                Map.entry("status", event.status()),
                Map.entry("stage", event.stage()),
                Map.entry("occurred_at", event.occurredAt().toString())
        );
    }

    /**
     * 转换文档 OCR 路由策略。
     *
     * @param document 文档任务
     * @return OCR 路由策略读模型
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private Map<String, Object> routePolicyRow(DocumentJob document) {
        if (document == null) {
            return emptyRoutePolicyRow();
        } else {
            return documentRoutePolicyRow(document);
        }
    }

    /**
     * 创建空 OCR 路由策略行。
     *
     * @return 空 OCR 路由策略行
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private Map<String, Object> emptyRoutePolicyRow() {
        return Map.ofEntries(
                Map.entry("routing_mode", "DEFAULT"),
                Map.entry("model_key", DocLensConstants.EMPTY_VALUE),
                Map.entry("node_id", DocLensConstants.EMPTY_VALUE),
                Map.entry("load_balance_strategy", DocLensConstants.EMPTY_VALUE)
        );
    }

    /**
     * 转换非空文档 OCR 路由策略。
     *
     * @param document 文档任务
     * @return OCR 路由策略读模型
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private Map<String, Object> documentRoutePolicyRow(DocumentJob document) {
        return Map.ofEntries(
                Map.entry("routing_mode", document.ocrRoutePolicy().routingMode().name()),
                Map.entry("model_key", document.ocrRoutePolicy().modelKey().orElse(DocLensConstants.EMPTY_VALUE)),
                Map.entry("node_id", document.ocrRoutePolicy().nodeId().orElse(DocLensConstants.EMPTY_VALUE)),
                Map.entry("load_balance_strategy",
                        document.ocrRoutePolicy().loadBalanceStrategy().orElse(DocLensConstants.EMPTY_VALUE))
        );
    }

    /**
     * 计算文档耗时。
     *
     * @param document 文档任务
     * @return 耗时毫秒
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private long durationMillis(DocumentJob document) {
        OffsetDateTime end = document.updatedAt();
        return Duration.between(document.createdAt(), end).toMillis();
    }
}
