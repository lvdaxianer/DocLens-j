package io.github.lvdaxianer.doclens.j.query.application;

import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJob;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.EmptyCallbackJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.EmptyOcrResultRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResult;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import io.github.lvdaxianer.doclens.j.shared.domain.ResourceNotFoundException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
    private static final float HASH_MAP_LOAD_FACTOR = 0.75F;

    private final BatchRepository batchRepository;
    private final DocumentJobRepository documentRepository;
    private final OcrEventRepository eventRepository;
    private final OcrResultRepository resultRepository;
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
        this(new Dependencies(new Dependencies.Repositories(batchRepository, documentRepository, eventRepository,
                new EmptyOcrResultRepository()), new Dependencies.Services(
                new EmptyDashboardOcrMetricsProvider(), new EmptyCallbackJobRepository())));
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
        this(new Dependencies(new Dependencies.Repositories(batchRepository, documentRepository, eventRepository,
                new EmptyOcrResultRepository()), new Dependencies.Services(ocrMetricsProvider,
                new EmptyCallbackJobRepository())));
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
        this.batchRepository = safeDependencies.repositories().batchRepository();
        this.documentRepository = safeDependencies.repositories().documentRepository();
        this.eventRepository = safeDependencies.repositories().eventRepository();
        this.resultRepository = safeDependencies.repositories().resultRepository();
        this.ocrMetricsProvider = safeDependencies.services().ocrMetricsProvider();
        this.callbackJobRepository = safeDependencies.services().callbackJobRepository();
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
        return summaryView(batches, documents, events);
    }

    /**
     * 获取 caller 范围内 Dashboard 总览。
     *
     * @param caller caller 身份
     * @return Dashboard 总览读模型
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public Map<String, Object> summary(CallerIdentity caller) {
        List<Batch> batches = batchRepository.listRecentForCaller(caller, RECENT_BATCH_LIMIT);
        List<DocumentJob> documents = documentRepository.listByBatchIds(batchIds(batches));
        List<OcrEvent> events = callerEvents(batches);
        return summaryView(batches, documents, events);
    }

    /**
     * 构建 Dashboard 总览。
     *
     * @param batches 批次集合
     * @param documents 文档集合
     * @param events 事件集合
     * @return Dashboard 总览读模型
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private Map<String, Object> summaryView(List<Batch> batches, List<DocumentJob> documents, List<OcrEvent> events) {
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
     * 获取 caller 范围内 Dashboard 批次列表。
     *
     * @param caller caller 身份
     * @return 批次列表读模型
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public Map<String, Object> batches(CallerIdentity caller) {
        List<Batch> batches = batchRepository.listRecentForCaller(caller, RECENT_BATCH_LIMIT);
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
        return batchDetailView(batch);
    }

    /**
     * 获取 caller 范围内 Dashboard 批次详情。
     *
     * @param caller caller 身份
     * @param batchId 批次 ID
     * @return 批次详情读模型
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public Map<String, Object> batchDetail(CallerIdentity caller, String batchId) {
        Batch batch = batchRepository.findByIdForCaller(caller, batchId)
                .orElseThrow(() -> new ResourceNotFoundException("batch " + batchId + " not found"));
        return batchDetailView(batch);
    }

    /**
     * 构建 Dashboard 批次详情。
     *
     * @param batch 批次聚合
     * @return 批次详情读模型
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private Map<String, Object> batchDetailView(Batch batch) {
        String batchId = batch.batchId();
        List<DocumentJob> documents = documentRepository.listByBatchId(batchId);
        List<OcrEvent> events = eventRepository.listByBatchId(batchId);
        List<CallbackJob> callbackJobs = callbackJobRepository.listByBatchId(batchId);
        Map<String, Integer> chunkCounts = chunkCounts(documents);
        return Map.ofEntries(
                Map.entry("batch", rowAssembler.batchRow(batch, documents)),
                Map.entry("documents", rowAssembler.documentRows(documents,
                        ocrMetricsProvider.finalHitNodesByBatch(batchId), chunkCounts)),
                Map.entry("events", rowAssembler.eventRows(events)),
                Map.entry("callback_jobs", rowAssembler.callbackJobRows(callbackJobs)),
                Map.entry("ocr_route_policy", rowAssembler.batchRoutePolicy(documents)),
                Map.entry("batch_dispatch_hit_nodes", ocrMetricsProvider.dispatchHitNodesByBatch(batchId)),
                Map.entry("failure_summary", rowAssembler.failureSummary(documents))
        );
    }

    /**
     * 获取批次内文档对应的 chunk 数。
     *
     * @param documents 文档集合
     * @return 文档 chunk 数映射
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private Map<String, Integer> chunkCounts(List<DocumentJob> documents) {
        List<String> documentIds = documents.stream().map(DocumentJob::documentId).toList();
        int capacity = (int) (Math.max(1, documentIds.size()) / HASH_MAP_LOAD_FACTOR) + 1;
        Map<String, Integer> chunkCounts = new java.util.LinkedHashMap<>(capacity);
        for (OcrResult result : resultRepository.findByDocumentIds(documentIds)) {
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
     * @date 2026-06-19
     */
    private Integer chunkCount(OcrResult result) {
        Object rawCount = result.rawVendorOutput().get("llm_chunk_count");
        // 从 OCR 结果元数据中读取 chunk 数时，保留数值字段。
        if (rawCount instanceof Number number) {
            return number.intValue();
        } else {
            // 元数据缺失或类型不匹配时，回退到 0，避免前端展示异常值。
            return 0;
        }
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
        return ocrHealthView(documents);
    }

    /**
     * 获取 caller 范围内 OCR 健康摘要。
     *
     * @param caller caller 身份
     * @return OCR 健康读模型
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public Map<String, Object> ocrHealth(CallerIdentity caller) {
        List<Batch> batches = batchRepository.listRecentForCaller(caller, RECENT_BATCH_LIMIT);
        List<DocumentJob> documents = documentRepository.listByBatchIds(batchIds(batches));
        return ocrHealthView(documents);
    }

    /**
     * 构建 OCR 健康摘要。
     *
     * @param documents 文档集合
     * @return OCR 健康读模型
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private Map<String, Object> ocrHealthView(List<DocumentJob> documents) {
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
     * 获取 caller 批次关联事件。
     *
     * @param batches caller 批次集合
     * @return caller 事件集合
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private List<OcrEvent> callerEvents(List<Batch> batches) {
        Set<String> callerBatchIds = Set.copyOf(batchIds(batches));
        return eventRepository.listRecent(RECENT_EVENT_LIMIT).stream()
                .filter(event -> callerBatchIds.contains(event.batchId()))
                .toList();
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
     * @param repositories 数据仓储集合
     * @param services 查询服务集合
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public record Dependencies(
            Repositories repositories,
            Services services
    ) {

        /**
         * Dashboard 查询服务数据仓储集合。
         *
         * @param batchRepository 批次仓储
         * @param documentRepository 文档仓储
         * @param eventRepository 事件仓储
         * @param resultRepository OCR 结果仓储
         * @author lvdaxianerplus
         * @date 2026-06-19
         */
        public record Repositories(
                BatchRepository batchRepository,
                DocumentJobRepository documentRepository,
                OcrEventRepository eventRepository,
                OcrResultRepository resultRepository
        ) {

            /**
             * 创建 Dashboard 查询服务数据仓储集合。
             *
             * @author lvdaxianerplus
             * @date 2026-06-19
             */
            public Repositories {
                batchRepository = Objects.requireNonNull(batchRepository, "batch repository is required");
                documentRepository = Objects.requireNonNull(documentRepository, "document repository is required");
                eventRepository = Objects.requireNonNull(eventRepository, "event repository is required");
                resultRepository = Objects.requireNonNull(resultRepository, "result repository is required");
            }
        }

        /**
         * Dashboard 查询服务查询能力集合。
         *
         * @param ocrMetricsProvider OCR 指标提供器
         * @param callbackJobRepository 回调任务仓储
         * @author lvdaxianerplus
         * @date 2026-06-19
         */
        public record Services(
                DashboardOcrMetricsProvider ocrMetricsProvider,
                CallbackJobRepository callbackJobRepository
        ) {

            /**
             * 创建 Dashboard 查询服务查询能力集合。
             *
             * @author lvdaxianerplus
             * @date 2026-06-19
             */
            public Services {
                ocrMetricsProvider = Objects.requireNonNull(ocrMetricsProvider, "ocr metrics provider is required");
                callbackJobRepository = Objects.requireNonNull(callbackJobRepository,
                        "callback job repository is required");
            }
        }

        /**
         * 创建 Dashboard 查询服务依赖集合。
         *
         * @author lvdaxianerplus
         * @date 2026-06-15
         */
        public Dependencies {
            repositories = Objects.requireNonNull(repositories, "repositories is required");
            services = Objects.requireNonNull(services, "services is required");
        }
    }

}
