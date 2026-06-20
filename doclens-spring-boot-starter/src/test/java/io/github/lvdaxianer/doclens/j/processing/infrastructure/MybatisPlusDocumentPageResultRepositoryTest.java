package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResult;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResultRepository;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.JsonCodec;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 文档页 OCR 结果 MyBatis-Plus 仓储集成测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
@SpringBootTest(classes = MybatisPlusDocumentPageResultRepositoryTest.TestApplication.class)
class MybatisPlusDocumentPageResultRepositoryTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-10T10:00:00+08:00");

    @Autowired
    private DocumentPageResultRepository repository;

    /**
     * H2 内存库每个测试上下文独立，避免页结果 upsert 断言受其他测试影响。
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
                () -> "jdbc:h2:mem:document_page_result_repo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
    }

    /**
     * 页结果应按文档与页码 upsert，并能按页序读取。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void upsertPageResultKeepsSingleResultPerDocumentPage() {
        // 先写入旧版本第二页，再写入第一页，模拟页级 OCR 完成顺序早于文档页序。
        repository.upsert(result("doc-1", 2, "old page two"));
        repository.upsert(result("doc-1", 1, "page one"));
        // 重复写入同一页应覆盖旧结果，支持宕机重试后的幂等修复。
        repository.upsert(result("doc-1", 2, "page two"));

        // 文档级聚合读取时必须按 pageNo 排序，不能按完成时间排序。
        assertThat(repository.listByDocumentId("doc-1"))
                .extracting(DocumentPageResult::pageText)
                .containsExactly("page one", "page two");
        // 单页查询应返回 upsert 后的最新内容。
        assertThat(repository.findByDocumentIdAndPageNo("doc-1", 2))
                .get()
                .extracting(DocumentPageResult::pageText)
                .isEqualTo("page two");
        assertBatchLookupReturnsExistingDocumentPages();
    }

    /**
     * 按文档删除页结果后，重试应重新生成而不是复用旧结果。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Test
    void deleteByDocumentIdRemovesAllResultsForDocument() {
        repository.upsert(result("doc-2", 1, "page one"));
        repository.upsert(result("doc-2", 2, "page two"));
        repository.upsert(result("doc-3", 1, "other document"));

        repository.deleteByDocumentId("doc-2");

        assertThat(repository.listByDocumentId("doc-2")).isEmpty();
        assertThat(repository.listByDocumentId("doc-3")).extracting(DocumentPageResult::pageText)
                .containsExactly("other document");
    }

    /**
     * 批量查询应只返回已存在文档的页结果。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertBatchLookupReturnsExistingDocumentPages() {
        // 批量查询用于页任务恢复，必须一次读取多个文档的已完成页结果。
        assertThat(repository.listByDocumentIds(List.of("doc-1", "doc-missing")))
                .extracting(DocumentPageResult::pageNo)
                .containsExactly(1, 2);
    }

    /**
     * 创建测试页结果。
     *
     * @param documentId 文档 ID
     * @param pageNo 页码
     * @param pageText 页面文本
     * @return 文档页结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private DocumentPageResult result(String documentId, int pageNo, String pageText) {
        // 测试数据保留 rawOutput/layoutBlocks/warnings，覆盖 JSON 编解码链路。
        return new DocumentPageResult(documentId, pageNo, Map.of("page", pageNo), pageText,
                List.of(Map.of("text", pageText)), 0.99D, List.of(), "node-1", BASE_TIME, BASE_TIME);
    }

    /**
     * 文档页结果仓储测试应用。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
            "io.github.lvdaxianer.doclens.j.autoconfigure.DocLensAutoConfiguration",
            "io.github.lvdaxianer.doclens.j.autoconfigure.DocLensDashboardMetricsAutoConfiguration",
            "io.github.lvdaxianer.doclens.j.autoconfigure.DocLensPaddleOcrAutoConfiguration",
            "io.github.lvdaxianer.doclens.j.autoconfigure.DocLensExtractionAutoConfiguration",
            "io.github.lvdaxianer.doclens.j.autoconfigure.DocLensProcessingAutoConfiguration"
    })
    @MapperScan(basePackageClasses = DocumentPageResultMapper.class, annotationClass = Mapper.class)
    @Import({
            io.github.lvdaxianer.doclens.j.shared.config.MybatisPlusConfiguration.class,
            MybatisPlusDocumentPageResultRepository.class
    })
    static class TestApplication {

        /**
         * 创建测试 JSON 编解码器。
         *
         * @return JSON 编解码器
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        /**
         * 创建测试 JSON 编解码器。
         *
         * @param objectMapper Jackson 映射器
         * @return JSON 编解码器
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        @Bean
        JsonCodec jsonCodec(ObjectMapper objectMapper) {
            return new JsonCodec(objectMapper);
        }
    }
}
