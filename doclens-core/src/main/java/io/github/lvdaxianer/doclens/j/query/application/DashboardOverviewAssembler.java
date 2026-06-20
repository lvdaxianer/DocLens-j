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

/**
 * Dashboard 总览与健康读模型组装器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
class DashboardOverviewAssembler {

    private final DashboardRowAssembler rowAssembler;
    private final DashboardStageMetricsAssembler stageMetricsAssembler;
    private final DashboardOcrMetricsProvider ocrMetricsProvider;

    /**
     * 创建 Dashboard 总览与健康读模型组装器。
     *
     * @param rowAssembler Dashboard 行组装器
     * @param stageMetricsAssembler 阶段指标组装器
     * @param ocrMetricsProvider OCR 指标提供器
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    DashboardOverviewAssembler(DashboardRowAssembler rowAssembler,
            DashboardStageMetricsAssembler stageMetricsAssembler,
            DashboardOcrMetricsProvider ocrMetricsProvider) {
        this.rowAssembler = rowAssembler;
        this.stageMetricsAssembler = stageMetricsAssembler;
        this.ocrMetricsProvider = ocrMetricsProvider;
    }

    /**
     * 构建 Dashboard 总览。
     *
     * @param batches 批次集合
     * @param documents 文档集合
     * @param events 事件集合
     * @return Dashboard 总览读模型
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    Map<String, Object> summaryView(List<Batch> batches, List<DocumentJob> documents, List<OcrEvent> events) {
        return Map.ofEntries(
                Map.entry("overview", overview(batches, documents)),
                Map.entry("throughput", throughput(documents)),
                Map.entry("stage_status_counts", stageMetricsAssembler.stageStatusCounts(documents)),
                Map.entry("image_progress", stageMetricsAssembler.imageProgress(documents)),
                Map.entry("ocr_resources", ocrMetricsProvider.ocrResources()),
                Map.entry("recent_batches", rowAssembler.batchRows(batches, documents)),
                Map.entry("recent_failures", rowAssembler.failureRows(documents)),
                Map.entry("recent_events", rowAssembler.eventRows(events))
        );
    }

    /**
     * 构建 OCR 健康摘要。
     *
     * @param documents 文档集合
     * @return OCR 健康读模型
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    Map<String, Object> ocrHealthView(List<DocumentJob> documents) {
        return Map.ofEntries(
                Map.entry("adapter_key", DocLensConstants.DEFAULT_ADAPTER_KEY),
                Map.entry("success_rate", ratio(completedCount(documents), documents.size())),
                Map.entry("failure_rate", ratio(failedCount(documents), documents.size())),
                Map.entry("average_duration_ms", averageDurationMillis(documents)),
                Map.entry("ocr_resources", ocrMetricsProvider.ocrResources()),
                Map.entry("recent_failures", rowAssembler.failureRows(documents))
        );
    }

    /**
     * 构建总览计数行。
     *
     * @param batches 批次集合
     * @param documents 文档集合
     * @return 总览计数行
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private Map<String, Object> overview(List<Batch> batches, List<DocumentJob> documents) {
        return Map.ofEntries(
                Map.entry("batch_count", batches.size()),
                Map.entry("document_count", documents.size()),
                Map.entry("completed_documents", completedCount(documents)),
                Map.entry("failed_documents", failedCount(documents)),
                Map.entry("processing_documents", processingCount(documents)),
                Map.entry("success_rate", ratio(completedCount(documents), documents.size())),
                Map.entry("failure_rate", ratio(failedCount(documents), documents.size())),
                Map.entry("average_duration_ms", averageDurationMillis(documents))
        );
    }

    /**
     * 构建吞吐指标行。
     *
     * @param documents 文档集合
     * @return 吞吐指标行
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private Map<String, Object> throughput(List<DocumentJob> documents) {
        return Map.of("completed_last_window", completedCount(documents), "window_size", documents.size());
    }

    /**
     * 统计完成文档数量。
     *
     * @param documents 文档集合
     * @return 完成文档数量
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private long completedCount(List<DocumentJob> documents) {
        return documents.stream().filter(document -> document.status() == DocumentStatus.COMPLETED).count();
    }

    /**
     * 统计失败文档数量。
     *
     * @param documents 文档集合
     * @return 失败文档数量
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private long failedCount(List<DocumentJob> documents) {
        return documents.stream().filter(document -> document.status().isFailureLike()).count();
    }

    /**
     * 统计处理中文档数量。
     *
     * @param documents 文档集合
     * @return 处理中文档数量
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private long processingCount(List<DocumentJob> documents) {
        return documents.stream().filter(document -> document.status() == DocumentStatus.PROCESSING).count();
    }

    /**
     * 计算百分比。
     *
     * @param numerator 分子
     * @param denominator 分母
     * @return 百分比
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private double ratio(long numerator, long denominator) {
        if (denominator > 0) {
            // 有分母时按百分比保留两位小数。
            return Math.round(numerator * 10000D / denominator) / 100D;
        } else {
            // 空集合场景展示 0，避免除零异常。
            return 0D;
        }
    }

    /**
     * 计算平均处理耗时。
     *
     * @param documents 文档集合
     * @return 平均处理耗时毫秒
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private long averageDurationMillis(List<DocumentJob> documents) {
        return Math.round(documents.stream().mapToLong(this::durationMillis).average().orElse(0D));
    }

    /**
     * 计算单个文档处理耗时。
     *
     * @param document 文档任务
     * @return 处理耗时毫秒
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private long durationMillis(DocumentJob document) {
        OffsetDateTime end = document.updatedAt();
        return Duration.between(document.createdAt(), end).toMillis();
    }
}
