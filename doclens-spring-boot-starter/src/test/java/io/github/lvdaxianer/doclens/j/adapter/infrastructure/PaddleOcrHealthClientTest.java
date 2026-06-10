package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCreateRequest;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/**
 * PaddleOCR 健康检查客户端测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
class PaddleOcrHealthClientTest {

    private static final int SERVER_BACKLOG = 1;
    private static final int TEST_TIMEOUT_SECONDS = 2;
    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-10T10:00:00+08:00");

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 健康检查应调用 PaddleOCR 原生 OCR 接口，验证服务具备真实识别能力。
     *
     * @throws IOException 本地测试服务启动或请求体解析失败
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void healthCheckPostsTinyJsonImageToNativeOcrEndpoint() throws IOException {
        CapturedRequest captured = new CapturedRequest();
        HttpServer server = startServer(exchange -> {
            captured.method = exchange.getRequestMethod();
            captured.path = exchange.getRequestURI().getPath();
            captured.body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            writeResponse(exchange, 200, "{\"errorCode\": 0, \"errorMsg\": \"Success\", \"rec_texts\": [\"ok\"]}");
        });

        try {
            boolean healthy = new PaddleOcrHealthClient(TEST_TIMEOUT_SECONDS).isHealthy(nodeFor(server));

            assertThat(healthy).isTrue();
            assertThat(captured.method).isEqualTo("POST");
            assertThat(captured.path).isEqualTo("/ocr");
            assertRequestBody(captured.body);
        } finally {
            server.stop(0);
        }
    }

    /**
     * PaddleOCR 原生响应失败时应判定节点不健康。
     *
     * @throws IOException 本地测试服务启动失败
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void healthCheckRejectsNativeOcrErrorResponse() throws IOException {
        HttpServer server = startServer(exchange -> writeResponse(exchange, 200,
                "{\"errorCode\":500,\"errorMsg\":\"model unavailable\"}"));

        try {
            boolean healthy = new PaddleOcrHealthClient(TEST_TIMEOUT_SECONDS).isHealthy(nodeFor(server));

            assertThat(healthy).isFalse();
        } finally {
            server.stop(0);
        }
    }

    /**
     * 校验健康检查请求体包含原生 OCR API 必要字段。
     *
     * @param body 请求体
     * @throws IOException JSON 解析失败
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void assertRequestBody(String body) throws IOException {
        JsonNode request = objectMapper.readTree(body);
        assertThat(request.path("fileType").asInt()).isEqualTo(1);
        assertThat(request.path("visualize").asBoolean()).isFalse();
        assertThat(Base64.getDecoder().decode(request.path("file").asText())).isNotEmpty();
    }

    /**
     * 创建测试用 OCR 节点。
     *
     * @param server 本地 PaddleOCR 测试服务
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrNode nodeFor(HttpServer server) {
        return OcrNode.create(new OcrNodeCreateRequest("node-1", "paddle_ocr", "node-1",
                "127.0.0.1", server.getAddress().getPort(), true, true, 100, 4, BASE_TIME));
    }

    /**
     * 启动仅支持 /ocr 的 PaddleOCR 测试服务。
     *
     * @param handler 请求处理器
     * @return 本地 HTTP 服务
     * @throws IOException 本地端口监听失败
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private HttpServer startServer(ThrowingExchangeHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), SERVER_BACKLOG);
        server.createContext("/ocr", handler::handle);
        server.start();
        return server;
    }

    /**
     * 写入 JSON 响应。
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
     * 捕获到的 HTTP 请求。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static final class CapturedRequest {

        private String method = "";
        private String path = "";
        private String body = "";
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
