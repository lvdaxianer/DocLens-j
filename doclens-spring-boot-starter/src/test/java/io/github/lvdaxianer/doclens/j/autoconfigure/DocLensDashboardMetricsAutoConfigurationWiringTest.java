package io.github.lvdaxianer.doclens.j.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryService;
import io.github.lvdaxianer.doclens.j.testsupport.PostgreSqlTestContainerSupport;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Dashboard OCR 指标自动配置装配测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
@SpringBootTest(classes = DocLensDashboardMetricsAutoConfigurationWiringTest.TestApplication.class)
class DocLensDashboardMetricsAutoConfigurationWiringTest {

    private static final String NODE_ID = "ocr_node_dashboard_wiring";
    private static final String NODE_NAME = "Dashboard wiring OCR";
    private static final String MODEL_KEY = "paddle_ocr";
    private static final String NODE_HOST = "127.0.0.1";
    private static final int NODE_PORT = 18081;
    private static final int NODE_WEIGHT = 100;
    private static final int NODE_MAX_CONCURRENCY = 10;
    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-21T09:30:00+08:00");
    private static final PostgreSQLContainer<?> POSTGRESQL =
            PostgreSqlTestContainerSupport.createStartedContainer("dashboard_metrics_wiring");

    @TempDir
    static java.nio.file.Path tempDir;

    @Autowired
    private DashboardQueryService dashboardQueryService;
    @Autowired
    private OcrNodeRepository nodeRepository;

    /**
     * 配置隔离的 Starter 测试运行环境。
     *
     * @param registry 动态属性注册表
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        PostgreSqlTestContainerSupport.registerDatasource(registry, POSTGRESQL);
        registry.add("doclens.storage-root", () -> tempDir.resolve("storage").toString());
        registry.add("doclens.paddle-ocr.enabled", () -> "false");
    }

    /**
     * Dashboard 查询服务应使用真实 OCR 指标提供器，而不是空 fallback。
     *
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Test
    void dashboardQueryServiceUsesRealOcrMetricsProviderWhenOcrInfrastructureExists() {
        nodeRepository.save(enabledNode());

        Map<String, Object> summary = dashboardQueryService.summary();

        assertThat(summary.get("ocr_resources")).asInstanceOf(InstanceOfAssertFactories.MAP)
                .extractingByKey("nodes")
                .asInstanceOf(InstanceOfAssertFactories.LIST)
                .anySatisfy(row -> assertThat(row).asInstanceOf(InstanceOfAssertFactories.MAP)
                        .containsEntry("node_id", NODE_ID)
                        .containsEntry("node_name", NODE_NAME));
    }

    /**
     * 创建启用 OCR 节点。
     *
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private OcrNode enabledNode() {
        return new OcrNode(NODE_ID, MODEL_KEY, NODE_NAME, NODE_HOST, NODE_PORT, true, true, NODE_WEIGHT,
                NODE_MAX_CONCURRENCY, OcrNodeStatus.UP, 0L, 0L, 0L, 0L, Optional.of(BASE_TIME),
                Optional.empty(), Optional.empty(), Optional.empty(), BASE_TIME, BASE_TIME);
    }

    /**
     * 用于激活 Spring Boot 自动配置的最小宿主应用。
     *
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @SpringBootApplication
    static class TestApplication {
    }
}
