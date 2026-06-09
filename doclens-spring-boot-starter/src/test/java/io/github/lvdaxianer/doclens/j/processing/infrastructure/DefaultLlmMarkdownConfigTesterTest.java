package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigService;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigSettings;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigTestResponse;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigRepository;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * LLM 配置测试器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
class DefaultLlmMarkdownConfigTesterTest {

    private static final int SERVER_BACKLOG = 1;
    private static final String API_KEY = "sk-llm-secret";

    /**
     * 供应商探测失败时应返回可展示且已脱敏的失败原因。
     *
     * @throws IOException 本地 HTTP 服务启动失败
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void reportsConcreteFailureMessageWhenConnectivityTestFails() throws IOException {
        HttpServer server = startServer(exchange -> writeResponse(exchange, 401,
                "{\"error\":\"invalid api key sk-llm-secret\"}"));
        DefaultLlmMarkdownConfigTester tester = new DefaultLlmMarkdownConfigTester(new ObjectMapper(),
                new LlmMarkdownConfigService(new EmptyConfigRepository()));

        try {
            LlmMarkdownConfigTestResponse response = tester.test(new LlmMarkdownConfigSettings(
                    endpointFor(server), "markdown-model", API_KEY));

            assertThat(response.healthy()).isFalse();
            assertThat(response.message())
                    .contains("LLM Markdown returned HTTP 401")
                    .contains("***")
                    .doesNotContain(API_KEY);
        } finally {
            server.stop(0);
        }
    }

    /**
     * 启动本地 HTTP 测试服务。
     *
     * @param handler 请求处理器
     * @return 本地 HTTP 测试服务
     * @throws IOException 本地端口监听失败
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private HttpServer startServer(ThrowingExchangeHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), SERVER_BACKLOG);
        server.createContext("/v1/chat/completions", exchange -> handler.handle(exchange));
        server.start();
        return server;
    }

    /**
     * 拼接本地测试 endpoint。
     *
     * @param server 本地 HTTP 测试服务
     * @return endpoint
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private String endpointFor(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions";
    }

    /**
     * 写入测试响应。
     *
     * @param exchange HTTP 交换对象
     * @param statusCode 响应状态码
     * @param responseBody 响应体
     * @throws IOException 响应写入失败
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void writeResponse(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
        byte[] responseBytes = responseBody.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }

    /**
     * 空配置仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static final class EmptyConfigRepository implements LlmMarkdownConfigRepository {

        /**
         * 查询当前配置。
         *
         * @return 当前配置
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        @Override
        public Optional<LlmMarkdownConfig> find() {
            return Optional.empty();
        }

        /**
         * 保存配置。
         *
         * @param config LLM Markdown 配置
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        @Override
        public void save(LlmMarkdownConfig config) {
        }
    }

    /**
     * 允许抛出受检异常的 HTTP 处理器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @FunctionalInterface
    private interface ThrowingExchangeHandler {

        /**
         * 处理 HTTP 请求。
         *
         * @param exchange HTTP 交换对象
         * @throws IOException 请求处理失败
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        void handle(HttpExchange exchange) throws IOException;
    }
}
