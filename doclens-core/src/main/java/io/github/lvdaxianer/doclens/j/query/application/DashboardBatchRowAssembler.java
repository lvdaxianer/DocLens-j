package io.github.lvdaxianer.doclens.j.query.application;

import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus;
import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentStatus;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dashboard 批次行读模型组装器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
class DashboardBatchRowAssembler {

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
        Map<String, List<DocumentJob>> documentsByBatch = documents.stream()
                .collect(Collectors.groupingBy(DocumentJob::batchId));
        return batches.stream()
                .filter(batch -> hasDisplayableDocuments(batch, documentsByBatch))
                .map(batch -> batchRow(batch, documentsByBatch.getOrDefault(batch.batchId(), List.of())))
                .toList();
    }

    /**
     * 判断批次是否具备 Dashboard 展示价值。
     *
     * @param batch 批次聚合
     * @param documentsByBatch 批次文档映射
     * @return 是否可展示
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private boolean hasDisplayableDocuments(Batch batch, Map<String, List<DocumentJob>> documentsByBatch) {
        return batch.totalFiles() > 0 || !documentsByBatch.getOrDefault(batch.batchId(), List.of()).isEmpty();
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
        BatchStatus displayStatus = batchDisplayStatus(batch, documents);
        int completedFiles = batchCompletedFiles(batch, documents);
        int failedFiles = batchFailedFiles(batch, documents);
        return Map.ofEntries(
                Map.entry("batch_id", batch.batchId()),
                Map.entry("status", displayStatus.name().toLowerCase()),
                Map.entry("total_files", batch.totalFiles()),
                Map.entry("completed_files", completedFiles),
                Map.entry("failed_files", failedFiles),
                Map.entry("progress_percent", batchProgress(batch, documents)),
                Map.entry("success_rate", ratio(completedFiles, batch.totalFiles())),
                Map.entry("failure_rate", ratio(failedFiles, batch.totalFiles())),
                Map.entry("average_duration_ms", averageDurationMillis(documents)),
                Map.entry("created_at", batch.createdAt().toString()),
                Map.entry("updated_at", batch.updatedAt().toString()),
                Map.entry("callback_url", batch.callbackUrl().orElse(DocLensConstants.EMPTY_VALUE)),
                Map.entry("idempotency_key", batch.idempotencyKey().orElse(DocLensConstants.EMPTY_VALUE)),
                Map.entry(CallerIdentity.CLIENT_ID_FIELD, batch.callerIdentity().clientId()),
                Map.entry(CallerIdentity.SOURCE_APP_FIELD, batch.callerIdentity().sourceApp()),
                Map.entry(CallerIdentity.TENANT_KEY_FIELD,
                        batch.callerIdentity().tenantKey().orElse(DocLensConstants.EMPTY_VALUE)),
                Map.entry("metadata", batch.metadata().values())
        );
    }

    /**
     * 计算批次在 Dashboard 中的展示状态。
     *
     * @param batch 批次聚合
     * @param documents 批次内文档任务
     * @return Dashboard 展示状态
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private BatchStatus batchDisplayStatus(Batch batch, List<DocumentJob> documents) {
        if (documents.isEmpty()) {
            return batch.status();
        } else {
            return batchStatusFromDocuments(batch, documents);
        }
    }

    /**
     * 从文档状态聚合批次展示状态。
     *
     * @param batch 批次聚合
     * @param documents 批次内文档任务
     * @return 文档聚合后的批次展示状态
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private BatchStatus batchStatusFromDocuments(Batch batch, List<DocumentJob> documents) {
        long processing = statusCount(documents, DocumentStatus.PROCESSING);
        if (processing > 0) {
            return BatchStatus.PROCESSING;
        } else {
            return nonProcessingBatchStatus(batch, documents);
        }
    }

    /**
     * 计算没有处理中任务时的批次展示状态。
     *
     * @param batch 批次聚合
     * @param documents 批次内文档任务
     * @return 非处理中场景的批次展示状态
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private BatchStatus nonProcessingBatchStatus(Batch batch, List<DocumentJob> documents) {
        long queued = statusCount(documents, DocumentStatus.QUEUED);
        long completed = statusCount(documents, DocumentStatus.COMPLETED);
        long failed = failureLikeCount(documents);
        if (queued > 0 && (completed > 0 || failed > 0)) {
            return BatchStatus.PROCESSING;
        } else {
            return terminalBatchStatus(batch, queued, completed, failed);
        }
    }

    /**
     * 计算终态优先的批次展示状态。
     *
     * @param batch 批次聚合
     * @param queued 排队文档数量
     * @param completed 完成文档数量
     * @param failed 失败文档数量
     * @return 终态优先的批次展示状态
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private BatchStatus terminalBatchStatus(Batch batch, long queued, long completed, long failed) {
        BatchStatus status;
        if (failed > 0 && completed > 0) {
            status = BatchStatus.PARTIAL_FAILED;
        } else if (failed > 0) {
            status = BatchStatus.FAILED;
        } else if (completed > 0 && queued == 0) {
            status = BatchStatus.COMPLETED;
        } else {
            status = batch.status();
        }
        return status;
    }

    /**
     * 计算批次在 Dashboard 中的展示进度。
     *
     * @param batch 批次聚合
     * @param documents 批次内文档任务
     * @return Dashboard 展示进度
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private int batchProgress(Batch batch, List<DocumentJob> documents) {
        if (documents.isEmpty()) {
            return batchProgressFromSummary(batch);
        } else {
            return documentAverageProgress(documents);
        }
    }

    /**
     * 从批次摘要字段计算展示进度。
     *
     * @param batch 批次聚合
     * @return 批次摘要进度
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private int batchProgressFromSummary(Batch batch) {
        if (batch.totalFiles() > 0) {
            return (int) Math.round((batch.completedFiles() + batch.failedFiles()) * 100.0 / batch.totalFiles());
        } else {
            return DocLensConstants.ZERO_PROGRESS_PERCENT;
        }
    }

    /**
     * 从文档进度计算批次平均展示进度。
     *
     * @param documents 批次内文档任务
     * @return 文档平均进度
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private int documentAverageProgress(List<DocumentJob> documents) {
        return (int) Math.round(documents.stream().mapToInt(DocumentJob::progressPercent).average().orElse(0D));
    }

    /**
     * 计算批次展示已完成文件数。
     *
     * @param batch 批次聚合
     * @param documents 批次内文档任务
     * @return Dashboard 展示已完成文件数
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private int batchCompletedFiles(Batch batch, List<DocumentJob> documents) {
        if (documents.isEmpty()) {
            return batch.completedFiles();
        } else {
            return Math.toIntExact(statusCount(documents, DocumentStatus.COMPLETED));
        }
    }

    /**
     * 计算批次展示失败文件数。
     *
     * @param batch 批次聚合
     * @param documents 批次内文档任务
     * @return Dashboard 展示失败文件数
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private int batchFailedFiles(Batch batch, List<DocumentJob> documents) {
        if (documents.isEmpty()) {
            return batch.failedFiles();
        } else {
            return Math.toIntExact(failureLikeCount(documents));
        }
    }

    /**
     * 统计指定文档状态数量。
     *
     * @param documents 文档任务集合
     * @param status 目标文档状态
     * @return 目标状态数量
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private long statusCount(List<DocumentJob> documents, DocumentStatus status) {
        return documents.stream().filter(document -> document.status() == status).count();
    }

    /**
     * 统计失败类终态文档数量。
     *
     * @param documents 文档任务集合
     * @return 失败类终态数量
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private long failureLikeCount(List<DocumentJob> documents) {
        return documents.stream().filter(document -> document.status().isFailureLike()).count();
    }

    /**
     * 计算百分比。
     *
     * @param numerator 分子
     * @param denominator 分母
     * @return 百分比
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private double ratio(long numerator, long denominator) {
        if (denominator > 0) {
            return Math.round(numerator * 10000D / denominator) / 100D;
        } else {
            return 0D;
        }
    }

    /**
     * 计算平均耗时。
     *
     * @param documents 文档集合
     * @return 平均耗时毫秒
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private long averageDurationMillis(List<DocumentJob> documents) {
        return Math.round(documents.stream().mapToLong(this::durationMillis).average().orElse(0D));
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
