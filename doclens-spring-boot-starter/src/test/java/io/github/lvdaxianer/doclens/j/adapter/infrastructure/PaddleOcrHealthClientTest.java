package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCreateRequest;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * PaddleOCR 健康检查客户端测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
class PaddleOcrHealthClientTest {

    private static final int SERVER_BACKLOG = 1;
    private static final int TEST_TIMEOUT_SECONDS = 5;
    private static final int HTTP_OK_STATUS = 200;
    private static final int HEALTHY_ERROR_CODE = 0;
    private static final int UNHEALTHY_ERROR_CODE = 500;
    private static final int PADDLE_IMAGE_FILE_TYPE = 1;
    private static final String HEALTH_CHECK_IMAGE_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=";
    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-11T05:00:00+08:00");

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * PaddleOCR 原生健康检查应通过 /ocr JSON/Base64 探测真实识别接口。
     *
     * @throws IOException 本地测试服务启动失败
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void healthCheckPostsJsonBase64ToNativeOcrEndpoint() throws IOException {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> requestPath = new AtomicReference<>();
        AtomicReference<String> contentType = new AtomicReference<>();
        HttpServer server = startServer(exchange -> {
            method.set(exchange.getRequestMethod());
            requestPath.set(exchange.getRequestURI().getPath());
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            writeResponse(exchange, HTTP_OK_STATUS, """
                    {"errorCode":%d,"errorMsg":"Success","rec_texts":["PaddleOCR API Test 112"],"rec_scores":[0.99]}
                    """.formatted(HEALTHY_ERROR_CODE));
        });

        try {
            PaddleOcrHealthClient client = new PaddleOcrHealthClient(TEST_TIMEOUT_SECONDS);
            boolean healthy = client.isHealthy(nodeFor(server));

            assertThat(healthy).isTrue();
            assertThat(method.get()).isEqualTo("POST");
            assertThat(requestPath.get()).isEqualTo("/ocr");
            assertThat(contentType.get()).isEqualTo("application/json");
            com.fasterxml.jackson.databind.JsonNode body = objectMapper.readTree(requestBody.get());
            assertThat(body.path("file").asText()).isEqualTo(HEALTH_CHECK_IMAGE_BASE64);
            assertThat(body.path("fileType").asInt()).isEqualTo(PADDLE_IMAGE_FILE_TYPE);
            assertThat(body.path("visualize").asBoolean()).isFalse();
        } finally {
            server.stop(0);
        }
    }

    /**
     * PaddleOCR 原生健康检查应按 errorCode 判断业务健康状态。
     *
     * @throws IOException 本地测试服务启动失败
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void healthCheckRequiresZeroErrorCode() throws IOException {
        HttpServer server = startServer(exchange -> writeResponse(exchange, HTTP_OK_STATUS, """
                {"errorCode":%d,"errorMsg":"model unavailable"}
                """.formatted(UNHEALTHY_ERROR_CODE)));

        try {
            PaddleOcrHealthClient client = new PaddleOcrHealthClient(TEST_TIMEOUT_SECONDS);
            boolean healthy = client.isHealthy(nodeFor(server));

            assertThat(healthy).isFalse();
        } finally {
            server.stop(0);
        }
    }

    /**
     * 启动本地 OCR 健康测试服务。
     *
     * @param handler 请求处理器
     * @return 本地 HTTP 测试服务
     * @throws IOException 本地端口监听失败
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private HttpServer startServer(ThrowingExchangeHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), SERVER_BACKLOG);
        server.createContext("/ocr", exchange -> {
            try {
                handler.handle(exchange);
            } catch (IOException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                throw ex;
            }
        });
        server.start();
        return server;
    }

    /**
     * 创建指向本地测试服务的 OCR 节点。
     *
     * @param server 本地 HTTP 测试服务
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private OcrNode nodeFor(HttpServer server) {
        return OcrNode.create(new OcrNodeCreateRequest("node_112", "paddle_ocr", "paddle-112",
                "127.0.0.1", server.getAddress().getPort(), true, true, 100, 4, BASE_TIME));
    }

    /**
     * 写入测试响应。
     *
     * @param exchange HTTP 交换对象
     * @param statusCode 响应状态码
     * @param responseBody 响应体
     * @throws IOException 响应写入失败
     * @author lvdaxianerplus
     * @date 2026-06-11
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
     * @date 2026-06-11
     */
    @FunctionalInterface
    private interface ThrowingExchangeHandler {

        /**
         * 处理 HTTP 请求。
         *
         * @param exchange HTTP 交换对象
         * @throws IOException 请求处理失败
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        void handle(HttpExchange exchange) throws IOException;
    }
}
