package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingRequest;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingResult;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessor;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmUsageType;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 可配置 LLM Markdown 后处理器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
class ConfigurableMarkdownPostProcessorTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final OffsetDateTime CHECKED_AT = OffsetDateTime.parse("2026-06-12T12:00:00+08:00");

    /**
     * 存在数据库配置时应优先使用运行时配置调用 LLM。
     *
     * @throws Exception 测试 HTTP 服务异常时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void usesRuntimeConfigBeforeFallbackProcessor() throws Exception {
        try (MockLlmServer server = MockLlmServer.start()) {
            LlmMarkdownConfig config = LlmMarkdownConfig.configured(
                    "default", server.endpoint().toString(), "runtime-model", "sk-runtime")
                    .updateHealth(true, "", CHECKED_AT);
            ConfigurableMarkdownPostProcessor processor = new ConfigurableMarkdownPostProcessor(OBJECT_MAPPER,
                    new FixedConfigRepository(config), new FallbackProcessor("fallback text"));

            MarkdownPostProcessingResult result = processor.process(request());

            assertThat(result.markdown()).isEqualTo("# 运行时 Markdown");
            assertThat(server.lastAuthorization()).isEqualTo("Bearer sk-runtime");
            assertThat(server.lastBody()).contains("\"model\":\"runtime-model\"");
        }
    }

    /**
     * 没有数据库配置时应直通 OCR 原文。
     *
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void returnsOriginalOcrTextWhenRuntimeConfigIsAbsent() {
        ConfigurableMarkdownPostProcessor processor = new ConfigurableMarkdownPostProcessor(OBJECT_MAPPER,
                new FixedConfigRepository(null), new FallbackProcessor("fallback text"));

        MarkdownPostProcessingResult result = processor.process(request());

        assertThat(result.markdown()).isEqualTo("OCR 文本");
        assertThat(result.markdownApplied()).isFalse();
        assertThat(result.warnings()).contains("no_available_llm_config");
    }

    /**
     * 运行时配置暂停使用时应直通 OCR 原文且不调用兜底 LLM。
     *
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void returnsPassthroughWhenRuntimeConfigIsDisabled() {
        LlmMarkdownConfig disabledConfig = LlmMarkdownConfig.configured(
                "default", "https://llm.example.com/v1/chat/completions", "runtime-model", "sk-runtime")
                .withEnabled(false);
        ConfigurableMarkdownPostProcessor processor = new ConfigurableMarkdownPostProcessor(OBJECT_MAPPER,
                new FixedConfigRepository(disabledConfig), new FallbackProcessor("fallback text"));

        MarkdownPostProcessingResult result = processor.process(request());

        assertThat(result.markdown()).isEqualTo("OCR 文本");
        assertThat(result.markdownApplied()).isFalse();
    }

    /**
     * 创建 Markdown 后处理请求。
     *
     * @return Markdown 后处理请求
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private MarkdownPostProcessingRequest request() {
        return new MarkdownPostProcessingRequest("doc-1", "demo.txt", Map.of("source", "test"), "OCR 文本");
    }

    /**
     * 固定配置仓储。
     *
     * @param config LLM Markdown 配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private record FixedConfigRepository(LlmMarkdownConfig config) implements LlmMarkdownConfigRepository {

        /**
         * 查询当前配置。
         *
         * @return 当前配置
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        @Override
        public Optional<LlmMarkdownConfig> find() {
            return Optional.ofNullable(config);
        }

        /**
         * 按用途查询配置。
         *
         * @param usageType 配置用途
         * @return 配置列表
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        @Override
        public List<LlmMarkdownConfig> listByUsage(LlmUsageType usageType) {
            return Optional.ofNullable(config)
                    .filter(current -> current.usageType() == usageType)
                    .stream()
                    .toList();
        }

        /**
         * 保存配置。
         *
         * @param config LLM Markdown 配置
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        @Override
        public void save(LlmMarkdownConfig config) {
        }

        /**
         * 批量保存配置。
         *
         * @param configs LLM Markdown 配置列表
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        @Override
        public void saveAll(List<LlmMarkdownConfig> configs) {
        }
    }

    /**
     * 固定兜底处理器。
     *
     * @param markdown Markdown 文本
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private record FallbackProcessor(String markdown) implements MarkdownPostProcessor {

        /**
         * 返回固定 Markdown 文本。
         *
         * @param request Markdown 后处理请求
         * @return Markdown 后处理结果
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        @Override
        public MarkdownPostProcessingResult process(MarkdownPostProcessingRequest request) {
            return MarkdownPostProcessingResult.markdown(markdown);
        }
    }

    /**
     * 测试用 LLM HTTP 服务。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static final class MockLlmServer implements AutoCloseable {

        private final HttpServer server;
        private String lastAuthorization = "";
        private String lastBody = "";

        /**
         * 创建测试 HTTP 服务。
         *
         * @param server HTTP 服务
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        private MockLlmServer(HttpServer server) {
            this.server = server;
        }

        /**
         * 启动测试 HTTP 服务。
         *
         * @return 测试 HTTP 服务
         * @throws IOException 启动失败时抛出
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        static MockLlmServer start() throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            MockLlmServer mockServer = new MockLlmServer(server);
            server.createContext("/v1/chat/completions", mockServer::handle);
            server.start();
            return mockServer;
        }

        /**
         * 读取接口地址。
         *
         * @return 接口地址
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        URI endpoint() {
            return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions");
        }

        /**
         * 读取最后一次 Authorization Header。
         *
         * @return Authorization Header
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        String lastAuthorization() {
            return lastAuthorization;
        }

        /**
         * 读取最后一次请求体。
         *
         * @return 请求体
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        String lastBody() {
            return lastBody;
        }

        /**
         * 处理 LLM 请求。
         *
         * @param exchange HTTP 交换对象
         * @throws IOException 读写失败时抛出
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        private void handle(HttpExchange exchange) throws IOException {
            lastAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
            lastBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] response = """
                    {"choices":[{"message":{"content":"# 运行时 Markdown"}}]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        }

        /**
         * 关闭测试 HTTP 服务。
         *
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        @Override
        public void close() {
            server.stop(0);
        }
    }
}
