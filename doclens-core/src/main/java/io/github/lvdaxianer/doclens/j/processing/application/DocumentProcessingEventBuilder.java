package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.EventCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResult;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 文档处理事件构建器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
class DocumentProcessingEventBuilder {

    private static final String BATCH_EVENT_PREFIX = "batch.";
    private static final int RESULT_SUMMARY_CAPACITY = 5;
    private static final String PAGE_COUNT_FIELD = "pageCount";
    private static final String BLOCK_COUNT_FIELD = "blockCount";
    private static final String TABLE_COUNT_FIELD = "tableCount";
    private static final String CONFIDENCE_FIELD = "confidence";
    private static final String CALLBACK_BODY_FIELD = "callback_body";

    private final OcrEventFactory eventFactory;

    /**
     * 创建文档处理事件构建器。
     *
     * @param eventFactory OCR 事件工厂
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    DocumentProcessingEventBuilder(OcrEventFactory eventFactory) {
        this.eventFactory = eventFactory;
    }

    /**
     * 构建文档完成链路事件集合。
     *
     * @param context 文档完成事件上下文
     * @return 文档完成链路事件集合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    List<OcrEvent> completionEvents(CompletionEventContext context) {
        return List.of(startedEvent(context), pageCompletedEvent(context), saveTextEvent(context),
                completedEvent(context));
    }

    /**
     * 构建文档开始事件。
     *
     * @param context 文档完成事件上下文
     * @return 文档开始事件
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private OcrEvent startedEvent(CompletionEventContext context) {
        return event(new DocumentEventPlan(context.started(), DocLensConstants.EVENT_DOCUMENT_STARTED,
                Map.of("percent", DocLensConstants.START_PROGRESS_PERCENT), Map.of()));
    }

    /**
     * 构建图片页完成事件。
     *
     * @param context 文档完成事件上下文
     * @return 图片页完成事件
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private OcrEvent pageCompletedEvent(CompletionEventContext context) {
        return event(new DocumentEventPlan(context.progressed(), DocLensConstants.EVENT_DOCUMENT_PAGE_COMPLETED,
                pageProgress(context.progressed()), Map.of()));
    }

    /**
     * 构建保存文本阶段事件。
     *
     * @param context 文档完成事件上下文
     * @return 保存文本阶段事件
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private OcrEvent saveTextEvent(CompletionEventContext context) {
        return event(new DocumentEventPlan(context.saving(), DocLensConstants.EVENT_DOCUMENT_STAGE_CHANGED,
                pageProgress(context.saving()), Map.of("stage", context.saving().stage().name().toLowerCase())));
    }

    /**
     * 构建文档完成事件。
     *
     * @param context 文档完成事件上下文
     * @return 文档完成事件
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private OcrEvent completedEvent(CompletionEventContext context) {
        return event(new DocumentEventPlan(context.completed(), DocLensConstants.EVENT_DOCUMENT_COMPLETED,
                Map.of("percent", DocLensConstants.COMPLETED_PROGRESS_PERCENT),
                resultSummary(context.completed(), context.result(), context.batch())));
    }

    /**
     * 构建文档失败事件。
     *
     * @param failed 失败文档任务
     * @param ex 失败异常
     * @return 文档失败事件
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    OcrEvent failedEvent(DocumentJob failed, RuntimeException ex) {
        return event(new DocumentEventPlan(failed, DocLensConstants.EVENT_DOCUMENT_FAILED,
                Map.of("percent", DocLensConstants.COMPLETED_PROGRESS_PERCENT),
                Map.of("code", DocLensConstants.ERROR_CODE_OCR_FAILED, "message", ex.getMessage())));
    }

    /**
     * 构建批次结束事件。
     *
     * @param batchId 批次 ID
     * @param status 批次状态
     * @param first 批次首个文档
     * @return 批次结束事件
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    OcrEvent batchFinishedEvent(String batchId, BatchStatus status, DocumentJob first) {
        return eventFactory.create(new EventCreateRequest(batchId, Optional.empty(),
                BATCH_EVENT_PREFIX + status.name().toLowerCase(), status.name().toLowerCase(), status.name().toLowerCase(),
                Map.of("percent", DocLensConstants.COMPLETED_PROGRESS_PERCENT), first.metadata(), Optional.empty(),
                Map.of(), Map.of()));
    }

    /**
     * 构建文档页进度载荷。
     *
     * @param document 文档任务
     * @return 页进度载荷
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private Map<String, Object> pageProgress(DocumentJob document) {
        return Map.of("percent", document.progressPercent(), "current_page", document.currentPage(),
                "total_pages", document.totalPages());
    }

    /**
     * 构建文档完成事件的结果摘要。
     *
     * @param document 文档任务
     * @param result OCR 结果
     * @param batch 可选批次
     * @return 结果摘要
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private Map<String, Object> resultSummary(DocumentJob document, OcrResult result, Optional<Batch> batch) {
        Map<String, Object> summary = new LinkedHashMap<>(RESULT_SUMMARY_CAPACITY);
        summary.put(PAGE_COUNT_FIELD, result.pageText().size());
        summary.put(BLOCK_COUNT_FIELD, result.layoutBlocks().size());
        summary.put(TABLE_COUNT_FIELD, result.tables().size());
        summary.put(CONFIDENCE_FIELD, result.confidence());
        summary.put(CALLBACK_BODY_FIELD, callbackBody(document, result, batch));
        return summary;
    }

    /**
     * 构建解析完成后的回调 body。
     *
     * @param document 文档任务
     * @param result OCR 结果
     * @param batch 可选批次
     * @return 回调 body
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private Map<String, Object> callbackBody(DocumentJob document, OcrResult result, Optional<Batch> batch) {
        if (batch.isPresent()) {
            // 批次存在时使用上传时保存的幂等键构造回调契约。
            return DocumentCompletedCallbackBody.from(batch.get(), document, result).toMap();
        } else {
            // 批次缺失时仍返回稳定契约，避免回调消费方收到不完整结构。
            return new DocumentCompletedCallbackBody(document.metadata().values(), result.finalText(), "").toMap();
        }
    }

    /**
     * 创建 OCR 事件。
     *
     * @param plan 事件构建参数
     * @return OCR 事件
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private OcrEvent event(DocumentEventPlan plan) {
        DocumentJob document = plan.document();
        return eventFactory.create(new EventCreateRequest(document.batchId(), Optional.of(document.documentId()),
                plan.eventType(), document.status().name().toLowerCase(), document.stage().name().toLowerCase(),
                plan.progress(), document.metadata(), document.resultId(), summaryDetail(plan), errorDetail(plan)));
    }

    /**
     * 构建普通事件详情。
     *
     * @param plan 事件构建参数
     * @return 普通事件详情
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private Map<String, Object> summaryDetail(DocumentEventPlan plan) {
        if (!DocLensConstants.EVENT_DOCUMENT_FAILED.equals(plan.eventType())) {
            // 非失败事件的 detail 放入 summary，便于统一展示进度详情。
            return plan.detail();
        } else {
            // 失败事件的 detail 仅作为 error 输出，summary 保持为空。
            return Map.of();
        }
    }

    /**
     * 构建错误事件详情。
     *
     * @param plan 事件构建参数
     * @return 错误事件详情
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private Map<String, Object> errorDetail(DocumentEventPlan plan) {
        if (DocLensConstants.EVENT_DOCUMENT_FAILED.equals(plan.eventType())) {
            // 失败事件的 detail 放入 error，方便前端直接展示错误原因。
            return plan.detail();
        } else {
            // 非失败事件没有错误详情。
            return Map.of();
        }
    }

    /**
     * 文档完成事件构建上下文。
     *
     * @param started 开始处理阶段文档
     * @param progressed 图片页完成阶段文档
     * @param saving 保存文本阶段文档
     * @param completed 完成阶段文档
     * @param result OCR 结果
     * @param batch 可选批次
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    record CompletionEventContext(
            DocumentJob started,
            DocumentJob progressed,
            DocumentJob saving,
            DocumentJob completed,
            OcrResult result,
            Optional<Batch> batch
    ) {
    }
}
