package io.github.lvdaxianer.doclens.j.ingestion.interfaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.lvdaxianer.doclens.j.api.CreateBatchRequest;
import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import io.github.lvdaxianer.doclens.j.ingestion.infrastructure.CallerPartitionResolver;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.JsonCodec;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * CreateBatchRequestMapper 的分块策略映射测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-19
 */
class CreateBatchRequestMapperChunkStrategyTest {

    /** 测试 caller 分区键请求头。 */
    private static final String CALLER_PARTITION_HEADER = "X-Doclens-Key";
    /** 测试 caller 分区键。 */
    private static final String CALLER_PARTITION_KEY = "tenant-east";
    /** 默认来源应用。 */
    private static final String DEFAULT_SOURCE_APP = "dashboard";
    /** 文件表单字段名。 */
    private static final String FILES_PART_NAME = "files";
    /** 测试文件名。 */
    private static final String TEST_FILE_NAME = "demo.txt";
    /** 测试文件内容类型。 */
    private static final String TEST_CONTENT_TYPE = "text/plain";
    /** 测试文件内容。 */
    private static final String TEST_CONTENT = "demo";
    /** metadata 参数名。 */
    private static final String METADATA_PARAM = "metadata";
    /** 空 JSON 对象。 */
    private static final String EMPTY_JSON_OBJECT = "{}";
    /** chunkStrategy 参数名。 */
    private static final String CHUNK_STRATEGY_PARAM = "chunkStrategy";
    /** callback_url 参数名。 */
    private static final String CALLBACK_URL_PARAM = "callback_url";
    /** 通用分块策略。 */
    private static final String GENERAL_CHUNK_STRATEGY = "GENERAL";
    /** 技术文档分块策略。 */
    private static final String TECHNICAL_CHUNK_STRATEGY = "TECHNICAL";
    /** 最大上传文件数。 */
    private static final int MAX_UPLOAD_FILE_COUNT = 30;

    private JsonCodec jsonCodec;
    private CallerPartitionResolver callerPartitionResolver;
    private CreateBatchRequestMapper mapper;

    /**
     * 初始化测试依赖。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @BeforeEach
    void setUp() {
        jsonCodec = mock(JsonCodec.class);
        callerPartitionResolver = mock(CallerPartitionResolver.class);
        mapper = new CreateBatchRequestMapper(jsonCodec, callerPartitionResolver);
        when(callerPartitionResolver.resolve(CALLER_PARTITION_KEY))
                .thenReturn(new CallerIdentity(CALLER_PARTITION_KEY, DEFAULT_SOURCE_APP,
                        Optional.of(CALLER_PARTITION_KEY)));
        when(jsonCodec.parseObject(EMPTY_JSON_OBJECT)).thenReturn(Map.of());
    }

    /**
     * 默认请求应暴露通用分块策略字段。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Test
    void exposesChunkStrategyOnMappedRequestByDefault() throws Exception {
        CreateBatchRequest request = mapper.toRequest(List.of(file()), request(null));

        assertThat(request.chunkStrategy()).isEqualTo(GENERAL_CHUNK_STRATEGY);
    }

    /**
     * 显式 chunkStrategy 应保留原值。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Test
    void preservesExplicitChunkStrategyOnMappedRequest() throws Exception {
        CreateBatchRequest request = mapper.toRequest(List.of(file()), request(TECHNICAL_CHUNK_STRATEGY));

        assertThat(request.chunkStrategy()).isEqualTo(TECHNICAL_CHUNK_STRATEGY);
    }

    /**
     * 上传文件数量超过服务端批次上限时应拒绝。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    @Test
    void rejectsUploadWhenFileCountExceedsServerLimit() {
        assertThatThrownBy(() -> mapper.toRequest(filesExceedingLimit(), request(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("最多上传 30 个文件");
    }

    /**
     * 非 HTTP 回调地址协议应被拒绝。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    @Test
    void rejectsCallbackUrlWithNonHttpScheme() {
        assertThatThrownBy(() -> mapper.toRequest(List.of(file()),
                requestWithCallback("httpx://example.com/callback")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("callback_url must be http or https URL");
    }

    /**
     * HTTPS 回调地址协议应保持可用。
     *
     * @throws Exception 请求映射失败时抛出
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    @Test
    void acceptsCallbackUrlWithHttpsScheme() throws Exception {
        CreateBatchRequest request = mapper.toRequest(List.of(file()),
                requestWithCallback("https://example.com/cb"));

        assertThat(request.callbackUrl()).isEqualTo("https://example.com/cb");
    }

    /**
     * 构建上传文件。
     *
     * @return 上传文件
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private MockMultipartFile file() {
        return new MockMultipartFile(FILES_PART_NAME, TEST_FILE_NAME, TEST_CONTENT_TYPE, TEST_CONTENT.getBytes());
    }

    /**
     * 构建超出批次上限的上传文件集合。
     *
     * @return 上传文件集合
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    private List<MultipartFile> filesExceedingLimit() {
        return java.util.stream.IntStream.rangeClosed(0, MAX_UPLOAD_FILE_COUNT)
                .<MultipartFile>mapToObj(index -> file())
                .toList();
    }

    /**
     * 构建 multipart 请求。
     *
     * @param chunkStrategy 分块策略
     * @return multipart 请求
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private MockHttpServletRequest request(String chunkStrategy) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CALLER_PARTITION_HEADER, CALLER_PARTITION_KEY);
        request.setParameter(METADATA_PARAM, EMPTY_JSON_OBJECT);
        if (chunkStrategy != null) {
            request.setParameter(CHUNK_STRATEGY_PARAM, chunkStrategy);
        } else {
            // 默认情况下不传 chunkStrategy，验证服务端兜底。
        }
        return request;
    }

    /**
     * 构建带回调地址的 multipart 请求。
     *
     * @param callbackUrl 回调地址
     * @return multipart 请求
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    private MockHttpServletRequest requestWithCallback(String callbackUrl) {
        MockHttpServletRequest request = request(null);
        request.setParameter(CALLBACK_URL_PARAM, callbackUrl);
        return request;
    }

}
