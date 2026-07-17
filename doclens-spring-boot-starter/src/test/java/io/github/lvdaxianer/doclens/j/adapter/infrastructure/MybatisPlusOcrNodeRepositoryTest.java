package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCreateRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeDeploymentType;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.JsonCodec;
import io.github.lvdaxianer.doclens.j.testsupport.PostgreSqlTestContainerSupport;
import java.time.OffsetDateTime;
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
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * OCR 节点 MyBatis-Plus 仓储集成测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
@SpringBootTest(classes = MybatisPlusOcrNodeRepositoryTest.TestApplication.class)
class MybatisPlusOcrNodeRepositoryTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-08T12:00:00+08:00");
    private static final PostgreSQLContainer<?> POSTGRESQL =
            PostgreSqlTestContainerSupport.createStartedContainer("ocr_node_repo");

    @Autowired
    private OcrNodeRepository repository;

    /**
     * 配置 PostgreSQL 与 Flyway 测试数据库。
     *
     * @param registry 动态属性注册表
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        PostgreSqlTestContainerSupport.registerDatasource(registry, POSTGRESQL);
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

        assertThat(repository.findById("node_1")).get().satisfies(saved -> assertPersistedNode(saved, node));
        assertThat(repository.listByModelKey("paddle_repo_save"))
                .singleElement()
                .satisfies(saved -> assertPersistedNode(saved, node));
        assertThat(repository.listEnabled()).anySatisfy(saved -> assertPersistedNode(saved, node));
    }

    /**
     * 在线节点配置应能持久化渠道、模型和凭证状态。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void saveFindsOnlineNodeSettings() {
        OcrNode node = onlineNode("node_2", "paddle_repo_online");

        repository.save(node);

        assertThat(repository.findById("node_2"))
                .get()
                .satisfies(saved -> {
                    assertThat(saved.deploymentType()).isEqualTo(OcrNodeDeploymentType.ONLINE);
                    assertThat(saved.channelKey()).contains("aliyun_bailian_dashscope");
                    assertThat(saved.providerModel()).contains("qwen-vl-ocr-2025-11-20");
                    assertThat(saved.credentialConfigured()).isTrue();
                });
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
     * 断言 PostgreSQL 持久化后的节点业务字段与时间点一致。
     *
     * @param saved 已保存节点
     * @param expected 期望节点
     * @author lvdaxianer@yeah.net
     * @date 2026-07-14
     */
    private void assertPersistedNode(OcrNode saved, OcrNode expected) {
        assertThat(saved)
                .usingRecursiveComparison()
                .ignoringFields("createdAt", "updatedAt")
                .isEqualTo(expected);
        assertThat(saved.createdAt().toInstant()).isEqualTo(expected.createdAt().toInstant());
        assertThat(saved.updatedAt().toInstant()).isEqualTo(expected.updatedAt().toInstant());
    }

    /**
     * 创建测试在线 OCR 节点。
     *
     * @param id 节点 ID
     * @param modelKey 模型标识
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrNode onlineNode(String id, String modelKey) {
        return OcrNode.create(new OcrNodeCreateRequest(id, modelKey, OcrNodeDeploymentType.ONLINE, id, "", 0,
                "aliyun_bailian_dashscope", "qwen-vl-ocr-2025-11-20", "sk-secret", true,
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

        /**
         * 为测试上下文补齐仓储依赖的 JSON 编解码器。
         *
         * @return JSON 编解码器
         * @author lvdaxianerplus
         * @date 2026-06-17
         */
        @Bean
        JsonCodec jsonCodec() {
            return new JsonCodec(new ObjectMapper());
        }
    }
}
