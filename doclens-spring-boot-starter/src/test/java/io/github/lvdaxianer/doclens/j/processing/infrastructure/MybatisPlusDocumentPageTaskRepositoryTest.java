package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTask;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskClaimRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskCompletionRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskFailureRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskStatus;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 文档页任务 MyBatis-Plus 仓储集成测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
@SpringBootTest(classes = MybatisPlusDocumentPageTaskRepositoryTest.TestApplication.class)
class MybatisPlusDocumentPageTaskRepositoryTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-10T10:00:00+08:00");

    @Autowired
    private DocumentPageTaskRepository repository;

    /**
     * H2 内存库每个测试上下文独立，避免不同仓储测试之间共享页任务数据。
     * 这里不复用生产配置，是为了只验证页任务持久化边界。
     */

    /**
     * 配置 H2 与 Flyway 测试数据库。
     *
     * @param registry 动态属性注册表
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:document_page_task_repo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
    }

    /**
     * 页任务应按文档和页码顺序持久化，保证后续聚合顺序不乱。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void repositoryStoresPageTasksByDocumentAndPageNumber() {
        // 先反序写入，模拟 PDF 拆页后并发准备导致的页任务乱序入库。
        DocumentPageTask second = task("task-2", "doc-1", 2, "page://2");
        DocumentPageTask first = task("task-1", "doc-1", 1, "page://1");

        // 批量保存页任务，避免为每页单独触发数据库往返。
        repository.saveAll(List.of(second, first));

        // 查询时必须恢复文档内页码顺序，保证最终 OCR 文本聚合不乱。
        assertThat(repository.listByDocumentId("doc-1"))
                .extracting(DocumentPageTask::pageNo)
                .containsExactly(1, 2);
    }

    /**
     * 队列查询应只返回等待中的页任务，并遵守调用方给定的数量上限。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void listQueuedOnlyReturnsQueuedTasksWithinLimit() {
        // 同时写入等待任务和已抢占任务，覆盖调度查询的状态过滤。
        repository.saveAll(List.of(
                task("task-3", "doc-2", 1, "page://3"),
                task("task-4", "doc-2", 2, "page://4").markProcessing("worker-1", BASE_TIME.plusSeconds(30))
        ));

        // 调度侧只能看到 QUEUED 任务，避免重复派发正在处理的页面。
        assertThat(repository.listQueued(1))
                .extracting(DocumentPageTask::taskId)
                .containsExactly("task-3");
    }

    /**
     * 原子抢占只能让第一个工作线程拿到任务，后续线程必须失败。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void tryMarkProcessingOnlyClaimsQueuedTaskOnce() {
        // 保存一个等待任务，模拟多个 OCR worker 同时从共享队列看到同一页。
        repository.saveAll(List.of(task("task-5", "doc-3", 1, "page://5")));

        // 第一次抢占成功，第二次基于数据库状态条件更新必须失败。
        boolean firstClaimed = repository.tryMarkProcessing(claimRequest("task-5", "worker-a"));
        boolean secondClaimed = repository.tryMarkProcessing(claimRequest("task-5", "worker-b"));

        // 最终任务只属于第一个 worker，避免同一页被重复 OCR。
        assertThat(firstClaimed).isTrue();
        assertThat(secondClaimed).isFalse();
        assertThat(repository.findByDocumentIdAndPageNo("doc-3", 1)).get()
                .extracting(DocumentPageTask::status, task -> task.lockedBy().orElse(""))
                .containsExactly(DocumentPageTaskStatus.PROCESSING, "worker-a");
    }

    /**
     * 非等待状态页任务不能再次被抢占。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void tryMarkProcessingRejectsAlreadyProcessingTask() {
        // 已处理中的任务代表其他 worker 已持有锁，调度器不能覆盖锁信息。
        DocumentPageTask processing = task("task-6", "doc-4", 1, "page://6")
                .markProcessing("worker-a", BASE_TIME.plusSeconds(30), BASE_TIME);
        repository.saveAll(List.of(processing));

        // 对非 QUEUED 状态执行原子抢占，预期数据库条件更新不到任何行。
        boolean claimed = repository.tryMarkProcessing(claimRequest("task-6", "worker-b"));

        // 锁归属保持不变，说明并发抢占语义由数据库兜住。
        assertThat(claimed).isFalse();
        assertThat(repository.findByDocumentIdAndPageNo("doc-4", 1)).get()
                .extracting(task -> task.lockedBy().orElse(""))
                .isEqualTo("worker-a");
    }

    /**
     * 页任务完成状态必须实时落库，供宕机恢复和文档聚合读取。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void markCompletedPersistsTerminalSuccessState() {
        // 先抢占再完成，模拟 OCR 成功后的强一致状态写入。
        repository.saveAll(List.of(task("task-7", "doc-5", 1, "page://7")));
        repository.tryMarkProcessing(claimRequest("task-7", "worker-a"));

        // 完成时记录处理节点和完成时间，供后续聚合与排障使用。
        repository.markCompleted(completeRequest("task-7", "worker-a"));

        // 读取数据库状态，确认终态不会停留在 PROCESSING。
        assertThat(repository.findByDocumentIdAndPageNo("doc-5", 1)).get()
                .extracting(DocumentPageTask::status, task -> task.completedAt().orElseThrow())
                .containsExactly(DocumentPageTaskStatus.COMPLETED, BASE_TIME.plusSeconds(10));
    }

    /**
     * 非持锁 worker 不能把处理中的页任务标记完成。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void markCompletedRejectsWorkerThatDoesNotOwnTask() {
        // 抢占归属是页任务并发安全边界，完成状态也必须校验 worker 身份。
        repository.saveAll(List.of(task("task-9", "doc-7", 1, "page://9")));
        repository.tryMarkProcessing(claimRequest("task-9", "worker-a"));

        // 如果非持锁 worker 能完成任务，就可能覆盖真实 worker 的执行结果。
        assertThatThrownBy(() -> repository.markCompleted(completeRequest("task-9", "worker-b")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("task-9");
    }

    /**
     * 页任务失败状态必须实时落库，避免失败页在重启后丢失原因。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void markFailedPersistsTerminalFailureState() {
        // 先抢占再失败，模拟 OCR 三次重试后进入终态失败。
        repository.saveAll(List.of(task("task-8", "doc-6", 1, "page://8")));
        repository.tryMarkProcessing(claimRequest("task-8", "worker-a"));

        // 失败原因写入页任务，后续列表和排障都依赖这份持久化信息。
        repository.markFailed(failureRequest("task-8", "OCR_TIMEOUT"));

        // 验证失败状态和错误码已经落库，不依赖内存队列保留上下文。
        assertThat(repository.findByDocumentIdAndPageNo("doc-6", 1)).get()
                .extracting(DocumentPageTask::status, task -> task.errorCode().orElse(""))
                .containsExactly(DocumentPageTaskStatus.FAILED, "OCR_TIMEOUT");
    }

    /**
     * 创建测试页任务。
     *
     * @param taskId 任务 ID
     * @param documentId 文档 ID
     * @param pageNo 页码
     * @param imageUri 页面图片地址
     * @return 文档页任务
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private DocumentPageTask task(String taskId, String documentId, int pageNo, String imageUri) {
        return DocumentPageTask.create(
                new DocumentPageTaskCreateRequest(taskId, "batch-1", documentId, pageNo, imageUri, BASE_TIME));
    }

    /**
     * 创建测试抢占请求。
     *
     * @param taskId 页任务 ID
     * @param workerId 工作线程 ID
     * @return 页任务抢占请求
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private DocumentPageTaskClaimRequest claimRequest(String taskId, String workerId) {
        return new DocumentPageTaskClaimRequest(taskId, workerId, BASE_TIME.plusSeconds(30), BASE_TIME);
    }

    /**
     * 创建测试完成请求。
     *
     * @param taskId 页任务 ID
     * @param workerId 工作线程 ID
     * @return 页任务完成请求
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private DocumentPageTaskCompletionRequest completeRequest(String taskId, String workerId) {
        return new DocumentPageTaskCompletionRequest(taskId, workerId, BASE_TIME.plusSeconds(10));
    }

    /**
     * 创建测试失败请求。
     *
     * @param taskId 页任务 ID
     * @param errorCode 错误码
     * @return 页任务失败请求
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private DocumentPageTaskFailureRequest failureRequest(String taskId, String errorCode) {
        return new DocumentPageTaskFailureRequest(taskId, errorCode, "timeout", BASE_TIME.plusSeconds(10));
    }

    /**
     * 文档页任务仓储测试应用。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
            "io.github.lvdaxianer.doclens.j.autoconfigure.DocLensAutoConfiguration",
            "io.github.lvdaxianer.doclens.j.autoconfigure.DocLensPaddleOcrAutoConfiguration",
            "io.github.lvdaxianer.doclens.j.autoconfigure.DocLensExtractionAutoConfiguration",
            "io.github.lvdaxianer.doclens.j.autoconfigure.DocLensProcessingAutoConfiguration"
    })
    @MapperScan(basePackageClasses = DocumentPageTaskMapper.class, annotationClass = Mapper.class)
    @Import({
            io.github.lvdaxianer.doclens.j.shared.config.MybatisPlusConfiguration.class,
            MybatisPlusDocumentPageTaskRepository.class
    })
    static class TestApplication {

        /**
         * 测试应用只装配页任务 Mapper 和仓储，避免启动完整 OCR 处理链路。
         */
    }
}
