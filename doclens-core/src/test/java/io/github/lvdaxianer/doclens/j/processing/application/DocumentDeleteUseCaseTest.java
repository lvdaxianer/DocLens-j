package io.github.lvdaxianer.doclens.j.processing.application;

import static io.github.lvdaxianer.doclens.j.processing.application.DocumentDeleteUseCaseTestSupport.completedBatch;
import static io.github.lvdaxianer.doclens.j.processing.application.DocumentDeleteUseCaseTestSupport.document;
import static io.github.lvdaxianer.doclens.j.processing.application.DocumentDeleteUseCaseTestSupport.documentEvent;
import static io.github.lvdaxianer.doclens.j.processing.application.DocumentDeleteUseCaseTestSupport.documentUseCase;
import static io.github.lvdaxianer.doclens.j.processing.application.DocumentDeleteUseCaseTestSupport.failedBatch;
import static io.github.lvdaxianer.doclens.j.processing.application.DocumentDeleteUseCaseTestSupport.processingBatch;
import static io.github.lvdaxianer.doclens.j.processing.application.DocumentDeleteUseCaseTestSupport.result;
import static io.github.lvdaxianer.doclens.j.processing.application.DocumentDeleteUseCaseTestSupport.stalledDocument;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentDeleteUseCaseInfrastructure.RecordingCheckpointStore;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentDeleteUseCaseInfrastructure.RecordingObjectStorage;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 文档删除用例测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
class DocumentDeleteUseCaseTest {

    /*
     * 这组测试只覆盖单文档删除入口。
     * 批次级删除已经移动到 BatchDeleteUseCaseTest，
     * 避免一个测试类同时承担两种应用服务职责。
     *
     * 每个场景都使用独立的内存仓储集合，
     * 防止测试之间共享状态导致顺序依赖。
     * 对象存储使用 RecordingObjectStorage，
     * 只记录删除 URI，不触碰真实文件系统。
     *
     * 断言重点保持在删除副作用：
     * 文档是否移除、OCR 结果是否移除、
     * 事件是否保留必要审计、批次摘要是否刷新、
     * 原始文件和 Markdown 结果是否被请求删除。
     */

    /**
     * 失败文档删除后应清理文档、结果、事件和关联存储，并刷新批次摘要。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void deleteFailedDocumentCleansDependenciesAndRefreshesBatchSummary() {
        DeleteDocumentScenario scenario = failedAndCompletedDocumentScenario();

        // 删除失败文档后，依赖资源应清理，批次仍保留剩余完成文档。
        scenario.useCase().delete("doc-failed");

        assertFailedDocumentCleanup(scenario);
    }

    /**
     * 删除文档时应同步清理该文档的 Markdown chunk checkpoint 目录。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void deleteDocumentCleansMarkdownChunkCheckpoint() {
        DeleteDocumentScenario scenario = lastDocumentScenario();

        // checkpoint 目录属于显式删除文档的一部分，避免恢复链路复用残留 chunk。
        scenario.useCase().delete("doc-last");

        assertThat(scenario.checkpointStore().deletedDocumentIds).containsExactly("doc-last");
    }

    /**
     * 已完成和卡死文档也应允许删除，便于用户清理历史结果或恢复失败残留。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void deleteCompletedAndStalledDocumentsIsAllowed() {
        DeleteDocumentScenario scenario = completedAndStalledDocumentScenario();

        // 连续删除两个可删除文档时，应同时覆盖有结果和无结果两种资源路径。
        scenario.useCase().delete("doc-completed");
        scenario.useCase().delete("doc-stalled");

        assertThat(scenario.repositories().documentRepository.listByBatchId("batch-test")).isEmpty();
        assertThat(scenario.repositories().resultRepository.findByDocumentId("doc-completed")).isEmpty();
        assertThat(scenario.objectStorage().deletedUris)
                .contains("local://results/doc-completed.md", "local://doc-completed", "local://doc-stalled");
    }

    /**
     * 删除批次内最后一个文档后，不应保留空批次残留。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void deleteLastDocumentRemovesEmptyBatch() {
        DeleteDocumentScenario scenario = lastDocumentScenario();

        // 删除最后一个文档时，不再刷新摘要，而是移除批次本身。
        scenario.useCase().delete("doc-last");

        assertThat(scenario.repositories().documentRepository.findById("doc-last")).isEmpty();
        assertThat(scenario.repositories().batchRepository.findById("batch-test")).isEmpty();
    }

    /**
     * 处理中删除在第一版应明确拒绝，避免破坏运行中的处理链路。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void deleteProcessingDocumentIsRejected() {
        DeleteDocumentScenario scenario = processingDocumentScenario();

        // 拒绝删除时不能产生部分清理副作用。
        assertThatThrownBy(() -> scenario.useCase().delete("doc-processing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("processing");
        assertThat(scenario.repositories().documentRepository.findById("doc-processing")).isPresent();
        assertThat(scenario.objectStorage().deletedUris).isEmpty();
    }

    /**
     * 创建失败文档和完成文档并存的删除场景。
     *
     * @return 删除测试场景
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DeleteDocumentScenario failedAndCompletedDocumentScenario() {
        DocumentDeleteUseCaseRepositories repositories = new DocumentDeleteUseCaseRepositories();
        RecordingObjectStorage objectStorage = new RecordingObjectStorage();
        OffsetDateTime now = OffsetDateTime.now();
        repositories.documentRepository.saveAll(List.of(failedDocument(now), completedDocument(now, "doc-completed", 1)));
        repositories.batchRepository.save(failedBatch(2, 1, 1));
        repositories.resultRepository.save(result("doc-failed", "local://results/doc-failed.md"));
        repositories.eventRepository.saveAll(List.of(documentEvent("doc-failed", DocLensConstants.EVENT_DOCUMENT_FAILED),
                documentEvent("doc-completed", DocLensConstants.EVENT_DOCUMENT_COMPLETED)));
        return scenario(repositories, objectStorage);
    }

    /**
     * 创建完成文档和卡死文档并存的删除场景。
     *
     * @return 删除测试场景
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DeleteDocumentScenario completedAndStalledDocumentScenario() {
        DocumentDeleteUseCaseRepositories repositories = new DocumentDeleteUseCaseRepositories();
        RecordingObjectStorage objectStorage = new RecordingObjectStorage();
        OffsetDateTime now = OffsetDateTime.now();
        repositories.documentRepository.saveAll(List.of(completedDocument(now, "doc-completed", 0),
                stalledDocument("doc-stalled", now, 1)));
        repositories.batchRepository.save(processingBatch(2, 1, 0));
        repositories.resultRepository.save(result("doc-completed", "local://results/doc-completed.md"));
        return scenario(repositories, objectStorage);
    }

    /**
     * 创建仅包含最后一个文档的删除场景。
     *
     * @return 删除测试场景
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DeleteDocumentScenario lastDocumentScenario() {
        DocumentDeleteUseCaseRepositories repositories = new DocumentDeleteUseCaseRepositories();
        RecordingObjectStorage objectStorage = new RecordingObjectStorage();
        OffsetDateTime now = OffsetDateTime.now();
        repositories.documentRepository.save(completedDocument(now, "doc-last", 0));
        repositories.batchRepository.save(completedBatch(1, 1, 0));
        repositories.resultRepository.save(result("doc-last", "local://results/doc-last.md"));
        return scenario(repositories, objectStorage);
    }

    /**
     * 创建处理中删除拒绝场景。
     *
     * @return 删除测试场景
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DeleteDocumentScenario processingDocumentScenario() {
        DocumentDeleteUseCaseRepositories repositories = new DocumentDeleteUseCaseRepositories();
        RecordingObjectStorage objectStorage = new RecordingObjectStorage();
        OffsetDateTime now = OffsetDateTime.now();
        repositories.documentRepository.save(document("doc-processing", 0).startProcessing(now));
        repositories.batchRepository.save(processingBatch(1, 0, 0));
        return scenario(repositories, objectStorage);
    }

    /**
     * 创建失败文档。
     *
     * @param now 测试时间
     * @return 失败文档
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentJob failedDocument(OffsetDateTime now) {
        return document("doc-failed", 0)
                .startProcessing(now)
                .fail(DocLensConstants.ERROR_CODE_OCR_FAILED, "ocr failed", now.plusSeconds(1));
    }

    /**
     * 创建已完成文档。
     *
     * @param now 测试时间
     * @param documentId 文档 ID
     * @param sortOrder 排序
     * @return 已完成文档
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentJob completedDocument(OffsetDateTime now, String documentId, int sortOrder) {
        return document(documentId, sortOrder)
                .startProcessing(now)
                .complete("result-" + documentId, now.plusSeconds(1));
    }

    /**
     * 断言失败文档删除后的副作用。
     *
     * @param scenario 删除测试场景
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertFailedDocumentCleanup(DeleteDocumentScenario scenario) {
        assertThat(scenario.repositories().documentRepository.findById("doc-failed")).isEmpty();
        assertThat(scenario.repositories().resultRepository.findByDocumentId("doc-failed")).isEmpty();
        assertThat(scenario.repositories().eventRepository.listByBatchId("batch-test"))
                .extracting(OcrEvent::documentId)
                .containsExactly(Optional.of("doc-completed"), Optional.of("doc-failed"));
        assertThat(scenario.objectStorage().deletedUris).containsExactly("local://doc-failed", "local://results/doc-failed.md");
        assertThat(scenario.repositories().batchRepository.findById("batch-test")).get()
                .satisfies(this::assertSingleCompletedBatch);
    }

    /**
     * 断言批次已刷新为单个完成文档。
     *
     * @param batch 批次
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertSingleCompletedBatch(io.github.lvdaxianer.doclens.j.ingestion.domain.Batch batch) {
        assertThat(batch.completedFiles()).isEqualTo(1);
        assertThat(batch.failedFiles()).isZero();
        assertThat(batch.status()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(batch.totalFiles()).isEqualTo(1);
    }

    /**
     * 创建删除测试场景。
     *
     * @param repositories 测试仓储集合
     * @param objectStorage 对象存储桩
     * @return 删除测试场景
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DeleteDocumentScenario scenario(
            DocumentDeleteUseCaseRepositories repositories,
            RecordingObjectStorage objectStorage
    ) {
        RecordingCheckpointStore checkpointStore = new RecordingCheckpointStore();
        return new DeleteDocumentScenario(repositories, objectStorage, checkpointStore,
                documentUseCase(repositories, objectStorage, checkpointStore));
    }

    /**
     * 单文档删除测试场景。
     *
     * @param repositories 测试仓储集合
     * @param objectStorage 对象存储桩
     * @param checkpointStore checkpoint 存储桩
     * @param useCase 文档删除用例
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private record DeleteDocumentScenario(
            DocumentDeleteUseCaseRepositories repositories,
            RecordingObjectStorage objectStorage,
            RecordingCheckpointStore checkpointStore,
            DocumentDeleteUseCase useCase
    ) {
    }
}
