package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
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
        this.endpoint = endpoint;
        this.timeout = timeout;
        this.payloadFactory = new DashScopeOcrPayloadFactory(objectMapper);
        this.responseMapper = new DashScopeOcrResponseMapper(objectMapper);
        this.responseSanitizer = new DashScopeOcrResponseSanitizer();
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(timeout)
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
            String requestBody = payloadFactory.buildRequestBody(node, request);
            HttpRequest httpRequest = buildRequest(node, requestBody);
            Instant startedAt = Instant.now();
            logRequest(node, request);
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            logResponse(node, response, startedAt);
            return parseResponse(node, request, response);
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
            String requestBody = payloadFactory.buildRequestBody(node, payloadFactory.permissionProbeRequest());
            HttpRequest httpRequest = buildRequest(node, requestBody);
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                // 2xx 响应说明在线 OCR 账号具备实际调用权限。
                return true;
            } else {
                // 非 2xx 响应需要记录脱敏摘要，供节点恢复诊断使用。
                LOGGER.warn("[第三方接口调用] 在线 OCR 权限探测失败|DashScopeOCR|{}|{}|nodeId={}, model={}, body={}",
                        endpoint, response.statusCode(), node.node().id(), payloadFactory.providerModel(node),
                        responseSanitizer.summarizeBody(node, response.body()));
                return false;
            }
        } catch (IllegalStateException ex) {
            LOGGER.warn("[第三方接口调用] 在线 OCR 权限探测失败|DashScopeOCR|{}|-|nodeId={}, error={}",
                    endpoint, node.node().id(), ex.getMessage());
            return false;
        } catch (IOException ex) {
            LOGGER.warn("[第三方接口调用] 在线 OCR 权限探测失败|DashScopeOCR|{}|-|nodeId={}, error={}",
                    endpoint, node.node().id(), ex.getMessage(), ex);
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.warn("[第三方接口调用] 在线 OCR 权限探测失败|DashScopeOCR|{}|-|nodeId={}, error={}",
                    endpoint, node.node().id(), ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * 构建在线 OCR HTTP 请求。
     *
     * @param node OCR 运行时节点
     * @param requestBody JSON 请求体
     * @return HTTP 请求
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private HttpRequest buildRequest(OcrRuntimeNode node, String requestBody) {
        String credential = node.node().credentialRef()
                .orElseThrow(() -> new IllegalStateException("online OCR credential is not configured"));
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
     * @param node OCR 运行时节点
     * @param request 图片 OCR 请求
     * @param response HTTP 响应
     * @return 图片 OCR 结果
     * @throws IOException JSON 解析失败
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private ImageOcrResult parseResponse(
            OcrRuntimeNode node,
            ImageOcrRequest request,
            HttpResponse<String> response
    ) throws IOException {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            // 成功响应统一转为领域 OCR 结果，屏蔽供应商响应结构。
            return responseMapper.map(request, response.body());
        } else {
            // 失败响应只暴露脱敏摘要，避免错误消息泄露凭证。
            throw new IllegalStateException(buildErrorMessage(node, response));
        }
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
     * @param node OCR 运行时节点
     * @param response HTTP 响应
     * @param startedAt 请求开始时间
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private void logResponse(OcrRuntimeNode node, HttpResponse<String> response, Instant startedAt) {
        long elapsedMillis = Duration.between(startedAt, Instant.now()).toMillis();
        LOGGER.debug("[第三方接口调用] 收到响应|DashScopeOCR|{}|{}|{}ms|nodeId={}, model={}, body={}",
                endpoint, response.statusCode(), elapsedMillis, node.node().id(), payloadFactory.providerModel(node),
                responseSanitizer.summarizeBody(node, response.body()));
    }

    /**
     * 构建不包含请求密钥的错误消息。
     *
     * @param node OCR 运行时节点
     * @param response HTTP 响应
     * @return 错误消息
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private String buildErrorMessage(OcrRuntimeNode node, HttpResponse<String> response) {
        return "DashScope online OCR returned HTTP " + response.statusCode() + ": "
                + responseSanitizer.summarizeBody(node, response.body());
    }
}
