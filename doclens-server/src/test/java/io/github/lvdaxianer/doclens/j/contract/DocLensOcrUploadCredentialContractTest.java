package io.github.lvdaxianer.doclens.j.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * DocLens OCR 上传接入方凭证契约测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
class DocLensOcrUploadCredentialContractTest extends DocLensOcrApiContractSupport {

    /** 上传文件表单字段名。 */
    private static final String FILES_PART_NAME = "files";
    /** API Key 请求头名称。 */
    private static final String API_KEY_HEADER = "X-DocLens-Api-Key";
    /** Authorization 请求头名称。 */
    private static final String AUTHORIZATION_HEADER = "Authorization";
    /** Bearer 请求头前缀。 */
    private static final String BEARER_PREFIX = "Bearer ";
    /** 测试接入方标识。 */
    private static final String CLIENT_ID = "rag-flow";
    /** 测试来源应用。 */
    private static final String SOURCE_APP = "knowledge-base";
    /** 测试租户键。 */
    private static final String TENANT_KEY = "tenant-east";
    /** 测试 API Key。 */
    private static final String API_KEY = "test-api-key";
    /** 测试 Bearer Token。 */
    private static final String BEARER_TOKEN = "test-bearer-token";

    /*
     * 该类开启 doclens.clients.credentials，专门覆盖配置凭证后的原生上传边界。
     * 未配置凭证的匿名放行契约继续由现有上传契约覆盖。
     */

    /**
     * 配置测试接入方凭证。
     *
     * @param registry 动态属性注册表
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @DynamicPropertySource
    static void credentialProperties(DynamicPropertyRegistry registry) {
        registry.add("doclens.clients.credentials[0].client-id", () -> CLIENT_ID);
        registry.add("doclens.clients.credentials[0].source-app", () -> SOURCE_APP);
        registry.add("doclens.clients.credentials[0].tenant-key", () -> TENANT_KEY);
        registry.add("doclens.clients.credentials[0].api-key", () -> API_KEY);
        registry.add("doclens.clients.credentials[0].bearer-token", () -> BEARER_TOKEN);
    }

    /**
     * 验证配置凭证后缺失凭证会拒绝上传。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void batchUploadRejectsMissingCredentialWhenCredentialsConfigured() throws Exception {
        mockMvc.perform(multipart("/api/v1/batches")
                        .file(uploadFile())
                        .param("metadata", "{\"bizId\":\"CRED-MISSING\"}")
                        .param("idempotency_key", "idem-credential-missing-" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("unauthorized caller credential"));
    }

    /**
     * 验证 API Key 凭证会写入批次调用方归因。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void batchUploadUsesApiKeyCredentialCallerIdentity() throws Exception {
        mockMvc.perform(multipart("/api/v1/batches")
                        .file(uploadFile())
                        .header(API_KEY_HEADER, API_KEY)
                        .param("metadata", "{\"bizId\":\"CRED-API-KEY\"}")
                        .param("idempotency_key", "idem-credential-api-key-" + UUID.randomUUID()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.client_id").value(CLIENT_ID))
                .andExpect(jsonPath("$.source_app").value(SOURCE_APP))
                .andExpect(jsonPath("$.tenant_key").value(TENANT_KEY));
    }

    /**
     * 验证 Bearer 凭证会写入批次调用方归因。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void batchUploadUsesBearerCredentialCallerIdentity() throws Exception {
        mockMvc.perform(multipart("/api/v1/batches")
                        .file(uploadFile())
                        .header(AUTHORIZATION_HEADER, BEARER_PREFIX + BEARER_TOKEN)
                        .param("metadata", "{\"bizId\":\"CRED-BEARER\"}")
                        .param("idempotency_key", "idem-credential-bearer-" + UUID.randomUUID()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.client_id").value(CLIENT_ID))
                .andExpect(jsonPath("$.source_app").value(SOURCE_APP))
                .andExpect(jsonPath("$.tenant_key").value(TENANT_KEY));
    }

    /**
     * 创建测试上传文件。
     *
     * @return 上传文件
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private MockMultipartFile uploadFile() {
        return new MockMultipartFile(FILES_PART_NAME, "credential.md", "text/markdown", "# Credential".getBytes());
    }
}
