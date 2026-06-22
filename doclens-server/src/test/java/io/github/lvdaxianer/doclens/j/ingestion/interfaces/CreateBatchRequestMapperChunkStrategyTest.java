package io.github.lvdaxianer.doclens.j.ingestion.interfaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.lvdaxianer.doclens.j.api.CreateBatchRequest;
import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import io.github.lvdaxianer.doclens.j.ingestion.infrastructure.CallerCredentialResolver;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.JsonCodec;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

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
    /** 通用分块策略。 */
    private static final String GENERAL_CHUNK_STRATEGY = "GENERAL";
    /** 技术文档分块策略。 */
    private static final String TECHNICAL_CHUNK_STRATEGY = "TECHNICAL";

    private JsonCodec jsonCodec;
    private CallerCredentialResolver callerCredentialResolver;
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
        callerCredentialResolver = mock(CallerCredentialResolver.class);
        mapper = new CreateBatchRequestMapper(jsonCodec, callerCredentialResolver);
        when(callerCredentialResolver.resolve(CALLER_PARTITION_KEY, ""))
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

}
