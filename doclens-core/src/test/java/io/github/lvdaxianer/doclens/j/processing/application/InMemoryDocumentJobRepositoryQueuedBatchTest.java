package io.github.lvdaxianer.doclens.j.processing.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 内存文档仓储排队批次查询测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
class InMemoryDocumentJobRepositoryQueuedBatchTest {

    private static final int RECOVERY_LIMIT = 2;
    private static final int PAGE_COUNT = 1;
    private static final long FILE_SIZE = 8L;

    /**
     * 查询排队批次时应先按批次去重，再应用恢复数量限制。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void listQueuedBatchIdsAppliesLimitAfterDistinctBatchIds() {
        InMemoryDocumentJobRepository repository = new InMemoryDocumentJobRepository();
        repository.save(queuedDocument("doc-1", "batch-1", 0));
        repository.save(queuedDocument("doc-2", "batch-1", 1));
        repository.save(queuedDocument("doc-3", "batch-2", 2));

        assertThat(repository.listQueuedBatchIds(RECOVERY_LIMIT)).containsExactly("batch-1", "batch-2");
    }

    /**
     * 创建排队文档。
     *
     * @param documentId 文档 ID
     * @param batchId 批次 ID
     * @param sortOrder 上传顺序
     * @return 排队文档
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private DocumentJob queuedDocument(String documentId, String batchId, int sortOrder) {
        return DocumentJob.create(new DocumentJobCreateRequest(documentId, batchId, documentId + ".txt",
                DocumentType.TEXT, FILE_SIZE, PAGE_COUNT, "local://" + documentId, "stub_ocr", Optional.empty(),
                JsonPayload.empty(), sortOrder, OffsetDateTime.now()));
    }
}
