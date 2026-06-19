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
        when(callerCredentialResolver.resolve(null, null))
                .thenReturn(new CallerIdentity("client-1", "dashboard", Optional.empty()));
        when(jsonCodec.parseObject("{}")).thenReturn(Map.of());
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

        assertThat(request.chunkStrategy()).isEqualTo("GENERAL");
    }

    /**
     * 显式 chunkStrategy 应保留原值。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Test
    void preservesExplicitChunkStrategyOnMappedRequest() throws Exception {
        CreateBatchRequest request = mapper.toRequest(List.of(file()), request("TECHNICAL"));

        assertThat(request.chunkStrategy()).isEqualTo("TECHNICAL");
    }

    /**
     * 构建上传文件。
     *
     * @return 上传文件
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private MockMultipartFile file() {
        return new MockMultipartFile("files", "demo.txt", "text/plain", "demo".getBytes());
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
        request.setParameter("metadata", "{}");
        if (chunkStrategy != null) {
            request.setParameter("chunkStrategy", chunkStrategy);
        } else {
            // 默认情况下不传 chunkStrategy，验证服务端兜底。
        }
        return request;
    }

}
