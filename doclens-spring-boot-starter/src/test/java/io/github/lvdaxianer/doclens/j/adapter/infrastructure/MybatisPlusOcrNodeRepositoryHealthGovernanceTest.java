package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeDeploymentType;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import java.time.OffsetDateTime;
import java.util.Optional;
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
 * OCR 节点健康治理字段仓储集成测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
@SpringBootTest(classes = MybatisPlusOcrNodeRepositoryHealthGovernanceTest.TestApplication.class)
class MybatisPlusOcrNodeRepositoryHealthGovernanceTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-10T09:00:00+08:00");
    private static final OffsetDateTime LAST_FAILURE_AT = OffsetDateTime.parse("2026-06-09T10:00:00+08:00");
    private static final OffsetDateTime CIRCUIT_OPEN_UNTIL = OffsetDateTime.parse("2026-06-10T10:00:00+08:00");
    private static final OffsetDateTime LAST_MANUAL_RECOVERY_AT = OffsetDateTime.parse("2026-06-10T09:30:00+08:00");

    @Autowired
    private OcrNodeRepository repository;

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
                () -> "jdbc:h2:mem:ocr_node_repo_health;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
    }

    /**
     * 仓储应持久化熔断窗口与恢复计数。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void repositoryPersistsCircuitWindowAndRecoveryCounters() {
        OcrNode node = sampleNode("node-health-1");

        repository.save(node);

        OcrNode reloaded = repository.findById(node.id()).orElseThrow();

        assertThat(reloaded.failureCount()).isEqualTo(2L);
        assertThat(reloaded.successCount()).isEqualTo(1L);
        assertThat(reloaded.lastFailureAt()).contains(LAST_FAILURE_AT);
        assertThat(reloaded.circuitOpenUntil()).contains(CIRCUIT_OPEN_UNTIL);
        assertThat(reloaded.lastManualRecoveryAt()).contains(LAST_MANUAL_RECOVERY_AT);
        assertThat(reloaded.lastError()).contains("timeout");
    }

    /**
     * 仓储更新时应允许清空熔断窗口与最近错误。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void repositoryUpdateClearsCircuitWindowAndLastError() {
        OcrNode node = sampleNode("node-health-2");
        repository.save(node);

        OcrNode recovered = node.updateHealthGovernance(0L, 3L, Optional.of(BASE_TIME.plusMinutes(10)),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(BASE_TIME.plusMinutes(10)),
                BASE_TIME.plusMinutes(10));
        repository.update(recovered);

        OcrNode reloaded = repository.findById(node.id()).orElseThrow();
        assertThat(reloaded.circuitOpenUntil()).isEmpty();
        assertThat(reloaded.lastError()).isEmpty();
        assertThat(reloaded.lastManualRecoveryAt()).contains(BASE_TIME.plusMinutes(10));
    }

    /**
     * 创建带健康治理字段的测试节点。
     *
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrNode sampleNode(String nodeId) {
        return new OcrNode(nodeId, "paddle_health", OcrNodeDeploymentType.OFFLINE, nodeId,
                "10.100.30.216", 8081, Optional.empty(), Optional.empty(), Optional.empty(), false,
                true, true, 50, 10, OcrNodeStatus.DOWN, 2L, 1L, 120L, 200L, Optional.of(BASE_TIME),
                Optional.of(BASE_TIME.minusMinutes(5)), Optional.of(LAST_FAILURE_AT), Optional.of("timeout"),
                Optional.of(CIRCUIT_OPEN_UNTIL), Optional.of(LAST_MANUAL_RECOVERY_AT), BASE_TIME, BASE_TIME);
    }

    /**
     * OCR 节点仓储测试应用。
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
    @MapperScan(basePackageClasses = OcrNodeMapper.class, annotationClass = Mapper.class)
    @Import({
            io.github.lvdaxianer.doclens.j.shared.config.MybatisPlusConfiguration.class,
            MybatisPlusOcrNodeRepository.class
    })
    static class TestApplication {
    }
}
