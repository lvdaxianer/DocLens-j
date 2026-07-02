package io.github.lvdaxianer.doclens.j.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.ingestion.infrastructure.CallerCredentialException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常映射与安全日志测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    private static final String SECRET_API_KEY = "secret-api-key";
    private static final long MAX_UPLOAD_BYTES = 524288000L;

    /**
     * 401 日志应保留错误语义，但不得泄露请求凭证。
     *
     * @param output 控制台捕获输出
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void unauthorizedLogsDoNotLeakCredential(CapturedOutput output) {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = request("/api/v1/batches");

        ResponseEntity<Map<String, String>> response = handler.handleUnauthorized(
                new CallerCredentialException("unauthorized caller credential"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).containsEntry("detail", "unauthorized caller credential");
        assertThat(output.getOut()).contains("/api/v1/batches").contains("未授权");
        assertThat(output.getOut()).doesNotContain(SECRET_API_KEY);
    }

    /**
     * 可信网关认证失败应映射为 401 且不得泄露网关证明。
     *
     * @param output 控制台捕获输出
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Test
    void trustedGatewayFailureReturnsUnauthorized(CapturedOutput output) {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = request("/api/v1/batches");

        ResponseEntity<Map<String, String>> response = handler.handleTrustedGatewayUnauthorized(
                new TrustedGatewayException("trusted principal is missing"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).containsEntry("detail", "trusted principal is missing");
        assertThat(output.getOut()).contains("/api/v1/batches").contains("可信网关");
        assertThat(output.getOut()).doesNotContain(SECRET_API_KEY);
    }

    /**
     * principal 分区授权失败应映射为 403 且不得泄露网关证明。
     *
     * @param output 控制台捕获输出
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Test
    void trustedGatewayAuthorizationFailureReturnsForbidden(CapturedOutput output) {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = request("/api/v1/batches");

        ResponseEntity<Map<String, String>> response = handler.handleTrustedGatewayForbidden(
                new TrustedGatewayAuthorizationException("trusted principal is not allowed for partition"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).containsEntry("detail", "trusted principal is not allowed for partition");
        assertThat(output.getOut()).contains("/api/v1/batches").contains("可信网关授权");
        assertThat(output.getOut()).doesNotContain(SECRET_API_KEY);
    }

    /**
     * 429 日志应输出接口组语义，但不得泄露请求凭证。
     *
     * @param output 控制台捕获输出
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void rateLimitLogsDoNotLeakCredential(CapturedOutput output) {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = request("/api/v1/dashboard/summary");

        ResponseEntity<Map<String, String>> response = handler.handleTooManyRequests(
                new RateLimitExceededException("dashboard-read", 2, 0, 1, "caller traffic limit exceeded"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(429);
        assertThat(response.getHeaders().getFirst("X-DocLens-Traffic-Group")).isEqualTo("dashboard-read");
        assertThat(response.getHeaders().getFirst("X-DocLens-RateLimit-Limit")).isEqualTo("2");
        assertThat(response.getHeaders().getFirst("X-DocLens-RateLimit-Remaining")).isEqualTo("0");
        assertThat(output.getOut()).contains("dashboard-read").contains("/api/v1/dashboard/summary");
        assertThat(output.getOut()).doesNotContain(SECRET_API_KEY);
    }

    /**
     * 503 日志应输出保护状态，但不得泄露请求凭证。
     *
     * @param output 控制台捕获输出
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void globalProtectionLogsDoNotLeakCredential(CapturedOutput output) {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = request("/api/v1/health");

        ResponseEntity<Map<String, String>> response = handler.handleServiceUnavailable(
                new GlobalProtectionExceededException("global protection limit exceeded"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getHeaders().getFirst("X-DocLens-Global-Protection")).isEqualTo("enabled");
        assertThat(output.getOut()).contains("/api/v1/health").contains("全局保护");
        assertThat(output.getOut()).doesNotContain(SECRET_API_KEY);
    }

    /**
     * 413 响应应返回结构化 detail，便于前端展示友好上传限制文案。
     *
     * @param output 控制台捕获输出
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void oversizedUploadReturnsStructuredPayload(CapturedOutput output) {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = request("/api/v1/batches");

        ResponseEntity<Map<String, String>> response = handler.handlePayloadTooLarge(
                new MaxUploadSizeExceededException(MAX_UPLOAD_BYTES), request);

        assertThat(response.getStatusCode().value()).isEqualTo(413);
        assertThat(response.getBody()).containsEntry("detail", "上传文件总大小不能超过 500MB，请拆分后再上传");
        assertThat(output.getOut()).contains("/api/v1/batches").contains("上传大小超限");
        assertThat(output.getOut()).doesNotContain(SECRET_API_KEY);
    }

    /**
     * 创建带敏感请求头的测试请求。
     *
     * @param uri 请求路径
     * @return 测试请求
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        request.addHeader("X-DocLens-Api-Key", SECRET_API_KEY);
        return request;
    }
}
