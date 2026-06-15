package io.github.lvdaxianer.doclens.j.query.application;

import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJob;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.EmptyCallbackJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import io.github.lvdaxianer.doclens.j.shared.domain.ResourceNotFoundException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Dashboard 控制台读模型查询服务。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public class DashboardQueryService {

    private static final int RECENT_BATCH_LIMIT = 20;
    private static final int RECENT_DOCUMENT_LIMIT = 100;
    private static final int RECENT_EVENT_LIMIT = 50;
    private static final int MILLIS_PER_SECOND = 1000;

    private final BatchRepository batchRepository;
    private final DocumentJobRepository documentRepository;
    private final OcrEventRepository eventRepository;
    private final CallbackJobRepository callbackJobRepository;
    private final DashboardRowAssembler rowAssembler;
    private final DashboardStageMetricsAssembler stageMetricsAssembler;
    private final DashboardOcrMetricsProvider ocrMetricsProvider;

    /**
     * 创建 Dashboard 查询服务。
     *
     * @param batchRepository 批次仓储
     * @param documentRepository 文档仓储
     * @param eventRepository 事件仓储
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public DashboardQueryService(
            BatchRepository batchRepository,
            DocumentJobRepository documentRepository,
            OcrEventRepository eventRepository
    ) {
        this(new Dependencies(batchRepository, documentRepository, eventRepository,
                new EmptyDashboardOcrMetricsProvider(), new EmptyCallbackJobRepository()));
    }

    /**
     * 创建 Dashboard 查询服务。
     *
     * @param batchRepository 批次仓储
     * @param documentRepository 文档仓储
     * @param eventRepository 事件仓储
     * @param ocrMetricsProvider OCR 指标提供器
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public DashboardQueryService(
            BatchRepository batchRepository,
            DocumentJobRepository documentRepository,
            OcrEventRepository eventRepository,
            DashboardOcrMetricsProvider ocrMetricsProvider
    ) {
        this(new Dependencies(batchRepository, documentRepository, eventRepository, ocrMetricsProvider,
                new EmptyCallbackJobRepository()));
    }

    /**
     * 创建 Dashboard 查询服务。
     *
     * @param dependencies 查询服务依赖
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public DashboardQueryService(Dependencies dependencies) {
        Dependencies safeDependencies = Objects.requireNonNull(dependencies, "dashboard dependencies is required");
        this.batchRepository = safeDependencies.batchRepository();
        this.documentRepository = safeDependencies.documentRepository();
        this.eventRepository = safeDependencies.eventRepository();
        this.ocrMetricsProvider = safeDependencies.ocrMetricsProvider();
        this.callbackJobRepository = safeDependencies.callbackJobRepository();
        this.rowAssembler = new DashboardRowAssembler();
        this.stageMetricsAssembler = new DashboardStageMetricsAssembler();
    }

    /**
     * 获取 Dashboard 总览。
     *
     * @return Dashboard 总览读模型
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public Map<String, Object> summary() {
        List<Batch> batches = batchRepository.listRecent(RECENT_BATCH_LIMIT);
        List<DocumentJob> documents = documentRepository.listRecent(RECENT_DOCUMENT_LIMIT);
        List<OcrEvent> events = eventRepository.listRecent(RECENT_EVENT_LIMIT);
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
     * 获取 Dashboard 批次列表。
     *
     * @return 批次列表读模型
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public Map<String, Object> batches() {
        List<Batch> batches = batchRepository.listRecent(RECENT_BATCH_LIMIT);
        List<DocumentJob> documents = documentRepository.listByBatchIds(batchIds(batches));
        return Map.of("items", rowAssembler.batchRows(batches, documents), "total", batches.size());
    }

    /**
     * 获取 Dashboard 批次详情。
     *
     * @param batchId 批次 ID
     * @return 批次详情读模型
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public Map<String, Object> batchDetail(String batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("batch " + batchId + " not found"));
        List<DocumentJob> documents = documentRepository.listByBatchId(batchId);
        List<OcrEvent> events = eventRepository.listByBatchId(batchId);
        List<CallbackJob> callbackJobs = callbackJobRepository.listByBatchId(batchId);
        return Map.ofEntries(
                Map.entry("batch", rowAssembler.batchRow(batch, documents)),
                Map.entry("documents", rowAssembler.documentRows(documents,
                        ocrMetricsProvider.finalHitNodesByBatch(batchId))),
                Map.entry("events", rowAssembler.eventRows(events)),
                Map.entry("callback_jobs", rowAssembler.callbackJobRows(callbackJobs)),
                Map.entry("ocr_route_policy", rowAssembler.batchRoutePolicy(documents)),
                Map.entry("batch_dispatch_hit_nodes", ocrMetricsProvider.dispatchHitNodesByBatch(batchId)),
                Map.entry("failure_summary", rowAssembler.failureSummary(documents))
        );
    }

    /**
     * 获取 OCR 健康摘要。
     *
     * @return OCR 健康读模型
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public Map<String, Object> ocrHealth() {
        List<DocumentJob> documents = documentRepository.listRecent(RECENT_DOCUMENT_LIMIT);
        return Map.ofEntries(
                Map.entry("adapter_key", DocLensConstants.DEFAULT_ADAPTER_KEY),
                Map.entry("success_rate", ratio(completedCount(documents), documents.size())),
                Map.entry("failure_rate", ratio(failedCount(documents), documents.size())),
                Map.entry("average_duration_ms", averageDurationMillis(documents)),
                Map.entry("ocr_resources", ocrMetricsProvider.ocrResources()),
                Map.entry("recent_failures", rowAssembler.failureRows(documents))
        );
    }

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

    private Map<String, Object> throughput(List<DocumentJob> documents) {
        return Map.of("completed_last_window", completedCount(documents), "window_size", documents.size());
    }

    private List<String> batchIds(List<Batch> batches) {
        return batches.stream().map(Batch::batchId).toList();
    }

    private long completedCount(List<DocumentJob> documents) {
        return documents.stream().filter(document -> document.status() == DocumentStatus.COMPLETED).count();
    }

    private long failedCount(List<DocumentJob> documents) {
        return documents.stream().filter(document -> document.status().isFailureLike()).count();
    }

    private long processingCount(List<DocumentJob> documents) {
        return documents.stream().filter(document -> document.status() == DocumentStatus.PROCESSING).count();
    }

    /**
     * 统计指定文档状态数量。
     *
     * @param documents 文档任务集合
     * @param status 目标文档状态
     * @return 目标状态数量
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private long statusCount(List<DocumentJob> documents, DocumentStatus status) {
        return documents.stream().filter(document -> document.status() == status).count();
    }

    private double ratio(long numerator, long denominator) {
        if (denominator > 0) {
            // 有分母时按百分比保留两位小数。
            return Math.round(numerator * 10000D / denominator) / 100D;
        } else {
            // 空集合场景展示 0，避免除零异常。
            return 0D;
        }
    }

    private long averageDurationMillis(List<DocumentJob> documents) {
        return Math.round(documents.stream().mapToLong(this::durationMillis).average().orElse(0D));
    }

    private long durationMillis(DocumentJob document) {
        OffsetDateTime end = document.updatedAt();
        return Duration.between(document.createdAt(), end).toMillis();
    }

    /**
     * Dashboard 查询服务依赖集合。
     *
     * @param batchRepository 批次仓储
     * @param documentRepository 文档仓储
     * @param eventRepository 事件仓储
     * @param ocrMetricsProvider OCR 指标提供器
     * @param callbackJobRepository 回调任务仓储
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public record Dependencies(
            BatchRepository batchRepository,
            DocumentJobRepository documentRepository,
            OcrEventRepository eventRepository,
            DashboardOcrMetricsProvider ocrMetricsProvider,
            CallbackJobRepository callbackJobRepository
    ) {

        /**
         * 创建 Dashboard 查询服务依赖集合。
         *
         * @author lvdaxianerplus
         * @date 2026-06-15
         */
        public Dependencies {
            batchRepository = Objects.requireNonNull(batchRepository, "batch repository is required");
            documentRepository = Objects.requireNonNull(documentRepository, "document repository is required");
            eventRepository = Objects.requireNonNull(eventRepository, "event repository is required");
            ocrMetricsProvider = Objects.requireNonNull(ocrMetricsProvider, "ocr metrics provider is required");
            callbackJobRepository = Objects.requireNonNull(callbackJobRepository,
                    "callback job repository is required");
        }
    }

}
