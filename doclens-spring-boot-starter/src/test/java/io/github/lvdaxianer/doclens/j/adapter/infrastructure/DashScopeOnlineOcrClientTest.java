package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
 * DashScope compatible 在线 OCR 客户端测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
class DashScopeOnlineOcrClientTest {

    private static final int SERVER_BACKLOG = 1;
    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-09T12:00:00+08:00");

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 在线 OCR 客户端应调用 compatible chat completions 并映射文本。
     *
     * @throws IOException 本地测试服务启动失败
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void recognizesImageThroughDashScopeCompatibleApi() throws IOException {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = startServer(exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            writeResponse(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"发票标题\\n金额 100\"}}]}");
        });

        try {
            DashScopeOnlineOcrClient client = new DashScopeOnlineOcrClient(objectMapper, endpointFor(server),
                    Duration.ofSeconds(5));
            ImageOcrResult result = client.recognizeImage(new OcrRuntimeNode(onlineNode()), request());

            assertThat(authorization.get()).isEqualTo("Bearer sk-test");
            assertThat(requestBody.get())
                    .contains("qwen-vl-ocr-2025-11-20")
                    .contains("请仅输出图像中的文本内容")
                    .contains("data:image/png;base64,");
            assertThat(result.pageText()).first().extracting("text").isEqualTo("发票标题\n金额 100");
        } finally {
            server.stop(0);
        }
    }

    /**
     * 在线 OCR 错误消息不应回显 API Key。
     *
     * @throws IOException 本地测试服务启动失败
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void sanitizesApiKeyFromErrorResponse() throws IOException {
        HttpServer server = startServer(exchange -> writeResponse(exchange, 401,
                "{\"error\":\"invalid api key sk-test\"}"));

        try {
            DashScopeOnlineOcrClient client = new DashScopeOnlineOcrClient(objectMapper, endpointFor(server),
                    Duration.ofSeconds(5));

            assertThatThrownBy(() -> client.recognizeImage(new OcrRuntimeNode(onlineNode()), request()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("***")
                    .hasMessageNotContaining("sk-test");
        } finally {
            server.stop(0);
        }
    }

    /**
     * 在线 OCR 权限探测遇到模型无权限时应返回 false。
     *
     * @throws IOException 本地测试服务启动失败
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void permissionProbeReturnsFalseWhenModelAccessDenied() throws IOException {
        HttpServer server = startServer(exchange -> writeResponse(exchange, 403,
                "{\"error\":{\"message\":\"Model access denied.\",\"code\":\"Model.AccessDenied\"}}"));

        try {
            DashScopeOnlineOcrClient client = new DashScopeOnlineOcrClient(objectMapper, endpointFor(server),
                    Duration.ofSeconds(5));

            assertThat(client.hasExecutionPermission(new OcrRuntimeNode(onlineNode()))).isFalse();
        } finally {
            server.stop(0);
        }
    }

    /**
     * 创建在线 OCR 节点。
     *
     * @return 在线 OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrNode onlineNode() {
        return OcrNode.create(new OcrNodeCreateRequest("node-online", "paddle_ocr", OcrNodeDeploymentType.ONLINE,
                "在线 OCR", "", 0, "aliyun_bailian_dashscope", "qwen-vl-ocr-2025-11-20", "sk-test", true,
                true, true, 100, 4, BASE_TIME));
    }

    /**
     * 创建图片 OCR 请求。
     *
     * @return 图片 OCR 请求
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private ImageOcrRequest request() {
        return new ImageOcrRequest("batch-online", "doc-online", "page.png", 1, "image".getBytes(),
                JsonPayload.empty());
    }

    /**
     * 启动本地 HTTP 测试服务。
     *
     * @param handler 请求处理器
     * @return 本地 HTTP 测试服务
     * @throws IOException 本地端口监听失败
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private HttpServer startServer(ThrowingExchangeHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), SERVER_BACKLOG);
        server.createContext("/compatible-mode/v1/chat/completions", exchange -> handler.handle(exchange));
        server.start();
        return server;
    }

    /**
     * 拼接本地测试 endpoint。
     *
     * @param server 本地 HTTP 测试服务
     * @return endpoint
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private URI endpointFor(HttpServer server) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort()
                + "/compatible-mode/v1/chat/completions");
    }

    /**
     * 写入测试响应。
     *
     * @param exchange HTTP 交换对象
     * @param statusCode 响应状态码
     * @param responseBody 响应体
     * @throws IOException 响应写入失败
     * @author lvdaxianerplus
     * @date 2026-06-09
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
     * @date 2026-06-09
     */
    @FunctionalInterface
    private interface ThrowingExchangeHandler {

        /**
         * 处理 HTTP 请求。
         *
         * @param exchange HTTP 交换对象
         * @throws IOException 请求处理失败
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        void handle(HttpExchange exchange) throws IOException;
    }
}
