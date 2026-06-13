package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.EnvironmentCredentialResolver;
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
 * DashScope OpenAI compatible 在线 OCR 客户端。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public class DashScopeOnlineOcrClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashScopeOnlineOcrClient.class);
    public static final URI DEFAULT_ENDPOINT = URI.create(
            "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions");

    private final URI endpoint;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final DashScopeOcrPayloadFactory payloadFactory;
    private final DashScopeOcrResponseMapper responseMapper;
    private final DashScopeOcrResponseSanitizer responseSanitizer;
    private final EnvironmentCredentialResolver credentialResolver;

    /**
     * 创建 DashScope compatible 在线 OCR 客户端。
     *
     * @param objectMapper JSON 映射器
     * @param endpoint 在线 OCR endpoint
     * @param timeout HTTP 请求超时时间
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public DashScopeOnlineOcrClient(ObjectMapper objectMapper, URI endpoint, Duration timeout) {
        this(new Options(objectMapper, endpoint, timeout, new EnvironmentCredentialResolver()));
    }

    /**
     * 创建可注入凭证解析器的 DashScope compatible 在线 OCR 客户端。
     *
     * @param options 客户端配置
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    DashScopeOnlineOcrClient(Options options) {
        this.endpoint = options.endpoint();
        this.timeout = options.timeout();
        this.payloadFactory = new DashScopeOcrPayloadFactory(options.objectMapper());
        this.responseMapper = new DashScopeOcrResponseMapper(options.objectMapper());
        this.responseSanitizer = new DashScopeOcrResponseSanitizer();
        this.credentialResolver = options.credentialResolver();
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(options.timeout())
                .build();
    }

    /**
     * 通过 DashScope compatible API 识别单张图片。
     *
     * @param node OCR 运行时节点
     * @param request 图片 OCR 请求
     * @return 图片 OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public ImageOcrResult recognizeImage(OcrRuntimeNode node, ImageOcrRequest request) {
        try {
            String credential = resolveCredential(node);
            String requestBody = payloadFactory.buildRequestBody(node, request);
            HttpRequest httpRequest = buildRequest(requestBody, credential);
            Instant startedAt = Instant.now();
            logRequest(node, request);
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            logResponse(new DashScopeResponseLogContext(node, response, startedAt, credential));
            return parseResponse(request, response, credential);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("DashScope online OCR request interrupted", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("DashScope online OCR request failed", ex);
        }
    }

    /**
     * 探测在线 OCR 节点是否具备真实执行权限。
     *
     * @param node OCR 运行时节点
     * @return 是否具备执行权限
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    boolean hasExecutionPermission(OcrRuntimeNode node) {
        try {
            String credential = resolveCredential(node);
            HttpResponse<String> response = sendPermissionProbe(node, credential);
            return handlePermissionProbeResponse(node, response, credential);
        } catch (IllegalStateException ex) {
            logPermissionProbeFailure(node, ex);
            return false;
        } catch (IOException ex) {
            logPermissionProbeFailure(node, ex);
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logPermissionProbeFailure(node, ex);
            return false;
        }
    }

    /**
     * 发送在线 OCR 权限探测请求。
     *
     * @param node OCR 运行时节点
     * @param credential 真实 API Key
     * @return 权限探测响应
     * @throws IOException HTTP 请求失败
     * @throws InterruptedException 请求线程被中断
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private HttpResponse<String> sendPermissionProbe(OcrRuntimeNode node, String credential)
            throws IOException, InterruptedException {
        String requestBody = payloadFactory.buildRequestBody(node, payloadFactory.permissionProbeRequest());
        HttpRequest httpRequest = buildRequest(requestBody, credential);
        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * 处理在线 OCR 权限探测响应。
     *
     * @param node OCR 运行时节点
     * @param response HTTP 响应
     * @param credential 真实 API Key
     * @return 是否具备执行权限
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private boolean handlePermissionProbeResponse(
            OcrRuntimeNode node,
            HttpResponse<String> response,
            String credential
    ) {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            // 2xx 响应说明在线 OCR 账号具备实际调用权限。
            return true;
        } else {
            // 非 2xx 响应需要记录脱敏摘要，供节点恢复诊断使用。
            logPermissionDenied(node, response, credential);
            return false;
        }
    }

    /**
     * 记录在线 OCR 权限不足响应。
     *
     * @param node OCR 运行时节点
     * @param response HTTP 响应
     * @param credential 真实 API Key
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private void logPermissionDenied(OcrRuntimeNode node, HttpResponse<String> response, String credential) {
        LOGGER.warn("[第三方接口调用] 在线 OCR 权限探测失败|DashScopeOCR|{}|{}|nodeId={}, model={}, body={}",
                endpoint, response.statusCode(), node.node().id(), payloadFactory.providerModel(node),
                responseSanitizer.summarizeBody(response.body(), credential));
    }

    /**
     * 记录在线 OCR 权限探测异常。
     *
     * @param node OCR 运行时节点
     * @param ex 异常
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private void logPermissionProbeFailure(OcrRuntimeNode node, Exception ex) {
        LOGGER.warn("[第三方接口调用] 在线 OCR 权限探测失败|DashScopeOCR|{}|-|nodeId={}, error={}",
                endpoint, node.node().id(), ex.getMessage(), ex);
    }

    /**
     * 构建在线 OCR HTTP 请求。
     *
     * @param requestBody JSON 请求体
     * @param credential 真实 API Key
     * @return HTTP 请求
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private HttpRequest buildRequest(String requestBody, String credential) {
        return HttpRequest.newBuilder(endpoint)
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(timeout)
                .header("Authorization", "Bearer " + credential)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    /**
     * 解析 DashScope compatible 响应。
     *
     * @param request 图片 OCR 请求
     * @param response HTTP 响应
     * @param credential 真实 API Key
     * @return 图片 OCR 结果
     * @throws IOException JSON 解析失败
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private ImageOcrResult parseResponse(
            ImageOcrRequest request,
            HttpResponse<String> response,
            String credential
    ) throws IOException {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            // 成功响应统一转为领域 OCR 结果，屏蔽供应商响应结构。
            return responseMapper.map(request, response.body());
        } else {
            // 失败响应只暴露脱敏摘要，避免错误消息泄露凭证。
            throw new IllegalStateException(buildErrorMessage(response, credential));
        }
    }

    /**
     * 从节点环境变量引用中解析真实 API Key。
     *
     * @param node OCR 运行时节点
     * @return 真实 API Key
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private String resolveCredential(OcrRuntimeNode node) {
        String credentialEnvVar = node.node().credentialRef()
                .orElseThrow(() -> new IllegalStateException("online OCR credential is not configured"));
        return credentialResolver.resolve(credentialEnvVar);
    }

    /**
     * 记录脱敏后的在线 OCR 请求摘要。
     *
     * @param node OCR 运行时节点
     * @param request 图片 OCR 请求
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private void logRequest(OcrRuntimeNode node, ImageOcrRequest request) {
        LOGGER.debug("[第三方接口调用] 发起请求|DashScopeOCR|{}|POST|-|-|nodeId={}, model={}, fileBytes={}",
                endpoint, node.node().id(), payloadFactory.providerModel(node), request.imageContent().length);
    }

    /**
     * 记录脱敏后的在线 OCR 响应摘要。
     *
     * @param context 响应日志上下文
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private void logResponse(DashScopeResponseLogContext context) {
        long elapsedMillis = Duration.between(context.startedAt(), Instant.now()).toMillis();
        LOGGER.debug("[第三方接口调用] 收到响应|DashScopeOCR|{}|{}|{}ms|nodeId={}, model={}, body={}",
                endpoint, context.response().statusCode(), elapsedMillis, context.node().node().id(),
                payloadFactory.providerModel(context.node()),
                responseSanitizer.summarizeBody(context.response().body(), context.credential()));
    }

    /**
     * 构建不包含请求密钥的错误消息。
     *
     * @param response HTTP 响应
     * @param credential 真实 API Key
     * @return 错误消息
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private String buildErrorMessage(HttpResponse<String> response, String credential) {
        return "DashScope online OCR returned HTTP " + response.statusCode() + ": "
                + responseSanitizer.summarizeBody(response.body(), credential);
    }

    /**
     * DashScope 在线 OCR 客户端配置。
     *
     * @param objectMapper JSON 映射器
     * @param endpoint 在线 OCR endpoint
     * @param timeout HTTP 请求超时时间
     * @param credentialResolver 环境变量凭证解析器
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    record Options(
            ObjectMapper objectMapper,
            URI endpoint,
            Duration timeout,
            EnvironmentCredentialResolver credentialResolver
    ) {
    }

    /**
     * DashScope 响应日志上下文。
     *
     * @param node OCR 运行时节点
     * @param response HTTP 响应
     * @param startedAt 请求开始时间
     * @param credential 真实 API Key，仅用于响应体脱敏
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private record DashScopeResponseLogContext(
            OcrRuntimeNode node,
            HttpResponse<String> response,
            Instant startedAt,
            String credential
    ) {
    }
}
