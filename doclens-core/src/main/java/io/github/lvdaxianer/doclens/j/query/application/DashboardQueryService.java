package io.github.lvdaxianer.doclens.j.query.application;

import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.EmptyCallbackJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.EmptyOcrResultRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import io.github.lvdaxianer.doclens.j.shared.domain.ResourceNotFoundException;
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

    private final BatchRepository batchRepository;
    private final DocumentJobRepository documentRepository;
    private final OcrEventRepository eventRepository;
    private final DashboardRowAssembler rowAssembler;
    private final DashboardBatchDetailAssembler batchDetailAssembler;
    private final DashboardOverviewAssembler overviewAssembler;

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
        this.rowAssembler = new DashboardRowAssembler();
        DashboardOcrMetricsProvider metricsProvider = safeDependencies.services().ocrMetricsProvider();
        DashboardStageMetricsAssembler stageAssembler = new DashboardStageMetricsAssembler();
        this.batchDetailAssembler = new DashboardBatchDetailAssembler(
                detailDependencies(safeDependencies, metricsProvider), rowAssembler);
        this.overviewAssembler = new DashboardOverviewAssembler(rowAssembler, stageAssembler, metricsProvider);
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
        return overviewAssembler.summaryView(batches, documents, events);
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
        return overviewAssembler.summaryView(batches, documents, events);
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
        return batchDetailAssembler.assemble(batch);
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
        return batchDetailAssembler.assemble(batch);
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
        return overviewAssembler.ocrHealthView(documents);
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
        return overviewAssembler.ocrHealthView(documents);
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

    /**
     * 获取批次 ID 集合。
     *
     * @param batches 批次集合
     * @return 批次 ID 集合
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private List<String> batchIds(List<Batch> batches) {
        return batches.stream().map(Batch::batchId).toList();
    }

    /**
     * 创建批次详情依赖集合。
     *
     * @param dependencies Dashboard 查询服务依赖
     * @param metricsProvider OCR 指标提供器
     * @return 批次详情依赖集合
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private DashboardBatchDetailAssembler.DetailDependencies detailDependencies(
            Dependencies dependencies,
            DashboardOcrMetricsProvider metricsProvider
    ) {
        return new DashboardBatchDetailAssembler.DetailDependencies(documentRepository, eventRepository,
                dependencies.repositories().resultRepository(),
                dependencies.services().callbackJobRepository(), metricsProvider);
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
