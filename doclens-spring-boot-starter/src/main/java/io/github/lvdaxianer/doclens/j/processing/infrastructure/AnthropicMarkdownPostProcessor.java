package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingRequest;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingResult;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessor;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Anthropic messages HTTP LLM Markdown 后处理器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public class AnthropicMarkdownPostProcessor implements MarkdownPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnthropicMarkdownPostProcessor.class);
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_API_KEY = "x-api-key";
    private static final String HEADER_ANTHROPIC_VERSION = "anthropic-version";
    private static final String APPLICATION_JSON = "application/json";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String API_KEY_MASK = "***";
    private static final int ERROR_BODY_MAX_LENGTH = 500;
    private static final int MAX_TOKENS = 4096;
    private static final String ROLE_USER = "user";
    private static final String CONTENT_TYPE_TEXT = "text";

    private final ObjectMapper objectMapper;
    private final AnthropicMarkdownPostProcessorOptions options;
    private final HttpClient httpClient;

    /**
     * 创建 Anthropic Markdown 后处理器。
     *
     * @param objectMapper JSON 映射器
     * @param options Anthropic 后处理配置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public AnthropicMarkdownPostProcessor(
            ObjectMapper objectMapper,
            AnthropicMarkdownPostProcessorOptions options
    ) {
        this.objectMapper = objectMapper;
        this.options = options;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(options.timeout())
                .build();
    }

    @Override
    public MarkdownPostProcessingResult process(MarkdownPostProcessingRequest request) {
        try {
            String requestBody = buildRequestBody(request);
            HttpRequest httpRequest = buildRequest(requestBody);
            Instant startedAt = Instant.now();
            logRequest(request, requestBody);
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            logResponse(response, startedAt);
            return parseResponse(response);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LLM Markdown request interrupted", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("LLM Markdown request failed", ex);
        }
    }

    /**
     * 构建 Anthropic messages 请求体。
     *
     * @param request Markdown 后处理请求
     * @return JSON 请求体
     * @throws IOException JSON 序列化失败
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private String buildRequestBody(MarkdownPostProcessingRequest request) throws IOException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", options.model());
        body.put("max_tokens", MAX_TOKENS);
        body.put("system", MarkdownPrompt.systemPrompt());
        body.set("messages", messages(request));
        return objectMapper.writeValueAsString(body);
    }

    /**
     * 构建 Anthropic user message。
     *
     * @param request Markdown 后处理请求
     * @return messages 节点
     * @throws IOException 元数据序列化失败
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ArrayNode messages(MarkdownPostProcessingRequest request) throws IOException {
        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", ROLE_USER);
        message.put("content", MarkdownPrompt.userPrompt(objectMapper, request));
        messages.add(message);
        return messages;
    }

    /**
     * 构建 HTTP 请求。
     *
     * @param requestBody JSON 请求体
     * @return HTTP 请求
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private HttpRequest buildRequest(String requestBody) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(options.endpoint())
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(options.timeout())
                .header(HEADER_CONTENT_TYPE, APPLICATION_JSON)
                .header(HEADER_ANTHROPIC_VERSION, ANTHROPIC_VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody));
        if (!options.apiKey().isBlank()) {
            builder.header(HEADER_API_KEY, options.apiKey());
        } else {
            // 离线或内网模型允许无 API Key 调用。
        }
        return builder.build();
    }

    /**
     * 解析 Anthropic 响应。
     *
     * @param response HTTP 响应
     * @return Markdown 后处理结果
     * @throws IOException JSON 解析失败
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private MarkdownPostProcessingResult parseResponse(HttpResponse<String> response) throws IOException {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonNode responseJson = objectMapper.readTree(response.body());
            String markdown = firstText(responseJson);
            return MarkdownPostProcessingResult.markdown(markdown);
        } else {
            throw new IllegalStateException(buildErrorMessage(response));
        }
    }

    /**
     * 读取第一段 text 内容。
     *
     * @param responseJson Anthropic 响应 JSON
     * @return Markdown 文本
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private String firstText(JsonNode responseJson) {
        for (JsonNode content : responseJson.path("content")) {
            if (CONTENT_TYPE_TEXT.equals(content.path("type").asText())) {
                return content.path("text").asText("");
            } else {
                // 非文本块不参与 Markdown 输出。
            }
        }
        return "";
    }

    /**
     * 记录脱敏后的 LLM 请求。
     *
     * @param request Markdown 后处理请求
     * @param requestBody 请求体
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void logRequest(MarkdownPostProcessingRequest request, String requestBody) {
        LOGGER.debug("[第三方接口调用] 发起请求|LLMMarkdownAnthropic|{}|POST|-|-|documentId={}, model={}, body={}",
                options.endpoint(), request.documentId(), options.model(), requestBody);
    }

    /**
     * 记录脱敏后的 LLM 响应。
     *
     * @param response HTTP 响应
     * @param startedAt 请求开始时间
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void logResponse(HttpResponse<String> response, Instant startedAt) {
        long elapsedMillis = Duration.between(startedAt, Instant.now()).toMillis();
        LOGGER.debug("[第三方接口调用] 收到响应|LLMMarkdownAnthropic|{}|{}|{}ms|body={}",
                options.endpoint(), response.statusCode(), elapsedMillis, summarizeBody(response.body()));
    }

    /**
     * 构建不包含 API Key 的错误消息。
     *
     * @param response HTTP 响应
     * @return 错误消息
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private String buildErrorMessage(HttpResponse<String> response) {
        return "LLM Markdown returned HTTP " + response.statusCode() + ": " + summarizeBody(response.body());
    }

    /**
     * 截断并脱敏响应体。
     *
     * @param responseBody 响应体
     * @return 响应体摘要
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private String summarizeBody(String responseBody) {
        String sanitizedBody = sanitizeBody(responseBody);
        if (sanitizedBody.length() <= ERROR_BODY_MAX_LENGTH) {
            return sanitizedBody;
        } else {
            return sanitizedBody.substring(0, ERROR_BODY_MAX_LENGTH);
        }
    }

    /**
     * 对响应体中可能回显的 API Key 做脱敏。
     *
     * @param responseBody 响应体
     * @return 脱敏后的响应体
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private String sanitizeBody(String responseBody) {
        if (!options.apiKey().isBlank()) {
            return responseBody.replace(options.apiKey(), API_KEY_MASK);
        } else {
            return responseBody;
        }
    }

    /**
     * Anthropic LLM Markdown 后处理配置。
     *
     * @param endpoint Anthropic messages endpoint
     * @param model 模型名称
     * @param apiKey API Key，可为空
     * @param timeout HTTP 请求超时时间
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public record AnthropicMarkdownPostProcessorOptions(
            URI endpoint,
            String model,
            String apiKey,
            Duration timeout
    ) {

        /**
         * 规整可选 API Key。
         *
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        public AnthropicMarkdownPostProcessorOptions {
            apiKey = apiKey == null ? "" : apiKey;
        }
    }
}
