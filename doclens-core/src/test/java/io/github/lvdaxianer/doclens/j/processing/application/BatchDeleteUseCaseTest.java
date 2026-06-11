package io.github.lvdaxianer.doclens.j.processing.application;

import static io.github.lvdaxianer.doclens.j.processing.application.DocumentDeleteUseCaseTestSupport.batchUseCase;
import static io.github.lvdaxianer.doclens.j.processing.application.DocumentDeleteUseCaseTestSupport.document;
import static io.github.lvdaxianer.doclens.j.processing.application.DocumentDeleteUseCaseTestSupport.failedBatch;
import static io.github.lvdaxianer.doclens.j.processing.application.DocumentDeleteUseCaseTestSupport.processingBatch;
import static io.github.lvdaxianer.doclens.j.processing.application.DocumentDeleteUseCaseTestSupport.result;
import static io.github.lvdaxianer.doclens.j.processing.application.DocumentDeleteUseCaseTestSupport.stalledDocument;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lvdaxianer.doclens.j.processing.application.DocumentDeleteUseCaseInfrastructure.RecordingObjectStorage;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 批次删除用例测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
class BatchDeleteUseCaseTest {

    /*
     * 这组测试只覆盖批次删除入口。
     * 它和单文档删除共享同一批测试桩，
     * 但断言重点放在整批原子性：
     * 要么所有文档都可删除并完成清理，
     * 要么发现任一不可删除文档就整体拒绝。
     *
     * 这种拆分让批次级规则更容易被阅读和维护，
     * 也避免 DocumentDeleteUseCaseTest 再次膨胀。
     */

    /**
     * 删除批次时应清理批次内所有可删除文档，并最终移除空批次。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void deleteBatchRemovesAllDeletableDocumentsAndEmptyBatch() {
        BatchDeleteScenario scenario = deletableBatchScenario();

        // 整批删除成功后，批次、文档、结果和对象存储引用都应完成清理。
        int deletedCount = scenario.useCase().delete("batch-test");

        assertThat(deletedCount).isEqualTo(2);
        assertDeletableBatchCleanup(scenario);
    }

    /**
     * 批次包含处理中或排队中文档时应整批拒绝，避免出现半删除状态。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void deleteBatchRejectsNonDeletableDocumentsWithoutPartialDelete() {
        BatchDeleteScenario scenario = nonDeletableBatchScenario();

        // 拒绝后保留所有文档和结果，证明没有发生半删除。
        assertThatThrownBy(() -> scenario.useCase().delete("batch-test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-deletable")
                .hasMessageContaining("processing");
        assertNonDeletableBatchUnchanged(scenario);
    }

    /**
     * 创建可删除批次场景。
     *
     * @return 批次删除测试场景
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private BatchDeleteScenario deletableBatchScenario() {
        DocumentDeleteUseCaseRepositories repositories = new DocumentDeleteUseCaseRepositories();
        RecordingObjectStorage objectStorage = new RecordingObjectStorage();
        OffsetDateTime now = OffsetDateTime.now();
        repositories.documentRepository.saveAll(List.of(completedDocument(now), stalledDocument("doc-stalled", now, 1)));
        repositories.batchRepository.save(failedBatch(2, 1, 0));
        repositories.resultRepository.save(result("doc-completed", "local://results/doc-completed.md"));
        return scenario(repositories, objectStorage);
    }

    /**
     * 创建包含不可删除文档的批次场景。
     *
     * @return 批次删除测试场景
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private BatchDeleteScenario nonDeletableBatchScenario() {
        DocumentDeleteUseCaseRepositories repositories = new DocumentDeleteUseCaseRepositories();
        RecordingObjectStorage objectStorage = new RecordingObjectStorage();
        OffsetDateTime now = OffsetDateTime.now();
        repositories.documentRepository.saveAll(List.of(completedDocument(now), processingDocument(now)));
        repositories.batchRepository.save(processingBatch(2, 1, 0));
        repositories.resultRepository.save(result("doc-completed", "local://results/doc-completed.md"));
        return scenario(repositories, objectStorage);
    }

    /**
     * 创建已完成文档。
     *
     * @param now 测试时间
     * @return 已完成文档
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentJob completedDocument(OffsetDateTime now) {
        return document("doc-completed", 0)
                .startProcessing(now)
                .complete("result-doc-completed", now.plusSeconds(1));
    }

    /**
     * 创建处理中文档。
     *
     * @param now 测试时间
     * @return 处理中文档
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentJob processingDocument(OffsetDateTime now) {
        return document("doc-processing", 1).startProcessing(now);
    }

    /**
     * 断言可删除批次已完成清理。
     *
     * @param scenario 批次删除测试场景
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertDeletableBatchCleanup(BatchDeleteScenario scenario) {
        assertThat(scenario.repositories().documentRepository.listByBatchId("batch-test")).isEmpty();
        assertThat(scenario.repositories().resultRepository.findByDocumentId("doc-completed")).isEmpty();
        assertThat(scenario.repositories().eventRepository.listByBatchId("batch-test")).hasSize(2);
        assertThat(scenario.objectStorage().deletedUris)
                .contains("local://doc-completed", "local://results/doc-completed.md", "local://doc-stalled");
        assertThat(scenario.repositories().batchRepository.findById("batch-test")).isEmpty();
    }

    /**
     * 断言不可删除批次保持原状。
     *
     * @param scenario 批次删除测试场景
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertNonDeletableBatchUnchanged(BatchDeleteScenario scenario) {
        assertThat(scenario.repositories().documentRepository.findById("doc-completed")).isPresent();
        assertThat(scenario.repositories().documentRepository.findById("doc-processing")).isPresent();
        assertThat(scenario.repositories().resultRepository.findByDocumentId("doc-completed")).isPresent();
        assertThat(scenario.objectStorage().deletedUris).isEmpty();
    }

    /**
     * 创建批次删除测试场景。
     *
     * @param repositories 测试仓储集合
     * @param objectStorage 对象存储桩
     * @return 批次删除测试场景
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private BatchDeleteScenario scenario(
            DocumentDeleteUseCaseRepositories repositories,
            RecordingObjectStorage objectStorage
    ) {
        return new BatchDeleteScenario(repositories, objectStorage, batchUseCase(repositories, objectStorage));
    }

    /**
     * 批次删除测试场景。
     *
     * @param repositories 测试仓储集合
     * @param objectStorage 对象存储桩
     * @param useCase 批次删除用例
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private record BatchDeleteScenario(
            DocumentDeleteUseCaseRepositories repositories,
            RecordingObjectStorage objectStorage,
            BatchDeleteUseCase useCase
    ) {
    }
}
