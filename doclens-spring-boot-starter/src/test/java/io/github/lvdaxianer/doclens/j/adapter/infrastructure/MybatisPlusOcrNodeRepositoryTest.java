package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCreateRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * OCR 节点 MyBatis-Plus 仓储集成测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
@SpringBootTest(classes = MybatisPlusOcrNodeRepositoryTest.TestApplication.class)
class MybatisPlusOcrNodeRepositoryTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-08T12:00:00+08:00");

    @Autowired
    private OcrNodeRepository repository;

    /**
     * 配置 H2 与 Flyway 测试数据库。
     *
     * @param registry 动态属性注册表
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:ocr_node_repo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
    }

    /**
     * 保存节点后应能按 ID 与模型查询。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void saveFindsNodeByIdAndModelKey() {
        OcrNode node = node("node_1", "paddle_repo_save", "10.100.30.215", 8080);

        repository.save(node);

        assertThat(repository.findById("node_1")).contains(node);
        assertThat(repository.listByModelKey("paddle_repo_save")).containsExactly(node);
        assertThat(repository.listEnabled()).contains(node);
    }

    /**
     * 同一模型下重复 host 和 port 应由数据库唯一约束拒绝。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void duplicateModelHostPortIsRejected() {
        repository.save(node("node_2", "paddle_repo_dup", "10.100.30.216", 8080));

        assertThatThrownBy(() -> repository.save(node("node_3", "paddle_repo_dup", "10.100.30.216", 8080)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * 创建测试 OCR 节点。
     *
     * @param id 节点 ID
     * @param modelKey 模型标识
     * @param host 节点主机
     * @param port 节点端口
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private OcrNode node(String id, String modelKey, String host, int port) {
        return OcrNode.create(new OcrNodeCreateRequest(id, modelKey, id, host, port,
                true, true, 100, 4, BASE_TIME));
    }

    /**
     * OCR 节点仓储测试应用。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
            "io.github.lvdaxianer.doclens.j.autoconfigure.DocLensAutoConfiguration",
            "io.github.lvdaxianer.doclens.j.autoconfigure.DocLensPaddleOcrAutoConfiguration",
            "io.github.lvdaxianer.doclens.j.autoconfigure.DocLensExtractionAutoConfiguration",
            "io.github.lvdaxianer.doclens.j.autoconfigure.DocLensProcessingAutoConfiguration"
    })
    @MapperScan(basePackageClasses = OcrNodeMapper.class, annotationClass = Mapper.class)
    @Import({
            io.github.lvdaxianer.doclens.j.shared.config.MybatisPlusConfiguration.class,
            MybatisPlusOcrNodeRepository.class
    })
    static class TestApplication {
    }
}
