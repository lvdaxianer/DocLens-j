package io.github.lvdaxianer.doclens.j.processing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.processing.application.ChunkStrategy;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 文档任务进度计算测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
class DocumentJobProgressTest {

    private static final int LARGE_DOCUMENT_TOTAL_PAGES = 677;
    private static final int COMPLETED_OCR_PAGES = 6;

    /**
     * 大文档 OCR 已完成少量页时，进度应高于启动下限。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void markPageCompletedIncreasesProgressForLargeDocument() {
        DocumentJob document = document(LARGE_DOCUMENT_TOTAL_PAGES).startProcessing(OffsetDateTime.now());

        DocumentJob progressed = document.markPageCompleted(COMPLETED_OCR_PAGES, LARGE_DOCUMENT_TOTAL_PAGES,
                OffsetDateTime.now());

        assertThat(progressed.progressPercent()).isGreaterThan(DocLensConstants.START_PROGRESS_PERCENT);
    }

    /**
     * 创建指定页数的测试文档任务。
     *
     * @param pageCount 文档页数
     * @return 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private DocumentJob document(int pageCount) {
        DocumentJobCreateRequest request = new DocumentJobCreateRequest("doc-progress", "batch-progress",
                "progress.pdf", DocumentType.PDF, 1, pageCount, "local://progress.pdf", "stub_ocr",
                Optional.empty(), JsonPayload.empty(), 0, OffsetDateTime.now(), ChunkStrategy.general());
        return DocumentJob.create(request);
    }
}
