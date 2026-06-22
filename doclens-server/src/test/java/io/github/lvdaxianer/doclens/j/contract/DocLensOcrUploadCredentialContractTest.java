package io.github.lvdaxianer.doclens.j.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;

/**
 * DocLens OCR 上传 caller 分区键契约测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
class DocLensOcrUploadCredentialContractTest extends DocLensOcrApiContractSupport {

    /** 上传文件表单字段名。 */
    private static final String FILES_PART_NAME = "files";
    /** 批次创建接口路径。 */
    private static final String BATCHES_PATH = "/api/v1/batches";
    /** 测试 caller 分区键。 */
    private static final String PARTITION_KEY = "tenant-east";
    /** 另一个合法 caller 分区键。 */
    private static final String OTHER_PARTITION_KEY = "tenant-west";
    /** 默认来源应用。 */
    private static final String DEFAULT_SOURCE_APP = "dashboard";
    /** 缺失分区键错误消息。 */
    private static final String MISSING_CALLER_PARTITION_KEY_MESSAGE = "missing caller partition key";
    /** 历史配置属性前缀。 */
    private static final String LEGACY_CREDENTIAL_PREFIX = "doclens.clients.credentials[0].";
    /** 历史配置 client id 示例值。 */
    private static final String LEGACY_CLIENT_ID = "legacy-client";
    /** 历史配置 source app 示例值。 */
    private static final String LEGACY_SOURCE_APP = "legacy-source";
    /** 历史配置 tenant key 示例值。 */
    private static final String LEGACY_TENANT_KEY = "legacy-tenant";
    /** 历史配置 api key 示例值。 */
    private static final String LEGACY_API_KEY = "legacy-api-key";
    /** 历史配置 bearer token 示例值。 */
    private static final String LEGACY_BEARER_TOKEN = "legacy-bearer-token";
    /** 测试文件名。 */
    private static final String UPLOAD_FILE_NAME = "credential.md";
    /** 测试文件内容类型。 */
    private static final String UPLOAD_CONTENT_TYPE = "text/markdown";
    /** 测试文件内容。 */
    private static final String UPLOAD_CONTENT = "# Credential";

    /**
     * 配置历史接入方凭证，验证服务端不会把它作为 allowlist。
     *
     * @param registry 动态属性注册表
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @DynamicPropertySource
    static void credentialProperties(DynamicPropertyRegistry registry) {
        registry.add(LEGACY_CREDENTIAL_PREFIX + "client-id", () -> LEGACY_CLIENT_ID);
        registry.add(LEGACY_CREDENTIAL_PREFIX + "source-app", () -> LEGACY_SOURCE_APP);
        registry.add(LEGACY_CREDENTIAL_PREFIX + "tenant-key", () -> LEGACY_TENANT_KEY);
        registry.add(LEGACY_CREDENTIAL_PREFIX + "api-key", () -> LEGACY_API_KEY);
        registry.add(LEGACY_CREDENTIAL_PREFIX + "bearer-token", () -> LEGACY_BEARER_TOKEN);
    }

    /**
     * 验证缺失 caller 分区键会拒绝上传。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void batchUploadRejectsMissingCallerPartitionKey() throws Exception {
        MvcResult result = mockMvc.perform(multipart(BATCHES_PATH)
                        .file(uploadFile())
                        .param("metadata", "{\"bizId\":\"CRED-MISSING\"}")
                        .param("idempotency_key", "idem-credential-missing-" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(MISSING_CALLER_PARTITION_KEY_MESSAGE))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain(PARTITION_KEY);
    }

    /**
     * 验证 caller 分区键会写入批次调用方归因。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void batchUploadUsesCallerPartitionKeyIdentity() throws Exception {
        mockMvc.perform(multipart(BATCHES_PATH)
                        .file(uploadFile())
                        .header(CALLER_PARTITION_HEADER, PARTITION_KEY)
                        .param("metadata", "{\"bizId\":\"CRED-API-KEY\"}")
                        .param("idempotency_key", "idem-credential-api-key-" + UUID.randomUUID()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.client_id").value(PARTITION_KEY))
                .andExpect(jsonPath("$.source_app").value(DEFAULT_SOURCE_APP))
                .andExpect(jsonPath("$.tenant_key").value(PARTITION_KEY));
    }

    /**
     * 验证未配置的 caller 分区键也会作为独立分区被接受。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void batchUploadAcceptsUnconfiguredCallerPartitionKey() throws Exception {
        mockMvc.perform(multipart(BATCHES_PATH)
                        .file(uploadFile())
                        .header(CALLER_PARTITION_HEADER, OTHER_PARTITION_KEY)
                        .param("metadata", "{\"bizId\":\"CRED-BEARER\"}")
                        .param("idempotency_key", "idem-credential-bearer-" + UUID.randomUUID()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.client_id").value(OTHER_PARTITION_KEY))
                .andExpect(jsonPath("$.source_app").value(DEFAULT_SOURCE_APP))
                .andExpect(jsonPath("$.tenant_key").value(OTHER_PARTITION_KEY));
    }

    /**
     * 验证 caller 分区键会清理首尾空白后再写入批次归因。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void batchUploadTrimsCallerPartitionKey() throws Exception {
        mockMvc.perform(multipart(BATCHES_PATH)
                        .file(uploadFile())
                        .header(CALLER_PARTITION_HEADER, "  " + PARTITION_KEY + "  ")
                        .param("metadata", "{\"bizId\":\"CRED-INVALID\"}")
                        .param("idempotency_key", "idem-credential-invalid-" + UUID.randomUUID()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.client_id").value(PARTITION_KEY))
                .andExpect(jsonPath("$.source_app").value(DEFAULT_SOURCE_APP))
                .andExpect(jsonPath("$.tenant_key").value(PARTITION_KEY));
    }

    /**
     * 创建测试上传文件。
     *
     * @return 上传文件
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private MockMultipartFile uploadFile() {
        return new MockMultipartFile(FILES_PART_NAME, UPLOAD_FILE_NAME, UPLOAD_CONTENT_TYPE,
                UPLOAD_CONTENT.getBytes());
    }
}
