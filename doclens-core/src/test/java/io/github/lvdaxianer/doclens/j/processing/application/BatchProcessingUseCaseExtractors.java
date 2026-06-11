package io.github.lvdaxianer.doclens.j.processing.application;

import static io.github.lvdaxianer.doclens.j.processing.application.BatchProcessingUseCaseTestSupport.CONCURRENT_TEST_DOCUMENTS;
import static io.github.lvdaxianer.doclens.j.processing.application.BatchProcessingUseCaseTestSupport.CONCURRENT_TEST_TIMEOUT_SECONDS;
import static io.github.lvdaxianer.doclens.j.processing.application.BatchProcessingUseCaseTestSupport.PROCESSED_DOCUMENT_ID_CAPACITY;

import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractionRequest;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractionResult;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractor;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.ProcessingStage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 批次处理测试提取器集合。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
final class BatchProcessingUseCaseExtractors {

    /**
     * 禁止实例化测试提取器集合。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private BatchProcessingUseCaseExtractors() {
    }
}

/**
 * 记录进度实时可见性的提取器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
class RecordingDocumentTextExtractor implements DocumentTextExtractor {

    private final DocumentJobRepository documentRepository;
    private boolean stageWasVisibleDuringExtraction;
    private boolean currentPageWasVisibleDuringExtraction;

    /**
     * 创建记录型文本提取器。
     *
     * @param documentRepository 文档仓储
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    RecordingDocumentTextExtractor(DocumentJobRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Override
    public DocumentTextExtractionResult extract(DocumentTextExtractionRequest request) {
        if ("doc-1".equals(request.document().documentId())) {
            // 观测文档主动上报进度，用于验证进度落库实时可见。
            reportAndCaptureProgress(request);
        } else {
            // 非观测文档只返回文本，避免把并发语义锁回文档串行。
        }
        return plainTextResult(request);
    }

    /**
     * 返回阶段进度是否在提取期间可见。
     *
     * @return 阶段进度是否可见
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    boolean stageWasVisibleDuringExtraction() {
        return stageWasVisibleDuringExtraction;
    }

    /**
     * 返回页进度是否在提取期间可见。
     *
     * @return 页进度是否可见
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    boolean currentPageWasVisibleDuringExtraction() {
        return currentPageWasVisibleDuringExtraction;
    }

    /**
     * 上报进度并捕获仓储中的实时状态。
     *
     * @param request 文本提取请求
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void reportAndCaptureProgress(DocumentTextExtractionRequest request) {
        request.progressReporter().report(ProcessingStage.OCR_IMAGES, 2, 5);
        stageWasVisibleDuringExtraction = documentRepository.findById("doc-1")
                .filter(document -> document.stage() == ProcessingStage.OCR_IMAGES)
                .isPresent();
        currentPageWasVisibleDuringExtraction = documentRepository.findById("doc-1")
                .filter(document -> document.currentPage() == 2)
                .filter(document -> document.totalPages() == 5)
                .isPresent();
    }

    /**
     * 创建普通文本提取结果。
     *
     * @param request 文本提取请求
     * @return 文本提取结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentTextExtractionResult plainTextResult(DocumentTextExtractionRequest request) {
        return DocumentTextExtractionResult.plainText(request.document().documentId(),
                request.document().fileName(), "text-" + request.document().documentId());
    }
}

/**
 * 先上报进度再失败的文本提取器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
class FailingDocumentTextExtractor implements DocumentTextExtractor {

    @Override
    public DocumentTextExtractionResult extract(DocumentTextExtractionRequest request) {
        request.progressReporter().report(ProcessingStage.OCR_IMAGES, 2, 5);
        throw new IllegalStateException("ocr failed");
    }
}

/**
 * 阻塞型文本提取器，用于验证批次是否能同时启动多个文档。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
class BlockingDocumentTextExtractor implements DocumentTextExtractor {

    private final CountDownLatch entered = new CountDownLatch(CONCURRENT_TEST_DOCUMENTS);
    private final CountDownLatch release = new CountDownLatch(1);

    /**
     * 等待两个文档都进入提取器。
     *
     * @return 两个文档是否都进入提取器
     * @throws InterruptedException 等待被中断时抛出
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    boolean awaitBothEntered() throws InterruptedException {
        return entered.await(CONCURRENT_TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 释放阻塞中的文档提取。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    void release() {
        release.countDown();
    }

    @Override
    public DocumentTextExtractionResult extract(DocumentTextExtractionRequest request) {
        entered.countDown();
        awaitRelease();
        return DocumentTextExtractionResult.plainText(request.document().documentId(),
                request.document().fileName(), "text-" + request.document().documentId());
    }

    /**
     * 等待测试释放提取流程。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void awaitRelease() {
        try {
            release.await(CONCURRENT_TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("blocking extractor interrupted", ex);
        }
    }
}

/**
 * 首个文档阻塞、第二个文档快速完成的提取器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
class SlowFirstDocumentTextExtractor implements DocumentTextExtractor {

    private final CountDownLatch releaseSlow = new CountDownLatch(1);
    private final CountDownLatch fastCompleted = new CountDownLatch(1);

    /**
     * 等待快文档完成提取。
     *
     * @return 快文档是否完成提取
     * @throws InterruptedException 等待被中断时抛出
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    boolean awaitFastCompleted() throws InterruptedException {
        return fastCompleted.await(CONCURRENT_TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 释放慢文档提取。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    void releaseSlow() {
        releaseSlow.countDown();
    }

    @Override
    public DocumentTextExtractionResult extract(DocumentTextExtractionRequest request) {
        if ("doc-slow".equals(request.document().documentId())) {
            // 慢文档阻塞时，快文档仍应能完成并先落库。
            awaitSlowRelease();
        } else {
            // 快文档完成信号用于测试观察持久化是否被慢文档阻塞。
            fastCompleted.countDown();
        }
        return DocumentTextExtractionResult.plainText(request.document().documentId(),
                request.document().fileName(), "text-" + request.document().documentId());
    }

    /**
     * 等待慢文档释放。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void awaitSlowRelease() {
        try {
            releaseSlow.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("slow extractor interrupted", ex);
        }
    }
}

/**
 * 返回固定文本的提取器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
record FixedTextExtractor(String text) implements DocumentTextExtractor {

    @Override
    public DocumentTextExtractionResult extract(DocumentTextExtractionRequest request) {
        return DocumentTextExtractionResult.plainText(request.document().documentId(),
                request.document().fileName(), text);
    }
}

/**
 * 记录实际被处理文档 ID 的提取器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
class CountingDocumentTextExtractor implements DocumentTextExtractor {

    private final List<String> processedDocumentIds = new ArrayList<>(PROCESSED_DOCUMENT_ID_CAPACITY);

    @Override
    public DocumentTextExtractionResult extract(DocumentTextExtractionRequest request) {
        processedDocumentIds.add(request.document().documentId());
        return DocumentTextExtractionResult.plainText(request.document().documentId(),
                request.document().fileName(), "text-" + request.document().documentId());
    }

    /**
     * 返回已处理文档 ID。
     *
     * @return 已处理文档 ID
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    List<String> processedDocumentIds() {
        return processedDocumentIds;
    }
}
