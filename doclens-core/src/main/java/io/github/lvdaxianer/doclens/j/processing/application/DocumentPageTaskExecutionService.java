package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrRouteExecutionResult;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRunningPageTaskCommand;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRunningPageTaskTracker;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingService;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResult;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResultRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTask;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskClaimRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskCompletionRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskFailureRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskRepository;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;
import java.time.OffsetDateTime;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文档页任务执行服务。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
public class DocumentPageTaskExecutionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentPageTaskExecutionService.class);
    private static final String PAGE_OCR_FAILED_CODE = "PAGE_OCR_FAILED";

    private final DocumentPageTaskRepository pageTaskRepository;
    private final DocumentJobRepository documentRepository;
    private final DocumentPageResultRepository pageResultRepository;
    private final ObjectStorage objectStorage;
    private final OcrRoutingService routingService;
    private final OcrRunningPageTaskTracker runningPageTaskTracker;
    private final TransactionRunner transactionRunner;
    private final String workerId;
    private final int workerBatchSize;
    private final int lockSeconds;
    private final Executor pageTaskExecutor;
    private final Consumer<DocumentPageTask> pageSuccessListener;

    /**
     * 创建文档页任务执行服务。
     *
     * @param dependencies 执行依赖
     * @param transactionRunner 事务执行器
     * @param options 执行配置
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    public DocumentPageTaskExecutionService(
            DocumentPageTaskExecutionDependencies dependencies,
            TransactionRunner transactionRunner,
            DocumentPageTaskExecutionOptions options
    ) {
        this.pageTaskRepository = dependencies.pageTaskRepository();
        this.documentRepository = dependencies.documentRepository();
        this.pageResultRepository = dependencies.pageResultRepository();
        this.objectStorage = dependencies.objectStorage();
        this.routingService = dependencies.routingService();
        this.runningPageTaskTracker = dependencies.runningPageTaskTracker();
        this.transactionRunner = transactionRunner;
        this.workerId = options.workerId();
        this.workerBatchSize = options.workerBatchSize();
        this.lockSeconds = options.lockSeconds();
        this.pageTaskExecutor = options.pageTaskExecutor();
        this.pageSuccessListener = options.pageSuccessListener();
    }

    /**
     * 执行一轮等待页任务扫描。
     *
     * @return 本轮成功抢占的任务数
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    public int runOnce() {
        return pageTaskRepository.listQueued(workerBatchSize).stream()
                .filter(this::tryClaim)
                .mapToInt(this::submitClaimed)
                .sum();
    }

    /**
     * 原子抢占页任务。
     *
     * @param task 页任务
     * @return 是否抢占成功
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private boolean tryClaim(DocumentPageTask task) {
        OffsetDateTime now = OffsetDateTime.now();
        DocumentPageTaskClaimRequest request = new DocumentPageTaskClaimRequest(task.taskId(), workerId,
                now.plusSeconds(lockSeconds), now);
        return pageTaskRepository.tryMarkProcessing(request);
    }

    /**
     * 执行已抢占页任务。
     *
     * @param task 页任务
     * @return 已处理任务数
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private int submitClaimed(DocumentPageTask task) {
        pageTaskExecutor.execute(() -> executeClaimed(task));
        return 1;
    }

    /**
     * 执行已抢占页任务。
     *
     * @param task 页任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void executeClaimed(DocumentPageTask task) {
        try {
            LOGGER.info("[页任务OCR] 开始执行页任务 taskId={}, documentId={}, pageNo={}", task.taskId(),
                    task.documentId(), task.pageNo());
            executeOcr(task);
            LOGGER.info("[页任务OCR] 页任务执行成功 taskId={}, documentId={}, pageNo={}", task.taskId(),
                    task.documentId(), task.pageNo());
        } catch (RuntimeException ex) {
            markFailed(task, ex);
        }
    }

    /**
     * 执行单页 OCR 并持久化页结果。
     *
     * @param task 已抢占页任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void executeOcr(DocumentPageTask task) {
        DocumentJob document = document(task);
        ImageOcrRequest request = imageRequest(document, task);
        recordRunningPageStart(task);
        try {
            OcrRouteExecutionResult routeResult = routingService.recognize(request, document.ocrRoutePolicy());
            transactionRunner.requiredVoid(() -> completeTask(task, routeResult));
            notifyPageSuccess(task);
        } finally {
            recordRunningPageCompletion(task);
        }
    }

    /**
     * 记录运行中的图片页任务。
     *
     * @param task 页任务
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private void recordRunningPageStart(DocumentPageTask task) {
        try {
            runningPageTaskTracker.recordStart(new OcrRunningPageTaskCommand(task.taskId(), task.batchId(),
                    task.documentId(), task.pageNo(), workerId, Thread.currentThread().getName(), OffsetDateTime.now()));
        } catch (RuntimeException ex) {
            LOGGER.warn("[页任务OCR] 记录运行中页任务失败 taskId={}, documentId={}, pageNo={}", task.taskId(),
                    task.documentId(), task.pageNo(), ex);
        }
    }

    /**
     * 清理运行中的图片页任务。
     *
     * @param task 页任务
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private void recordRunningPageCompletion(DocumentPageTask task) {
        try {
            runningPageTaskTracker.recordCompletion(task.taskId());
        } catch (RuntimeException ex) {
            LOGGER.warn("[页任务OCR] 清理运行中页任务失败 taskId={}, documentId={}, pageNo={}", task.taskId(),
                    task.documentId(), task.pageNo(), ex);
        }
    }

    /**
     * 查询页任务所属文档。
     *
     * @param task 页任务
     * @return 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentJob document(DocumentPageTask task) {
        return documentRepository.findById(task.documentId())
                .orElseThrow(() -> new IllegalStateException("document not found: " + task.documentId()));
    }

    /**
     * 构建图片 OCR 请求。
     *
     * @param document 文档任务
     * @param task 页任务
     * @return 图片 OCR 请求
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private ImageOcrRequest imageRequest(DocumentJob document, DocumentPageTask task) {
        byte[] imageContent = objectStorage.readBytes(task.imageStorageUri());
        return new ImageOcrRequest(task.batchId(), task.taskId(), task.documentId(), document.fileName(), task.pageNo(),
                imageContent, document.metadata());
    }

    /**
     * 写入页结果并标记页任务完成。
     *
     * @param task 页任务
     * @param routeResult OCR 路由结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void completeTask(DocumentPageTask task, OcrRouteExecutionResult routeResult) {
        OffsetDateTime now = OffsetDateTime.now();
        pageResultRepository.upsert(pageResult(task, routeResult, now));
        pageTaskRepository.markCompleted(new DocumentPageTaskCompletionRequest(task.taskId(), workerId, now));
    }

    /**
     * 构建文档页 OCR 结果。
     *
     * @param task 页任务
     * @param routeResult OCR 路由结果
     * @param now 当前时间
     * @return 文档页 OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentPageResult pageResult(
            DocumentPageTask task,
            OcrRouteExecutionResult routeResult,
            OffsetDateTime now
    ) {
        ImageOcrResult result = routeResult.result();
        return new DocumentPageResult(task.documentId(), task.pageNo(), result.rawOutput(), pageText(result),
                result.layoutBlocks(), result.confidence(), result.warnings(), routeResult.nodeId(), now, now);
    }

    /**
     * 合并页文本块为页文本。
     *
     * @param result 图片 OCR 结果
     * @return 页文本
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private String pageText(ImageOcrResult result) {
        return result.pageText().stream()
                .map(block -> String.valueOf(block.getOrDefault("text", "")))
                .collect(Collectors.joining("\n"));
    }

    /**
     * 通知页任务成功监听器。
     *
     * @param task 页任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void notifyPageSuccess(DocumentPageTask task) {
        try {
            pageSuccessListener.accept(task);
        } catch (RuntimeException ex) {
            LOGGER.warn("[页任务OCR] 页任务成功后聚合回调失败 taskId={}, documentId={}, pageNo={}, error={}",
                    task.taskId(), task.documentId(), task.pageNo(), ex.getMessage(), ex);
        }
    }

    /**
     * 标记页任务失败。
     *
     * @param task 页任务
     * @param ex OCR 异常
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void markFailed(DocumentPageTask task, RuntimeException ex) {
        LOGGER.warn("[页任务OCR] 页任务执行失败 taskId={}, documentId={}, pageNo={}, error={}", task.taskId(),
                task.documentId(), task.pageNo(), ex.getMessage(), ex);
        transactionRunner.requiredVoid(() -> pageTaskRepository.markFailed(new DocumentPageTaskFailureRequest(
                task.taskId(), PAGE_OCR_FAILED_CODE, ex.getMessage(), OffsetDateTime.now())));
    }
}
