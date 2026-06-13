package io.github.lvdaxianer.doclens.j.processing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTask;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.processing.domain.ProcessingStage;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 文档页任务预处理服务测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
class DocumentPageTaskPreparationServiceTest {

    /**
     * 预处理应创建有序页任务，但不能执行 OCR。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void preparationCreatesOrderedPageTasksWithoutRunningOcr() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryDocumentPageTaskRepository pageTaskRepository = new InMemoryDocumentPageTaskRepository();
        RecordingPageImagePreparation pageImagePreparation = new RecordingPageImagePreparation();
        DocumentPageTaskPreparationService service = service(documentRepository, pageTaskRepository,
                pageImagePreparation);
        DocumentJob document = document("doc-1");
        documentRepository.save(document);

        PreparedDocumentPages prepared = service.prepare(document);

        assertThat(pageImagePreparation.prepareCalls).isEqualTo(1);
        assertThat(prepared.documentId()).isEqualTo("doc-1");
        assertThat(prepared.pageImages()).extracting(PageImageRef::pageNo).containsExactly(1, 2, 3);
        assertThat(pageTaskRepository.listByDocumentId("doc-1"))
                .extracting(DocumentPageTask::pageNo, DocumentPageTask::status)
                .containsExactly(
                        tuple(1, DocumentPageTaskStatus.QUEUED),
                        tuple(2, DocumentPageTaskStatus.QUEUED),
                        tuple(3, DocumentPageTaskStatus.QUEUED)
                );
        assertThat(documentRepository.findById("doc-1")).get().satisfies(updated -> {
            assertThat(updated.status()).isEqualTo(DocumentStatus.PROCESSING);
            assertThat(updated.stage()).isEqualTo(ProcessingStage.OCR_QUEUED);
            assertThat(updated.totalPages()).isEqualTo(3);
        });
    }

    /**
     * 创建测试预处理服务。
     *
     * @param documentRepository 文档仓储
     * @param pageTaskRepository 页任务仓储
     * @param pageImagePreparation 页图片准备器
     * @return 预处理服务
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private DocumentPageTaskPreparationService service(
            DocumentJobRepository documentRepository,
            DocumentPageTaskRepository pageTaskRepository,
            PageImagePreparation pageImagePreparation
    ) {
        return new DocumentPageTaskPreparationService(documentRepository, pageTaskRepository, pageImagePreparation,
                new FixedPageTaskIdGenerator());
    }

    /**
     * 创建测试文档。
     *
     * @param documentId 文档 ID
     * @return 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private DocumentJob document(String documentId) {
        return DocumentJob.create(new DocumentJobCreateRequest(documentId, "batch-1", documentId + ".pdf",
                DocumentType.PDF, 128L, 3, "storage://" + documentId, "stub_ocr", Optional.empty(),
                JsonPayload.empty(), 0, OffsetDateTime.parse("2026-06-10T10:00:00+08:00")));
    }

    /**
     * 固定返回三页图片引用的准备器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static class RecordingPageImagePreparation implements PageImagePreparation {

        private int prepareCalls;

        /**
         * 准备测试页图片引用。
         *
         * @param document 文档任务
         * @return 页图片引用集合
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        @Override
        public List<PageImageRef> prepare(DocumentJob document) {
            prepareCalls++;
            return List.of(
                    new PageImageRef(1, "page://doc-1/1"),
                    new PageImageRef(2, "page://doc-1/2"),
                    new PageImageRef(3, "page://doc-1/3")
            );
        }
    }

    /**
     * 固定生成页任务 ID 的生成器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static class FixedPageTaskIdGenerator extends IdGenerator {

        private int sequence;

        /**
         * 生成可预测的页任务 ID。
         *
         * @return 页任务 ID
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        public String newPageTaskId() {
            sequence++;
            return "page-task-" + sequence;
        }
    }

}
