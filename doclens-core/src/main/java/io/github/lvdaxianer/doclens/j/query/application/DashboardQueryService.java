package io.github.lvdaxianer.doclens.j.query.application;

import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import io.github.lvdaxianer.doclens.j.shared.domain.ResourceNotFoundException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private static final Map<DocumentType, ProcessingTrack> PROCESSING_TRACKS = processingTracks();

    private final BatchRepository batchRepository;
    private final DocumentJobRepository documentRepository;
    private final OcrEventRepository eventRepository;

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
        this.batchRepository = batchRepository;
        this.documentRepository = documentRepository;
        this.eventRepository = eventRepository;
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
                Map.entry("recent_batches", batchRows(batches, documents)),
                Map.entry("recent_failures", failureRows(documents)),
                Map.entry("recent_events", eventRows(events))
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
        return Map.of("items", batchRows(batches, documents), "total", batches.size());
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
        return Map.ofEntries(
                Map.entry("batch", batchRow(batch, documents)),
                Map.entry("documents", documentRows(documents)),
                Map.entry("events", eventRows(events)),
                Map.entry("failure_summary", failureSummary(documents))
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
                Map.entry("recent_failures", failureRows(documents))
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

    private List<Map<String, Object>> batchRows(List<Batch> batches, List<DocumentJob> documents) {
        Map<String, List<DocumentJob>> documentsByBatch = documents.stream()
                .collect(Collectors.groupingBy(DocumentJob::batchId));
        return batches.stream().map(batch -> batchRow(batch,
                documentsByBatch.getOrDefault(batch.batchId(), List.of()))).toList();
    }

    private Map<String, Object> batchRow(Batch batch, List<DocumentJob> documents) {
        return Map.ofEntries(
                Map.entry("batch_id", batch.batchId()),
                Map.entry("status", batch.status().name().toLowerCase()),
                Map.entry("total_files", batch.totalFiles()),
                Map.entry("completed_files", batch.completedFiles()),
                Map.entry("failed_files", batch.failedFiles()),
                Map.entry("progress_percent", batchProgress(batch)),
                Map.entry("success_rate", ratio(batch.completedFiles(), batch.totalFiles())),
                Map.entry("failure_rate", ratio(batch.failedFiles(), batch.totalFiles())),
                Map.entry("average_duration_ms", averageDurationMillis(documents)),
                Map.entry("created_at", batch.createdAt().toString()),
                Map.entry("updated_at", batch.updatedAt().toString())
        );
    }

    private List<Map<String, Object>> documentRows(List<DocumentJob> documents) {
        return documents.stream().map(this::documentRow).toList();
    }

    private Map<String, Object> documentRow(DocumentJob document) {
        return Map.ofEntries(
                Map.entry("document_id", document.documentId()),
                Map.entry("batch_id", document.batchId()),
                Map.entry("file_name", document.fileName()),
                Map.entry("file_type", document.fileType().name().toLowerCase()),
                Map.entry("status", document.status().name().toLowerCase()),
                Map.entry("stage", document.stage().name().toLowerCase()),
                Map.entry("progress_percent", document.progressPercent()),
                Map.entry("duration_ms", durationMillis(document)),
                Map.entry("track", processingTrack(document)),
                Map.entry("error_code", document.errorCode().orElse(DocLensConstants.EMPTY_VALUE)),
                Map.entry("error_message", document.errorMessage().orElse(DocLensConstants.EMPTY_VALUE)),
                Map.entry("updated_at", document.updatedAt().toString())
        );
    }

    private List<Map<String, Object>> eventRows(List<OcrEvent> events) {
        return events.stream().map(this::eventRow).toList();
    }

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

    private List<Map<String, Object>> failureRows(List<DocumentJob> documents) {
        return documents.stream().filter(document -> document.status() == DocumentStatus.FAILED)
                .map(this::documentRow).toList();
    }

    private Map<String, Long> failureSummary(List<DocumentJob> documents) {
        return documents.stream().filter(document -> document.status() == DocumentStatus.FAILED)
                .collect(Collectors.groupingBy(document -> document.errorCode().orElse("UNKNOWN"), Collectors.counting()));
    }

    private List<Map<String, Object>> processingTrack(DocumentJob document) {
        ProcessingTrack track = ProcessingTrack.from(document);
        return List.of(track.node("上传", true), track.node("类型识别", true),
                track.node("转换", track.hasConversion()), track.node("渲染页图", track.hasPageRendering()),
                track.node("OCR", track.hasOcr()), track.node("合并文本", track.hasMerge()),
                track.node("入库/落盘", document.status() == DocumentStatus.COMPLETED));
    }

    private List<String> batchIds(List<Batch> batches) {
        return batches.stream().map(Batch::batchId).toList();
    }

    private int batchProgress(Batch batch) {
        if (batch.totalFiles() > 0) {
            return (int) Math.round((batch.completedFiles() + batch.failedFiles()) * 100.0 / batch.totalFiles());
        } else {
            return DocLensConstants.ZERO_PROGRESS_PERCENT;
        }
    }

    private long completedCount(List<DocumentJob> documents) {
        return documents.stream().filter(document -> document.status() == DocumentStatus.COMPLETED).count();
    }

    private long failedCount(List<DocumentJob> documents) {
        return documents.stream().filter(document -> document.status() == DocumentStatus.FAILED).count();
    }

    private long processingCount(List<DocumentJob> documents) {
        return documents.stream().filter(document -> document.status() == DocumentStatus.PROCESSING).count();
    }

    private double ratio(long numerator, long denominator) {
        if (denominator > 0) {
            return Math.round(numerator * 10000D / denominator) / 100D;
        } else {
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

    private record ProcessingTrack(boolean hasConversion, boolean hasPageRendering, boolean hasOcr, boolean hasMerge) {

        private static ProcessingTrack from(DocumentJob document) {
            return PROCESSING_TRACKS.get(document.fileType());
        }

        private Map<String, Object> node(String name, boolean active) {
            return Map.of("name", name, "active", active);
        }
    }

    private static Map<DocumentType, ProcessingTrack> processingTracks() {
        EnumMap<DocumentType, ProcessingTrack> tracks = new EnumMap<>(DocumentType.class);
        tracks.put(DocumentType.MARKDOWN, new ProcessingTrack(false, false, false, false));
        tracks.put(DocumentType.TEXT, new ProcessingTrack(false, false, false, false));
        tracks.put(DocumentType.IMAGE, new ProcessingTrack(false, false, true, true));
        tracks.put(DocumentType.PDF, new ProcessingTrack(false, true, true, true));
        tracks.put(DocumentType.WORD, new ProcessingTrack(true, true, true, true));
        return Map.copyOf(tracks);
    }
}
