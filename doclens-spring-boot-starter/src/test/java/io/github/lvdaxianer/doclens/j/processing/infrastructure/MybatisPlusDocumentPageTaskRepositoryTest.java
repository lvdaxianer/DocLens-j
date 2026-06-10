package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTask;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskRepository;
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
