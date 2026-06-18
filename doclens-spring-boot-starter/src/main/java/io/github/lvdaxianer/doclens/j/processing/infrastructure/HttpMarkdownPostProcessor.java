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
 * OpenAI compatible HTTP LLM Markdown 后处理器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public class HttpMarkdownPostProcessor implements MarkdownPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpMarkdownPostProcessor.class);
    private static final String ROLE_SYSTEM = "system";
    private static final String ROLE_USER = "user";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String APPLICATION_JSON = "application/json";
    private static final String API_KEY_MASK = "***";
    private static final int ERROR_BODY_MAX_LENGTH = 500;
    private final ObjectMapper objectMapper;
    private final HttpMarkdownPostProcessorOptions options;
    private final HttpClient httpClient;

    /**
     * 创建 HTTP LLM Markdown 后处理器。
     *
     * @param objectMapper JSON 映射器
     * @param options HTTP Markdown 后处理配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public HttpMarkdownPostProcessor(
            ObjectMapper objectMapper,
            HttpMarkdownPostProcessorOptions options
    ) {
        this.objectMapper = objectMapper;
        this.options = options;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(options.timeout())
                .build();
    }

    /**
     * 执行 LLM Markdown 后处理。
     *
     * @param request Markdown 后处理请求
     * @return Markdown 后处理结果
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
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
            throw new IllegalStateException(buildFailureMessage("LLM Markdown request interrupted", ex), ex);
        } catch (IOException ex) {
            throw new IllegalStateException(buildFailureMessage("LLM Markdown request failed", ex), ex);
        }
    }

    /**
     * 构建 OpenAI compatible 请求体。
     *
     * @param request Markdown 后处理请求
     * @return JSON 请求体
     * @throws IOException JSON 序列化失败
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private String buildRequestBody(MarkdownPostProcessingRequest request) throws IOException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", options.model());
        body.set("messages", messages(request));
        return objectMapper.writeValueAsString(body);
    }

    /**
     * 构建 system 与 user 消息。
     *
     * @param request Markdown 后处理请求
     * @return messages 节点
     * @throws IOException 元数据序列化失败
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private ArrayNode messages(MarkdownPostProcessingRequest request) throws IOException {
        ArrayNode messages = objectMapper.createArrayNode();
        messages.add(message(ROLE_SYSTEM, MarkdownPrompt.systemPrompt()));
        messages.add(message(ROLE_USER, userPrompt(request)));
        return messages;
    }

    /**
     * 构建单条 chat message。
     *
     * @param role 消息角色
     * @param content 消息内容
     * @return chat message
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private ObjectNode message(String role, String content) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    /**
     * 构建用户提示词。
     *
     * @param request Markdown 后处理请求
     * @return 用户提示词
     * @throws IOException 元数据序列化失败
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private String userPrompt(MarkdownPostProcessingRequest request) throws IOException {
        return MarkdownPrompt.userPrompt(objectMapper, request);
    }

    /**
     * 构建 HTTP 请求。
     *
     * @param requestBody JSON 请求体
     * @return HTTP 请求
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private HttpRequest buildRequest(String requestBody) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(options.endpoint())
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(options.timeout())
                .header(HEADER_CONTENT_TYPE, APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody));
        if (!options.apiKey().isBlank()) {
            // API Key 只进入请求头，不进入响应、异常或持久化记录。
            builder.header(HEADER_AUTHORIZATION, "Bearer " + options.apiKey());
        } else {
            // 离线或内网模型允许无 API Key 调用。
        }
        return builder.build();
    }

    /**
     * 解析 OpenAI compatible 响应。
     *
     * @param response HTTP 响应
     * @return Markdown 后处理结果
     * @throws IOException JSON 解析失败
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private MarkdownPostProcessingResult parseResponse(HttpResponse<String> response) throws IOException {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            // HTTP 成功时只读取 choices[0].message.content 作为 Markdown。
            JsonNode responseJson = objectMapper.readTree(response.body());
            String markdown = responseJson.path("choices").path(0).path("message").path("content").asText("");
            return MarkdownPostProcessingResult.markdown(markdown);
        } else {
            // HTTP 失败时抛出已脱敏的供应商错误摘要，由核心层负责回退。
            throw new IllegalStateException(buildErrorMessage(response));
        }
    }

    /**
     * 记录脱敏后的 LLM 请求。
     *
     * @param request Markdown 后处理请求
     * @param requestBody 请求体
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private void logRequest(MarkdownPostProcessingRequest request, String requestBody) {
        LOGGER.debug("[第三方接口调用] 发起请求|LLMMarkdown|{}|POST|-|-|documentId={}, model={}, body={}",
                options.endpoint(), request.documentId(), options.model(), requestBody);
    }

    /**
     * 记录脱敏后的 LLM 响应。
     *
     * @param response HTTP 响应
     * @param startedAt 请求开始时间
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private void logResponse(HttpResponse<String> response, Instant startedAt) {
        long elapsedMillis = Duration.between(startedAt, Instant.now()).toMillis();
        LOGGER.debug("[第三方接口调用] 收到响应|LLMMarkdown|{}|{}|{}ms|body={}", options.endpoint(),
                response.statusCode(), elapsedMillis, summarizeBody(response.body()));
    }

    /**
     * 构建不包含 API Key 的错误消息。
     *
     * @param response HTTP 响应
     * @return 错误消息
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private String buildErrorMessage(HttpResponse<String> response) {
        return "LLM Markdown returned HTTP " + response.statusCode() + ": " + summarizeBody(response.body());
    }

    /**
     * 构建请求失败消息，保留最深层的根因摘要。
     *
     * @param prefix 外层失败前缀
     * @param failure 失败异常
     * @return 请求失败消息
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private String buildFailureMessage(String prefix, Throwable failure) {
        String detail = rootCauseMessage(failure);
        if (detail.isBlank()) {
            // 根因没有可读消息时保留原有前缀，避免返回空提示。
            return prefix;
        } else {
            // 根因有可读消息时把它拼入外层消息，便于上层直接展示和排查。
            return prefix + ": " + detail;
        }
    }

    /**
     * 提取异常链中最深层的可读消息。
     *
     * @param failure 失败异常
     * @return 根因消息
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private String rootCauseMessage(Throwable failure) {
        String message = "";
        Throwable current = failure;
        while (current != null) {
            String currentMessage = normalizeMessage(current.getMessage());
            if (!currentMessage.isBlank()) {
                message = currentMessage;
            }
            current = current.getCause();
        }
        return message;
    }

    /**
     * 规整异常消息文本。
     *
     * @param value 原始消息
     * @return 规整后的消息
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private String normalizeMessage(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 截断并脱敏响应体。
     *
     * @param responseBody 响应体
     * @return 响应体摘要
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private String summarizeBody(String responseBody) {
        String sanitizedBody = sanitizeBody(responseBody);
        if (sanitizedBody.length() <= ERROR_BODY_MAX_LENGTH) {
            // 响应体未超限时返回完整脱敏摘要。
            return sanitizedBody;
        } else {
            // 响应体超限时截断，避免日志和异常提示过长。
            return sanitizedBody.substring(0, ERROR_BODY_MAX_LENGTH);
        }
    }

    /**
     * 对供应商响应体中可能回显的 API Key 做脱敏。
     *
     * @param responseBody 响应体
     * @return 脱敏后的响应体
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private String sanitizeBody(String responseBody) {
        if (!options.apiKey().isBlank()) {
            // 存在 API Key 时替换供应商可能回显的原值。
            return responseBody.replace(options.apiKey(), API_KEY_MASK);
        } else {
            // 未配置 API Key 时保持响应摘要原样。
            return responseBody;
        }
    }

    /**
     * HTTP LLM Markdown 后处理配置。
     *
     * @param endpoint OpenAI compatible endpoint
     * @param model 模型名称
     * @param apiKey API Key，可为空
     * @param timeout HTTP 请求超时时间
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public record HttpMarkdownPostProcessorOptions(
            URI endpoint,
            String model,
            String apiKey,
            Duration timeout
    ) {

        /**
         * 规整可选 API Key。
         *
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        public HttpMarkdownPostProcessorOptions {
            apiKey = apiKey == null ? "" : apiKey;
        }
    }
}
