package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCreateRequest;
import io.github.lvdaxianer.doclens.j.shared.config.DocLensProperties;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * PaddleOCR 原生客户端测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
class PaddleOcrNativeClientTest {

    private static final int SERVER_BACKLOG = 1;
    private static final int TEST_TIMEOUT_SECONDS = 5;
    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-08T10:00:00+08:00");

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 原生服务只接受普通 HTTP/1.1 请求。
     *
     * @throws IOException 本地测试服务启动失败
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void recognizeImageUsesPlainHttp11Request() throws IOException {
        AtomicReference<String> upgradeHeader = new AtomicReference<>();
        AtomicReference<String> protocol = new AtomicReference<>();
        HttpServer server = startServer(exchange -> {
            upgradeHeader.set(exchange.getRequestHeaders().getFirst("Upgrade"));
            protocol.set(exchange.getProtocol());
            writeResponse(exchange, 200, "{\"ok\":true}");
        });

        try {
            PaddleOcrNativeClient client = new PaddleOcrNativeClient(propertiesFor(server), objectMapper);
            JsonNode response = client.recognizeImage("image".getBytes(StandardCharsets.UTF_8));

            assertThat(response.path("ok").asBoolean()).isTrue();
            assertThat(protocol.get()).isEqualTo("HTTP/1.1");
            assertThat(upgradeHeader.get()).isNull();
        } finally {
            server.stop(0);
        }
    }

    /**
     * 非 2xx 响应应保留响应体，方便排查模型服务错误。
     *
     * @throws IOException 本地测试服务启动失败
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void recognizeImageIncludesResponseBodyWhenNativeApiFails() throws IOException {
        HttpServer server = startServer(exchange -> writeResponse(exchange, 422, "{\"detail\":\"Unsupported upgrade request.\"}"));

        try {
            PaddleOcrNativeClient client = new PaddleOcrNativeClient(propertiesFor(server), objectMapper);

            assertThatThrownBy(() -> client.recognizeImage("image".getBytes(StandardCharsets.UTF_8)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("HTTP 422")
                    .hasMessageContaining("Unsupported upgrade request");
        } finally {
            server.stop(0);
        }
    }

    /**
     * 运行时节点使用 host 和 port 拼接 PaddleOCR 固定接口路径。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void buildsRuntimeNodeUrisFromHostAndPort() {
        PaddleOcrNativeClient client = new PaddleOcrNativeClient(propertiesWithoutServer(), objectMapper);
        OcrRuntimeNode node = runtimeNode("node_215", "10.100.30.215", 8080);

        assertThat(client.ocrUri(node)).isEqualTo(URI.create("http://10.100.30.215:8080/ocr"));
        assertThat(client.healthUri(node)).isEqualTo(URI.create("http://10.100.30.215:8080/ocr"));
    }

    /**
     * 未配置 legacy endpoint 时应立即提示配置缺失。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-21
     */
    @Test
    void rejectsLegacyRecognitionWhenEndpointIsBlank() {
        PaddleOcrNativeClient client = new PaddleOcrNativeClient(propertiesWithEndpoint(""), objectMapper);

        assertThatThrownBy(() -> client.recognizeImage("image".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("PaddleOCR legacy endpoint is not configured");
    }

    /**
     * 零秒超时配置应降级为最小 HTTP 超时，避免禁用 PaddleOCR 时客户端构造失败。
     *
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void treatsZeroTimeoutAsMinimumHttpTimeout() {
        PaddleOcrNativeClient client = new PaddleOcrNativeClient(propertiesWithTimeout(0), objectMapper);

        OcrRuntimeNode node = runtimeNode("node_zero_timeout", "127.0.0.1", 18081);

        assertThat(client.ocrUri(node)).isEqualTo(URI.create("http://127.0.0.1:18081/ocr"));
    }

    /**
     * 启动本地 HTTP 测试服务。
     *
     * @param handler 请求处理器
     * @return 本地 HTTP 测试服务
     * @throws IOException 本地端口监听失败
     * @author lvdaxianerplus
     * @date 2026-06-08
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
     * 生成指向本地测试服务的配置。
     *
     * @param server 本地 HTTP 测试服务
     * @return DocLens 测试配置
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private DocLensProperties propertiesFor(HttpServer server) {
        return properties(endpointFor(server), TEST_TIMEOUT_SECONDS);
    }

    /**
     * 生成不依赖本地 HTTP 服务的配置。
     *
     * @return DocLens 测试配置
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private DocLensProperties propertiesWithoutServer() {
        return propertiesWithTimeout(TEST_TIMEOUT_SECONDS);
    }

    /**
     * 生成指定 PaddleOCR endpoint 的测试配置。
     *
     * @param endpoint PaddleOCR 接口地址
     * @return DocLens 测试配置
     * @author lvdaxianer@yeah.net
     * @date 2026-07-21
     */
    private DocLensProperties propertiesWithEndpoint(String endpoint) {
        return properties(endpoint, TEST_TIMEOUT_SECONDS);
    }

    /**
     * 生成指定 PaddleOCR endpoint 和超时时间的测试配置。
     *
     * @param endpoint PaddleOCR 接口地址
     * @param timeoutSeconds PaddleOCR 超时秒数
     * @return DocLens 测试配置
     * @author lvdaxianer@yeah.net
     * @date 2026-07-21
     */
    private DocLensProperties properties(String endpoint, int timeoutSeconds) {
        return new DocLensProperties(
                "target/test-storage",
                true,
                "worker-test",
                new DocLensProperties.CallbackProperties(1, TEST_TIMEOUT_SECONDS),
                new DocLensProperties.AdapterProperties("paddle_ocr"),
                new DocLensProperties.PaddleOcrProperties(true, endpoint, timeoutSeconds, false),
                new DocLensProperties.OcrHealthProperties(3, 2),
                new DocLensProperties.ExtractionProperties(1),
                new DocLensProperties.PdfRenderProperties(144, "png"),
                new DocLensProperties.WordConversionProperties("soffice", TEST_TIMEOUT_SECONDS),
                threadPools()
        );
    }

    /**
     * 生成指定 PaddleOCR 超时时间的配置。
     *
     * @param timeoutSeconds PaddleOCR 超时秒数
     * @return DocLens 测试配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private DocLensProperties propertiesWithTimeout(int timeoutSeconds) {
        return properties("http://127.0.0.1:1/ocr", timeoutSeconds);
    }

    /**
     * 创建运行时 OCR 节点。
     *
     * @param nodeId 节点 ID
     * @param host 节点主机
     * @param port 节点端口
     * @return 运行时 OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private OcrRuntimeNode runtimeNode(String nodeId, String host, int port) {
        OcrNode node = OcrNode.create(new OcrNodeCreateRequest(
                nodeId, "paddle_ocr", nodeId, host, port, true, true, 100, 4, BASE_TIME));
        return new OcrRuntimeNode(node);
    }

    /**
     * 创建测试线程池隔离配置。
     *
     * @return 线程池隔离配置
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private DocLensProperties.ThreadPoolsProperties threadPools() {
        DocLensProperties.ThreadPoolProperties pool = new DocLensProperties.ThreadPoolProperties(1, 1, 1, 1,
                "doclens-test-");
        return new DocLensProperties.ThreadPoolsProperties(pool, pool, pool, pool);
    }

    /**
     * 拼接本地 OCR 服务地址。
     *
     * @param server 本地 HTTP 测试服务
     * @return OCR 接口地址
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private String endpointFor(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/ocr";
    }

    /**
     * 写入测试响应。
     *
     * @param exchange HTTP 交换对象
     * @param statusCode 响应状态码
     * @param responseBody 响应体
     * @throws IOException 响应写入失败
     * @author lvdaxianerplus
     * @date 2026-06-08
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
     * @date 2026-06-08
     */
    @FunctionalInterface
    private interface ThrowingExchangeHandler {

        /**
         * 处理 HTTP 请求。
         *
         * @param exchange HTTP 交换对象
         * @throws IOException 请求处理失败
         * @author lvdaxianerplus
         * @date 2026-06-08
         */
        void handle(HttpExchange exchange) throws IOException;
    }
}
