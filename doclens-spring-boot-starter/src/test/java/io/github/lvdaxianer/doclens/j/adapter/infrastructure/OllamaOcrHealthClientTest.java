package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCreateRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeDeploymentType;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Ollama OCR 健康检查客户端测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
class OllamaOcrHealthClientTest {

    private static final int SERVER_BACKLOG = 1;
    private static final int TEST_TIMEOUT_SECONDS = 5;
    private static final int HTTP_OK_STATUS = 200;
    private static final int DEFAULT_WEIGHT = 100;
    private static final int DEFAULT_MAX_CONCURRENCY = 4;
    private static final String OLLAMA_MODEL_KEY = "ollama";
    private static final String OLLAMA_CHANNEL_KEY = "ollama";
    private static final String OLLAMA_PROVIDER_MODEL = "deepseek-ocr:latest";
    private static final String HEALTH_CHECK_IMAGE_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=";
    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-12T14:00:00+08:00");

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Ollama 健康检查应向 /api/generate 发送固定 Markdown prompt 和 1x1 图片。
     *
     * @throws IOException 本地测试服务启动失败
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void healthCheckPostsGenerateRequestWithFixedMarkdownPrompt() throws IOException {
        AtomicReference<String> requestMethod = new AtomicReference<>();
        AtomicReference<String> requestPath = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = startServer(exchange -> {
            requestMethod.set(exchange.getRequestMethod());
            requestPath.set(exchange.getRequestURI().getPath());
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            writeResponse(exchange, HTTP_OK_STATUS, "{\"response\":\"\",\"done\":true}");
        });

        try {
            OllamaOcrHealthClient client = new OllamaOcrHealthClient(objectMapper, TEST_TIMEOUT_SECONDS);
            boolean healthy = client.isHealthy(nodeFor(server));
            JsonNode body = objectMapper.readTree(requestBody.get());

            assertThat(healthy).isTrue();
            assertThat(requestMethod.get()).isEqualTo("POST");
            assertThat(requestPath.get()).isEqualTo("/api/generate");
            assertThat(body.path("model").asText()).isEqualTo(OLLAMA_PROVIDER_MODEL);
            assertThat(body.path("prompt").asText()).isEqualTo(OllamaOcrClient.DEEPSEEK_OCR_MARKDOWN_PROMPT);
            assertThat(body.path("images")).singleElement().satisfies(image ->
                    assertThat(image.asText()).isEqualTo(HEALTH_CHECK_IMAGE_BASE64));
            assertThat(body.path("stream").asBoolean()).isFalse();
        } finally {
            server.stop(0);
        }
    }

    /**
     * Ollama 健康检查遇到 error 字段时应判定为不健康。
     *
     * @throws IOException 本地测试服务启动失败
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void healthCheckRejectsOllamaErrorBody() throws IOException {
        HttpServer server = startServer(exchange -> writeResponse(exchange, HTTP_OK_STATUS,
                "{\"error\":\"model not found\"}"));

        try {
            OllamaOcrHealthClient client = new OllamaOcrHealthClient(objectMapper, TEST_TIMEOUT_SECONDS);
            boolean healthy = client.isHealthy(nodeFor(server));

            assertThat(healthy).isFalse();
        } finally {
            server.stop(0);
        }
    }

    /**
     * 启动本地 Ollama 健康测试服务。
     *
     * @param handler 请求处理器
     * @return 本地 HTTP 测试服务
     * @throws IOException 本地端口监听失败
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private HttpServer startServer(ThrowingExchangeHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), SERVER_BACKLOG);
        server.createContext("/api/generate", exchange -> handler.handle(exchange));
        server.start();
        return server;
    }

    /**
     * 创建指向本地测试服务的 Ollama OCR 节点。
     *
     * @param server 本地 HTTP 测试服务
     * @return Ollama OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private OcrNode nodeFor(HttpServer server) {
        return OcrNode.create(new OcrNodeCreateRequest("ollama-health", OLLAMA_MODEL_KEY,
                OcrNodeDeploymentType.OFFLINE, "ollama-health", "127.0.0.1", server.getAddress().getPort(),
                OLLAMA_CHANNEL_KEY, OLLAMA_PROVIDER_MODEL, "", false, true, true, DEFAULT_WEIGHT,
                DEFAULT_MAX_CONCURRENCY, BASE_TIME));
    }

    /**
     * 写入测试响应。
     *
     * @param exchange HTTP 交换对象
     * @param statusCode 响应状态码
     * @param responseBody 响应体
     * @throws IOException 响应写入失败
     * @author lvdaxianerplus
     * @date 2026-06-12
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
     * @date 2026-06-12
     */
    @FunctionalInterface
    private interface ThrowingExchangeHandler {

        /**
         * 处理 HTTP 请求。
         *
         * @param exchange HTTP 交换对象
         * @throws IOException 请求处理失败
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        void handle(HttpExchange exchange) throws IOException;
    }
}
