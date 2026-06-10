package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingRequest;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingResult;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.AnthropicMarkdownPostProcessor.AnthropicMarkdownPostProcessorOptions;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Anthropic HTTP LLM Markdown 后处理器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
class AnthropicMarkdownPostProcessorTest {

    private static final int SERVER_BACKLOG = 1;
    private static final String API_KEY = "sk-anthropic-secret";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 后处理器应按 Anthropic messages 契约请求 Markdown。
     *
     * @throws IOException 本地 HTTP 服务启动或 JSON 解析失败
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void convertsOcrTextToMarkdownThroughAnthropicMessagesApi() throws IOException {
        AtomicReference<String> apiKey = new AtomicReference<>();
        AtomicReference<String> anthropicVersion = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = startServer(exchange -> {
            apiKey.set(exchange.getRequestHeaders().getFirst("x-api-key"));
            anthropicVersion.set(exchange.getRequestHeaders().getFirst("anthropic-version"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            writeResponse(exchange, 200, "{\"content\":[{\"type\":\"text\",\"text\":\"# 发票\\n\\n金额 100\"}]}");
        });

        try {
            AnthropicMarkdownPostProcessor processor = processor(endpointFor(server), "MiniMax-M3", API_KEY);

            MarkdownPostProcessingResult result = processor.process(request());

            JsonNode body = objectMapper.readTree(requestBody.get());
            assertThat(apiKey.get()).isEqualTo(API_KEY);
            assertThat(anthropicVersion.get()).isEqualTo(ANTHROPIC_VERSION);
            assertThat(body.path("model").asText()).isEqualTo("MiniMax-M3");
            assertThat(body.path("max_tokens").asInt()).isGreaterThan(0);
            assertThat(body.path("system").asText()).contains("你是一个严谨的文档结构化编辑器");
            assertThat(body.path("messages").path(0).path("content").asText())
                    .contains("{\"kind\":\"invoice\"}")
                    .contains("OCR 文本：\n原始 OCR 文本");
            assertThat(result.markdown()).isEqualTo("# 发票\n\n金额 100");
        } finally {
            server.stop(0);
        }
    }

    /**
     * Anthropic 供应商错误中回显 API Key 时，异常消息必须脱敏。
     *
     * @throws IOException 本地 HTTP 服务启动失败
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void sanitizesApiKeyFromAnthropicErrorResponse() throws IOException {
        HttpServer server = startServer(exchange -> writeResponse(exchange, 401,
                "{\"error\":{\"message\":\"invalid api key sk-anthropic-secret\"}}"));

        try {
            AnthropicMarkdownPostProcessor processor = processor(endpointFor(server), "MiniMax-M3", API_KEY);

            assertThatThrownBy(() -> processor.process(request()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("***")
                    .hasMessageNotContaining(API_KEY);
        } finally {
            server.stop(0);
        }
    }

    /**
     * 创建被测后处理器。
     *
     * @param endpoint LLM endpoint
     * @param model 模型名称
     * @param apiKey API Key
     * @return Anthropic Markdown 后处理器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private AnthropicMarkdownPostProcessor processor(URI endpoint, String model, String apiKey) {
        AnthropicMarkdownPostProcessorOptions options = new AnthropicMarkdownPostProcessorOptions(endpoint, model,
                apiKey, Duration.ofSeconds(5));
        return new AnthropicMarkdownPostProcessor(objectMapper, options);
    }

    /**
     * 创建 Markdown 后处理请求。
     *
     * @return Markdown 后处理请求
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private MarkdownPostProcessingRequest request() {
        return new MarkdownPostProcessingRequest("doc-1", "invoice.txt", Map.of("kind", "invoice"), "原始 OCR 文本");
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
        server.createContext("/anthropic/v1/messages", exchange -> handler.handle(exchange));
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
    private URI endpointFor(HttpServer server) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/anthropic/v1/messages");
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
        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
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
