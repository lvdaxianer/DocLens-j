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

    /*
     * 稳定错误响应字段约定：
     * - code 用于前端判断错误类型
     * - detail 用于前端展示可读说明
     * - HTTP status 仍然保留协议语义
     * - 响应体不能只依赖自然语言 detail
     * - 自然语言 detail 允许后续调整文案
     * - 稳定 code 需要兼容前端恢复动作
     * - 测试只断言当前任务涉及的全局 handler
     * - Open WebUI 适配器保留自己的契约字段
     * - 幂等键查询保留旧 code/message/data 包装
     * - 后续前端 F2 会基于这些 code 做解析
     */
    private static final String CODE_KEY = "code";
    private static final String DETAIL_KEY = "detail";
    /*
     * 可恢复错误码分类：
     * - VALIDATION_FAILED 对应 400 输入问题
     * - MISSING_PARTITION 对应缺失分区键
     * - TRUSTED_GATEWAY_UNAUTHORIZED 对应网关证明或 principal 缺失
     * - PARTITION_FORBIDDEN 对应 principal 无权访问分区
     * - RATE_LIMITED 对应 caller 维度限流
     * - GLOBAL_PROTECTION 对应服务全局保护触发
     * - PAYLOAD_TOO_LARGE 对应上传体积超过限制
     * - INTERNAL_ERROR 对应未预期服务端失败
     * - 这些常量与生产代码保持同名，方便定位变更
     */
    private static final String BAD_REQUEST_CODE = "VALIDATION_FAILED";
    private static final String MISSING_PARTITION_CODE = "MISSING_PARTITION";
    private static final String TRUSTED_GATEWAY_UNAUTHORIZED_CODE = "TRUSTED_GATEWAY_UNAUTHORIZED";
    private static final String PARTITION_FORBIDDEN_CODE = "PARTITION_FORBIDDEN";
    private static final String RATE_LIMITED_CODE = "RATE_LIMITED";
    private static final String GLOBAL_PROTECTION_CODE = "GLOBAL_PROTECTION";
    private static final String PAYLOAD_TOO_LARGE_CODE = "PAYLOAD_TOO_LARGE";
    private static final String INTERNAL_ERROR_CODE = "INTERNAL_ERROR";
    private static final String INTERNAL_ERROR_DETAIL = "服务器暂时不可用，请稍后重试";
    /*
     * 敏感信息断言约定：
     * - 请求头里的密钥只用于验证日志脱敏
     * - 错误响应不能回显该密钥
     * - 500 兜底日志不能记录异常 message
     * - 429/503 日志只保留定位用的接口组和路径
     * - 401/403 日志只记录认证授权语义
     * - 上传大小日志只记录限制信息
     * - 这些测试防止后续为排障把 secret 打进日志
     * - MAX_UPLOAD_BYTES 与服务端 500MB 限制保持一致
     * - request() helper 会统一给每个场景塞入敏感头
     */
    private static final String SECRET_API_KEY = "secret-api-key";
    private static final long MAX_UPLOAD_BYTES = 524288000L;

    /**
     * 400 响应应返回稳定错误码，便于前端识别校验失败。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Test
    void badRequestReturnsStableErrorCode() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<Map<String, String>> response = handler.handleBadRequest(
                new IllegalArgumentException("invalid request"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry(CODE_KEY, BAD_REQUEST_CODE);
        assertThat(response.getBody()).containsEntry(DETAIL_KEY, "invalid request");
    }

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
        assertThat(response.getBody()).containsEntry(CODE_KEY, MISSING_PARTITION_CODE);
        assertThat(response.getBody()).containsEntry(DETAIL_KEY, "unauthorized caller credential");
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
        assertThat(response.getBody()).containsEntry(CODE_KEY, TRUSTED_GATEWAY_UNAUTHORIZED_CODE);
        assertThat(response.getBody()).containsEntry(DETAIL_KEY, "trusted principal is missing");
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
        assertThat(response.getBody()).containsEntry(CODE_KEY, PARTITION_FORBIDDEN_CODE);
        assertThat(response.getBody()).containsEntry(DETAIL_KEY, "trusted principal is not allowed for partition");
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
        assertThat(response.getBody()).containsEntry(CODE_KEY, RATE_LIMITED_CODE);
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
        assertThat(response.getBody()).containsEntry(CODE_KEY, GLOBAL_PROTECTION_CODE);
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
        assertThat(response.getBody()).containsEntry(CODE_KEY, PAYLOAD_TOO_LARGE_CODE);
        assertThat(response.getBody()).containsEntry(DETAIL_KEY, "上传文件总大小不能超过 500MB，请拆分后再上传");
        assertThat(output.getOut()).contains("/api/v1/batches").contains("上传大小超限");
        assertThat(output.getOut()).doesNotContain(SECRET_API_KEY);
    }

    /**
     * 500 响应应返回稳定错误码，且不得暴露内部异常细节。
     *
     * @param output 控制台捕获输出
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Test
    void unexpectedErrorReturnsStablePayload(CapturedOutput output) {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = request("/api/v1/dashboard/summary");

        ResponseEntity<Map<String, String>> response = handler.handleInternalError(
                new IllegalStateException("database password=secret-api-key failed"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).containsEntry(CODE_KEY, INTERNAL_ERROR_CODE);
        assertThat(response.getBody()).containsEntry(DETAIL_KEY, INTERNAL_ERROR_DETAIL);
        assertThat(output.getOut()).contains("/api/v1/dashboard/summary").contains("未预期异常");
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
