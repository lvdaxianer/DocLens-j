package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCreateRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeDeploymentType;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Ollama OCR 客户端测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
class OllamaOcrClientTest {

    private static final int SERVER_BACKLOG = 1;
    private static final int TEST_TIMEOUT_SECONDS = 5;
    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-12T10:00:00+08:00");

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Ollama 请求必须使用固定 Markdown prompt，避免 prompt 换行和标点导致模型行为漂移。
     *
     * @throws IOException 本地测试服务启动失败
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void sendsDeepSeekOcrGenerateRequestWithFixedMarkdownPrompt() throws IOException {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = startServer(exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            writeResponse(exchange, 200, "{\"response\":\"# 发票\\n金额：100\",\"done\":true}");
        });

        try {
            OllamaOcrClient client = new OllamaOcrClient(objectMapper, Duration.ofSeconds(TEST_TIMEOUT_SECONDS));
            ImageOcrResult result = client.recognizeImage(ollamaNode(server), request());
            JsonNode body = objectMapper.readTree(requestBody.get());

            assertThat(body.path("model").asText()).isEqualTo("deepseek-ocr:latest");
            assertThat(body.path("prompt").asText()).isEqualTo(OllamaOcrClient.DEEPSEEK_OCR_MARKDOWN_PROMPT);
            assertThat(body.path("stream").asBoolean()).isFalse();
            assertThat(body.path("images")).hasSize(1);
            assertThat(body.path("images").get(0).asText()).isEqualTo("aW1hZ2U=");
            assertThat(result.pageText()).first().extracting("text").isEqualTo("# 发票\n金额：100");
        } finally {
            server.stop(0);
        }
    }

    /**
     * Ollama 节点 URI 固定拼接到 /api/generate。
     *
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void buildsRuntimeNodeGenerateUriFromHostAndPort() {
        OllamaOcrClient client = new OllamaOcrClient(objectMapper, Duration.ofSeconds(TEST_TIMEOUT_SECONDS));
        OcrRuntimeNode node = new OcrRuntimeNode(ollamaNode("ollama-215", "10.100.30.215", 11434));

        assertThat(client.generateUri(node)).isEqualTo(URI.create("http://10.100.30.215:11434/api/generate"));
    }

    /**
     * 启动本地 HTTP 测试服务。
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
     * 创建指向本地服务的 Ollama 运行时节点。
     *
     * @param server 本地 HTTP 测试服务
     * @return Ollama 运行时节点
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private OcrRuntimeNode ollamaNode(HttpServer server) {
        OcrNode node = ollamaNode("ollama-local", "127.0.0.1", server.getAddress().getPort());
        return new OcrRuntimeNode(node);
    }

    /**
     * 创建 Ollama OCR 节点。
     *
     * @param nodeId 节点 ID
     * @param host 节点主机
     * @param port 节点端口
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private OcrNode ollamaNode(String nodeId, String host, int port) {
        return OcrNode.create(new OcrNodeCreateRequest(nodeId, "ollama",
                OcrNodeDeploymentType.OFFLINE, "Ollama", host, port, "ollama",
                "deepseek-ocr:latest", "", false, true, true, 100, 4, BASE_TIME));
    }

    /**
     * 创建图片 OCR 请求。
     *
     * @return 图片 OCR 请求
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private ImageOcrRequest request() {
        return new ImageOcrRequest("batch-ollama", "doc-ollama", "page.png", 1,
                "image".getBytes(StandardCharsets.UTF_8), JsonPayload.empty());
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
