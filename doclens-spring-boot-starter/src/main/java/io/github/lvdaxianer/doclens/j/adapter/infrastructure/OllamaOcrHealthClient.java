package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ollama OCR 健康检查客户端。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
public class OllamaOcrHealthClient implements OcrHealthClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(OllamaOcrHealthClient.class);
    private static final String GENERATE_PATH = "/api/generate";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final String DEFAULT_MODEL = "deepseek-ocr:latest";
    private static final String FIELD_MODEL = "model";
    private static final String FIELD_PROMPT = "prompt";
    private static final String FIELD_IMAGES = "images";
    private static final String FIELD_STREAM = "stream";
    private static final String FIELD_ERROR = "error";
    private static final String HEALTH_CHECK_IMAGE_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=";
    private static final int MIN_TIMEOUT_SECONDS = 1;
    private static final int HTTP_SUCCESS_MIN = 200;
    private static final int HTTP_SUCCESS_MAX = 300;

    private final HttpClient httpClient;
    private final Duration timeout;
    private final ObjectMapper objectMapper;

    /**
     * 创建 Ollama OCR 健康检查客户端。
     *
     * @param objectMapper JSON 映射器
     * @param timeoutSeconds 超时秒数
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public OllamaOcrHealthClient(ObjectMapper objectMapper, int timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.timeout = Duration.ofSeconds(Math.max(MIN_TIMEOUT_SECONDS, timeoutSeconds));
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(timeout)
                .build();
    }

    /**
     * 通过 Ollama /api/generate 接口判断节点是否健康。
     *
     * @param node OCR 节点
     * @return 是否健康
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Override
    public boolean isHealthy(OcrNode node) {
        URI uri = generateUri(node);
        try {
            HttpResponse<String> response = httpClient.send(request(uri, node), HttpResponse.BodyHandlers.ofString());
            LOGGER.debug("[服务间调用] 收到响应|OllamaOCR健康检查|{}|{}|-|body=<omitted>",
                    uri, response.statusCode());
            return isSuccess(response);
        } catch (IOException ex) {
            LOGGER.warn("[服务间调用] 调用失败|OllamaOCR健康检查|{}|-|-|error={}", uri, ex.getMessage(), ex);
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.warn("[服务间调用] 调用失败|OllamaOCR健康检查|{}|-|-|error={}", uri, ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * 拼接 Ollama 生成接口 URI。
     *
     * @param node OCR 节点
     * @return Ollama 生成接口 URI
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    URI generateUri(OcrNode node) {
        return URI.create("http://" + node.host() + ":" + node.port() + GENERATE_PATH);
    }

    /**
     * 构建 Ollama 健康探测请求。
     *
     * @param uri 健康检查 URI
     * @param node OCR 节点
     * @return HTTP 请求
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private HttpRequest request(URI uri, OcrNode node) throws IOException {
        LOGGER.debug("[服务间调用] 发起请求|OllamaOCR健康检查|{}|POST|-|-|headers={{Content-Type=application/json}}, body=<omitted>",
                uri);
        return HttpRequest.newBuilder(uri)
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(timeout)
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody(node)))
                .build();
    }

    /**
     * 构建 Ollama 健康探测请求体。
     *
     * @param node OCR 节点
     * @return 请求体 JSON
     * @throws IOException JSON 序列化失败
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private String requestBody(OcrNode node) throws IOException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put(FIELD_MODEL, model(node));
        body.put(FIELD_PROMPT, OllamaOcrClient.DEEPSEEK_OCR_MARKDOWN_PROMPT);
        body.set(FIELD_IMAGES, images());
        body.put(FIELD_STREAM, false);
        return objectMapper.writeValueAsString(body);
    }

    /**
     * 构建健康探测图片数组。
     *
     * @return 健康探测图片数组
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private ArrayNode images() {
        ArrayNode images = objectMapper.createArrayNode();
        images.add(HEALTH_CHECK_IMAGE_BASE64);
        return images;
    }

    /**
     * 解析 Ollama 运行模型。
     *
     * @param node OCR 节点
     * @return Ollama 模型名
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private String model(OcrNode node) {
        return node.providerModel().orElse(DEFAULT_MODEL);
    }

    /**
     * 判断 HTTP 响应和 Ollama JSON 是否成功。
     *
     * @param response HTTP 响应
     * @return 是否成功
     * @throws IOException JSON 解析失败
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private boolean isSuccess(HttpResponse<String> response) throws IOException {
        return isHttpSuccess(response.statusCode()) && isOllamaBodyParseable(response.body());
    }

    /**
     * 判断 HTTP 状态码是否成功。
     *
     * @param statusCode HTTP 状态码
     * @return 是否成功
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private boolean isHttpSuccess(int statusCode) {
        return statusCode >= HTTP_SUCCESS_MIN && statusCode < HTTP_SUCCESS_MAX;
    }

    /**
     * 判断 Ollama 响应体是否可解析。
     *
     * @param responseBody 响应体
     * @return 是否可解析
     * @throws IOException JSON 解析失败
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private boolean isOllamaBodyParseable(String responseBody) throws IOException {
        JsonNode body = objectMapper.readTree(responseBody);
        return !body.isMissingNode() && body.path(FIELD_ERROR).isMissingNode();
    }
}
