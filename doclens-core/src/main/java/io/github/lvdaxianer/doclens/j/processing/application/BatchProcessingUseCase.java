package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentOcrResultBuilder.DocumentOcrResultBuilderDependencies;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentProcessingEventBuilder.CompletionEventContext;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResult;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.ProcessingStage;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 并发处理单个批次内排队文档的用例。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public class BatchProcessingUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchProcessingUseCase.class);

    private final DocumentJobRepository documentRepository;
    private final OcrResultRepository resultRepository;
    private final OcrEventRepository eventRepository;
    private final BatchRepository batchRepository;
    private final DocumentOcrResultBuilder resultBuilder;
    private final DocumentProcessingEventBuilder eventBuilder;
    private final DocumentPageTaskPreparationService pageTaskPreparationService;
    private final CallbackJobCreationService callbackJobCreationService;
    private final TransactionRunner transactionRunner;
    private final ExecutorService documentProcessingExecutor;

    /**
     * 创建批次处理用例。
     *
     * @param dependencies 用例依赖
     * @param transactionRunner 事务执行器
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public BatchProcessingUseCase(BatchProcessingDependencies dependencies, TransactionRunner transactionRunner) {
        this.documentRepository = dependencies.documentRepository();
        this.resultRepository = dependencies.resultRepository();
        this.eventRepository = dependencies.eventRepository();
        this.batchRepository = dependencies.batchRepository();
        this.resultBuilder = new DocumentOcrResultBuilder(new DocumentOcrResultBuilderDependencies(
                dependencies.adapterRegistry(), dependencies.objectStorage(), dependencies.documentTextExtractor(),
                dependencies.idGenerator(), dependencies.markdownPostProcessor(), transactionRunner,
                dependencies.documentRepository()));
        this.eventBuilder = new DocumentProcessingEventBuilder(dependencies.eventFactory());
        this.documentProcessingExecutor = dependencies.documentProcessingExecutor();
        this.pageTaskPreparationService = dependencies.pageTaskPreparationService();
        this.callbackJobCreationService = new CallbackJobCreationService(dependencies.callbackJobRepository(),
                dependencies.idGenerator());
        this.transactionRunner = transactionRunner;
    }

    /**
     * 并发处理批次中的排队文档。
     *
     * @param batchId 批次 ID
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public void processBatch(String batchId) {
        List<DocumentJob> documents = queuedDocuments(batchId);
        Optional<Batch> batch = batchRepository.findById(batchId);
        DocumentProcessingTaskBatch tasks = submitDocumentTasks(documents, batch);
        int submittedTaskCount = tasks.size();
        for (int index = 0; index < submittedTaskCount; index++) {
            awaitCompletedDocumentResult(tasks)
                    .ifPresent(result -> transactionRunner.requiredVoid(() -> persistDocumentProcessing(result)));
        }
        transactionRunner.requiredVoid(() -> finishBatch(batchId));
    }

    /**
     * 提交批次内文档任务，允许多个文档同时进入页级 OCR 队列。
     *
     * @param documents 待处理文档集合
     * @param batch 可选批次
     * @return 文档处理 Future 集合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentProcessingTaskBatch submitDocumentTasks(List<DocumentJob> documents, Optional<Batch> batch) {
        CompletionService<DocumentProcessingResult> completionService =
                new ExecutorCompletionService<>(documentProcessingExecutor);
        DocumentProcessingTaskBatch tasks = new DocumentProcessingTaskBatch(completionService, documents.size());
        for (DocumentJob document : documents) {
            Future<DocumentProcessingResult> future = completionService.submit(() -> processDocument(document, batch));
            tasks.add(future, document);
        }
        return tasks;
    }

    /**
     * 等待任意一个文档任务完成，并将线程池异常转换为文档失败结果。
     *
     * @param tasks 文档任务批次
     * @return 已完成文档处理结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private Optional<DocumentProcessingResult> awaitCompletedDocumentResult(DocumentProcessingTaskBatch tasks) {
        Future<DocumentProcessingResult> completedFuture = null;
        try {
            completedFuture = tasks.completionService().take();
            DocumentProcessingResult result = completedFuture.get();
            tasks.removeDocumentOf(completedFuture);
            return Optional.of(result);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.warn("[OCR处理] 等待文档处理被中断", ex);
            throw new IllegalStateException("document processing interrupted", ex);
        } catch (ExecutionException ex) {
            LOGGER.warn("[OCR处理] 文档线程执行异常 error={}", ex.getMessage(), ex);
            return Optional.of(failDocumentAfterExecutionException(tasks, completedFuture, ex));
        }
    }

    /**
     * 将 Future 执行异常转换为文档失败结果。
     *
     * @param tasks 文档任务批次
     * @param completedFuture 已完成 Future
     * @param ex Future 执行异常
     * @return 文档失败结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentProcessingResult failDocumentAfterExecutionException(
            DocumentProcessingTaskBatch tasks,
            Future<DocumentProcessingResult> completedFuture,
            ExecutionException ex
    ) {
        DocumentJob document = tasks.removeDocumentOf(completedFuture);
        return failDocument(document, new IllegalStateException("document processing execution failed", ex));
    }

    /**
     * 处理单个文档并转换为持久化计划。
     *
     * @param document 文档任务
     * @param batch 可选批次
     * @return 文档处理结果
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private DocumentProcessingResult processDocument(DocumentJob document, Optional<Batch> batch) {
        try {
            if (requiresPageOcrQueue(document)) {
                return queueDocumentPages(document);
            }
            // 文本类文档不需要 OCR 节点，继续沿用同步直通链路。
            DocumentJob started = startDocument(document);
            return completeDocument(started, batch);
        } catch (RuntimeException ex) {
            LOGGER.warn("[OCR处理] 文档处理失败 documentId={}, error={}", document.documentId(), ex.getMessage(), ex);
            return failDocument(document, ex);
        }
    }

    /**
     * 判断文档是否需要进入页级 OCR 队列。
     *
     * @param document 文档任务
     * @return 是否需要页级 OCR 队列
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private boolean requiresPageOcrQueue(DocumentJob document) {
        return document.fileType() == DocumentType.IMAGE
                || document.fileType() == DocumentType.PDF
                || document.fileType() == DocumentType.WORD;
    }

    /**
     * 准备文档页任务并等待 OCR worker 执行。
     *
     * @param document 文档任务
     * @return 文档入队结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private DocumentProcessingResult queueDocumentPages(DocumentJob document) {
        PreparedDocumentPages prepared = pageTaskPreparationService.prepare(document);
        DocumentJob queued = latestDocument(document);
        OcrEvent event = eventBuilder.ocrQueuedEvent(queued, prepared.pageImages().size());
        return new DocumentProcessingResult(queued, Optional.empty(), List.of(event));
    }

    /**
     * 标记文档开始处理。
     *
     * @param document 文档任务
     * @return 开始处理后的文档任务
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private DocumentJob startDocument(DocumentJob document) {
        return document.startProcessing(OffsetDateTime.now());
    }

    /**
     * 完成文档解析并构建结果。
     *
     * @param started 已开始处理的文档
     * @param batch 可选批次
     * @return 文档处理结果
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private DocumentProcessingResult completeDocument(DocumentJob started, Optional<Batch> batch) {
        OcrResult result = resultBuilder.build(started);
        int pageCount = Math.max(DocLensConstants.DEFAULT_PAGE_COUNT, result.pageText().size());
        DocumentJob progressed = started.markPageCompleted(pageCount, pageCount, OffsetDateTime.now());
        DocumentJob saving = progressed.advanceStage(ProcessingStage.SAVE_TEXT, pageCount, pageCount,
                OffsetDateTime.now());
        DocumentJob completed = saving.complete(result.resultId(), OffsetDateTime.now());
        CompletionEventContext eventContext = new CompletionEventContext(started, progressed, saving,
                completed, result, batch);
        return new DocumentProcessingResult(completed, Optional.of(result),
                eventBuilder.completionEvents(eventContext));
    }

    /**
     * 构建文档失败后的持久化计划。
     *
     * @param document 文档任务
     * @param ex 失败异常
     * @return 文档失败处理结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentProcessingResult failDocument(DocumentJob document, RuntimeException ex) {
        DocumentJob failed = latestDocument(document).fail(DocLensConstants.ERROR_CODE_OCR_FAILED, ex.getMessage(),
                OffsetDateTime.now());
        return new DocumentProcessingResult(failed, Optional.empty(), List.of(eventBuilder.failedEvent(failed, ex)));
    }

    /**
     * 查询文档最新状态。
     *
     * @param document 原文档任务
     * @return 最新文档任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentJob latestDocument(DocumentJob document) {
        return documentRepository.findById(document.documentId()).orElse(document);
    }

    /**
     * 读取批次内仍需执行的排队文档。
     *
     * @param batchId 批次 ID
     * @return 待处理文档集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private List<DocumentJob> queuedDocuments(String batchId) {
        return documentRepository.listByBatchId(batchId).stream()
                .filter(document -> document.status() == DocumentStatus.QUEUED)
                .toList();
    }

    /**
     * 持久化单个文档处理结果。
     *
     * @param result 文档处理结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    void persistDocumentProcessing(DocumentProcessingResult result) {
        documentRepository.updateAll(List.of(result.document()));
        resultRepository.saveAll(result.result().stream().toList());
        eventRepository.saveAll(result.events());
        batchRepository.findById(result.document().batchId())
                .flatMap(Batch::callbackUrl)
                .ifPresent(callbackUrl -> callbackJobCreationService.saveForCompletedEvents(result.events(),
                        callbackUrl));
    }

    /**
     * 汇总并完成批次状态。
     *
     * @param batchId 批次 ID
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void finishBatch(String batchId) {
        List<DocumentJob> documents = documentRepository.listByBatchId(batchId);
        long completed = documents.stream().filter(document -> document.status() == DocumentStatus.COMPLETED).count();
        long failed = documents.stream().filter(document -> document.status().isFailureLike()).count();
        BatchStatus status = resolveBatchStatus(documents.size(), completed, failed);
        batchRepository.updateSummary(batchId, Math.toIntExact(completed), Math.toIntExact(failed), status);
        if (!documents.isEmpty()) {
            DocumentJob first = documents.getFirst();
            eventRepository.save(eventBuilder.batchFinishedEvent(batchId, status, first));
        } else {
            // 接收入库用例不会创建空批次。
            LOGGER.warn("[OCR处理] 完成批次时未找到文档 batchId={}", batchId);
        }
    }

    /**
     * 根据批次内文档终态数量解析批次状态。
     *
     * @param total 文档总数
     * @param completed 完成数量
     * @param failed 失败数量
     * @return 批次状态
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private BatchStatus resolveBatchStatus(int total, long completed, long failed) {
        if (completed + failed < total) {
            // 存在未完成的异步页任务时，批次仍处于处理中。
            return BatchStatus.PROCESSING;
        } else if (failed == 0 && completed == total) {
            // 全部文档成功完成时，批次完成。
            return BatchStatus.COMPLETED;
        } else if (completed == 0 && failed == total) {
            // 全部文档失败时，批次失败。
            return BatchStatus.FAILED;
        } else {
            // 成功和失败文档同时存在时，批次部分失败。
            return BatchStatus.PARTIAL_FAILED;
        }
    }
}
