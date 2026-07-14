package io.github.lvdaxianer.doclens.j.contract;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrHealthGovernance;
import io.github.lvdaxianer.doclens.j.testsupport.PostgreSqlTestContainerSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * OCR 全局治理配置 API 契约测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
@SpringBootTest
@AutoConfigureMockMvc
class OcrGovernanceConfigApiContractTest implements CallerCredentialContractSupport {

    /** PostgreSQL 契约测试数据库。 */
    private static final PostgreSQLContainer<?> POSTGRESQL =
            PostgreSqlTestContainerSupport.createStartedContainer("ocr_governance_config");

    @TempDir
    static java.nio.file.Path tempDir;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 配置隔离的测试数据库与文件存储。
     *
     * @param registry 动态属性注册表
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        PostgreSqlTestContainerSupport.registerDatasource(registry, POSTGRESQL);
        registry.add("doclens.storage-root", () -> tempDir.resolve("storage").toString());
        registry.add("doclens.paddle-ocr.enabled", () -> "false");
        registry.add("doclens.clients.credentials[0].client-id", () -> TEST_CLIENT_ID);
        registry.add("doclens.clients.credentials[0].source-app", () -> TEST_SOURCE_APP);
        registry.add("doclens.clients.credentials[0].tenant-key", () -> TEST_TENANT_KEY);
        registry.add("doclens.clients.credentials[0].api-key", () -> TEST_API_KEY);
    }

    /**
     * 清理治理配置单例表，保证每个契约用例从默认值开始。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @BeforeEach
    void cleanGovernanceConfig() {
        jdbcTemplate.update("DELETE FROM doclens_ocr_governance_config");
    }

    /**
     * 初始状态应返回当前系统生效的 OCR 全局治理默认值。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void getConfigReturnsCurrentDefaultGovernanceValues() throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/ocr-governance-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failure_threshold").value(OcrHealthGovernance.DEFAULT_FAILURE_THRESHOLD))
                .andExpect(jsonPath("$.probe_interval_seconds")
                        .value(OcrHealthGovernance.DEFAULT_PROBE_INTERVAL_SECONDS))
                .andExpect(jsonPath("$.circuit_open_seconds").value(OcrHealthGovernance.DEFAULT_CIRCUIT_OPEN_SECONDS))
                .andExpect(jsonPath("$.recovery_success_threshold")
                        .value(OcrHealthGovernance.DEFAULT_RECOVERY_SUCCESS_THRESHOLD))
                .andExpect(jsonPath("$.manual_recovery_attempts")
                        .value(OcrHealthGovernance.DEFAULT_MANUAL_RECOVERY_ATTEMPTS));
    }

    /**
     * 保存治理配置后应返回更新值，并可被后续查询读取。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void updateConfigPersistsNewGovernanceValues() throws Exception {
        mockMvc.perform(authenticatedPut("/api/v1/ocr-governance-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "failure_threshold": 5,
                                  "probe_interval_seconds": 30,
                                  "circuit_open_seconds": 120,
                                  "recovery_success_threshold": 2,
                                  "manual_recovery_attempts": 4
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failure_threshold").value(5))
                .andExpect(jsonPath("$.probe_interval_seconds").value(30))
                .andExpect(jsonPath("$.circuit_open_seconds").value(120))
                .andExpect(jsonPath("$.recovery_success_threshold").value(2))
                .andExpect(jsonPath("$.manual_recovery_attempts").value(4));

        mockMvc.perform(authenticatedGet("/api/v1/ocr-governance-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failure_threshold").value(5))
                .andExpect(jsonPath("$.probe_interval_seconds").value(30))
                .andExpect(jsonPath("$.circuit_open_seconds").value(120))
                .andExpect(jsonPath("$.recovery_success_threshold").value(2))
                .andExpect(jsonPath("$.manual_recovery_attempts").value(4));
    }

}
