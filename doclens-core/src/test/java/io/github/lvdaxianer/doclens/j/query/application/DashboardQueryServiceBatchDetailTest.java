package io.github.lvdaxianer.doclens.j.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.BASE_TIME;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.TEST_CALLBACK_URL;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.TEST_IDEMPOTENCY_KEY;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.TEST_METADATA_FILE_ID;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.TEST_METADATA_SOURCE;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.batch;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.batchWithIntakeInfo;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.completedDocument;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.queuedDocument;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.stagedDocument;

import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.InMemoryBatchRepository;
import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.InMemoryDocumentJobRepository;
import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.InMemoryOcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.processing.domain.ProcessingStage;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Dashboard 批次详情查询服务测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
class DashboardQueryServiceBatchDetailTest {

    /**
     * 批次详情应按上传顺序返回文档，并包含文件类型对应的处理轨道。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void batchDetailReturnsOrderedDocumentsAndProcessingTrack() {
        DashboardQueryService service = serviceWithDocuments(List.of(
                completedDocument("doc-word", DocumentType.WORD, 1),
                completedDocument("doc-text", DocumentType.TEXT, 0)
        ));

        List<?> documents = documentsOf(service.batchDetail("batch-test"));

        assertThat(documentIdsOf(documents)).containsExactly("doc-text", "doc-word");
        assertThat(statesOf((Map<?, ?>) documents.get(1)))
                .containsExactly("done", "done", "done", "done", "done", "done", "done", "done");
        assertThat(statesOf((Map<?, ?>) documents.get(0)))
                .containsExactly("done", "done", "skipped", "skipped", "skipped", "skipped", "skipped", "done");
    }

    /**
     * 批次详情应暴露创建时传入的接入信息。
     *
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void batchDetailExposesBatchIntakeInfo() {
        DashboardQueryService service = new DashboardQueryService(
                new InMemoryBatchRepository(List.of(batchWithIntakeInfo())),
                new InMemoryDocumentJobRepository(List.of(completedDocument("doc-text", DocumentType.TEXT, 0))),
                new InMemoryOcrEventRepository());

        Map<?, ?> batch = batchOf(service.batchDetail("batch-test"));
        Map<?, ?> metadata = (Map<?, ?>) batch.get("metadata");

        assertThat(batch.get("callback_url")).isEqualTo(TEST_CALLBACK_URL);
        assertThat(batch.get("idempotency_key")).isEqualTo(TEST_IDEMPOTENCY_KEY);
        assertThat(metadata.get("source")).isEqualTo(TEST_METADATA_SOURCE);
        assertThat(metadata.get("openwebui_file_id")).isEqualTo(TEST_METADATA_FILE_ID);
    }

    /**
     * 批次详情应展示待解析文档的当前步骤和未执行步骤。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void batchDetailShowsQueuedDocumentTrackStates() {
        DashboardQueryService service = serviceWithDocuments(List.of(
                queuedDocument("doc-pdf", DocumentType.PDF, 0)
        ));

        List<?> documents = documentsOf(service.batchDetail("batch-test"));

        assertThat(statesOf((Map<?, ?>) documents.getFirst()))
                .containsExactly("done", "current", "skipped", "pending", "pending", "pending", "pending",
                        "pending");
    }

    /**
     * 批次详情应展示 OCR 处理中和 OCR 失败节点。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void batchDetailShowsOcrProcessingAndFailedTrackStates() {
        DashboardQueryService service = serviceWithDocuments(List.of(
                stagedDocument("doc-processing", DocumentType.PDF, ProcessingStage.OCR_IMAGES, 2, 5, 0),
                stagedDocument("doc-failed", DocumentType.PDF, ProcessingStage.OCR_IMAGES, 2, 5, 1)
                        .fail("OCR_FAILED", "ocr failed", BASE_TIME.plusSeconds(7))
        ));

        List<?> documents = documentsOf(service.batchDetail("batch-test"));

        assertThat(statesOf((Map<?, ?>) documents.get(0)))
                .containsExactly("done", "done", "skipped", "done", "current", "pending", "pending", "pending");
        assertThat(statesOf((Map<?, ?>) documents.get(1)))
                .containsExactly("done", "done", "skipped", "done", "failed", "pending", "pending", "pending");
    }

    /**
     * 批次详情应按失败前阶段展示转换失败节点。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void batchDetailShowsConversionFailedTrackState() {
        DashboardQueryService service = serviceWithDocuments(List.of(
                stagedDocument("doc-failed", DocumentType.WORD, ProcessingStage.WORD_TO_PDF, 0, 1, 0)
                        .fail("WORD_TO_PDF_FAILED", "convert failed", BASE_TIME.plusSeconds(7))
        ));

        List<?> documents = documentsOf(service.batchDetail("batch-test"));

        assertThat(statesOf((Map<?, ?>) documents.getFirst()))
                .containsExactly("done", "done", "failed", "pending", "pending", "pending", "pending", "pending");
    }

    /**
     * 批次详情应展示独立的 LLM Markdown 后处理步骤。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void batchDetailShowsLlmMarkdownStepBetweenMergeAndSave() {
        DashboardQueryService service = serviceWithDocuments(List.of(
                stagedDocument("doc-word", DocumentType.WORD, ProcessingStage.SAVE_TEXT, 4, 4, 0)
        ));

        List<?> documents = documentsOf(service.batchDetail("batch-test"));

        assertThat(labelsOf((Map<?, ?>) documents.getFirst()))
                .containsExactly("上传", "类型识别", "转换", "渲染页图", "OCR", "合并文本", "LLM 排版", "入库/落盘");
        assertThat(statesOf((Map<?, ?>) documents.getFirst()))
                .containsExactly("done", "done", "done", "done", "done", "done", "current", "pending");
    }

    /**
     * 创建包含指定文档的批次详情查询服务。
     *
     * @param documents 测试文档列表
     * @return 批次详情查询服务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DashboardQueryService serviceWithDocuments(List<DocumentJob> documents) {
        return new DashboardQueryService(new InMemoryBatchRepository(List.of(batch())),
                new InMemoryDocumentJobRepository(documents), new InMemoryOcrEventRepository());
    }

    /**
     * 从详情结果中取出批次对象。
     *
     * @param detail 批次详情结果
     * @return 批次对象
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private Map<?, ?> batchOf(Map<String, Object> detail) {
        return (Map<?, ?>) detail.get("batch");
    }

    /**
     * 从详情结果中取出文档列表。
     *
     * @param detail 批次详情结果
     * @return 文档列表
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private List<?> documentsOf(Map<String, Object> detail) {
        return (List<?>) detail.get("documents");
    }

    /**
     * 提取文档 ID 列表。
     *
     * @param documents 文档列表
     * @return 文档 ID 列表
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private List<String> documentIdsOf(List<?> documents) {
        return documents.stream().map(document -> (String) ((Map<?, ?>) document).get("document_id")).toList();
    }

    /**
     * 提取处理轨道节点名称。
     *
     * @param document 文档详情
     * @return 轨道节点名称
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private List<String> labelsOf(Map<?, ?> document) {
        return trackOf(document).stream().map(node -> (String) ((Map<?, ?>) node).get("name")).toList();
    }

    /**
     * 提取处理轨道节点状态。
     *
     * @param document 文档详情
     * @return 轨道节点状态
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private List<String> statesOf(Map<?, ?> document) {
        return trackOf(document).stream().map(node -> (String) ((Map<?, ?>) node).get("state")).toList();
    }

    /**
     * 从文档详情中取出处理轨道。
     *
     * @param document 文档详情
     * @return 处理轨道
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private List<?> trackOf(Map<?, ?> document) {
        return (List<?>) document.get("track");
    }

}
