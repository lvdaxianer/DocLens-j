package io.github.lvdaxianer.doclens.j.shared.web;

import io.github.lvdaxianer.doclens.j.shared.domain.DuplicateResourceException;
import io.github.lvdaxianer.doclens.j.shared.domain.ResourceNotFoundException;
import io.github.lvdaxianer.doclens.j.ingestion.infrastructure.CallerCredentialException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * 将应用异常映射为稳定的 API 错误响应。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String CODE_KEY = "code";
    private static final String DETAIL_KEY = "detail";
    private static final String BAD_REQUEST_CODE = "VALIDATION_FAILED";
    private static final String MISSING_PARTITION_CODE = "MISSING_PARTITION";
    private static final String TRUSTED_GATEWAY_UNAUTHORIZED_CODE = "TRUSTED_GATEWAY_UNAUTHORIZED";
    private static final String PARTITION_FORBIDDEN_CODE = "PARTITION_FORBIDDEN";
    private static final String RATE_LIMITED_CODE = "RATE_LIMITED";
    private static final String GLOBAL_PROTECTION_CODE = "GLOBAL_PROTECTION";
    private static final String PAYLOAD_TOO_LARGE_CODE = "PAYLOAD_TOO_LARGE";
    private static final String NOT_FOUND_CODE = "NOT_FOUND";
    private static final String CONFLICT_CODE = "CONFLICT";
    private static final String INTERNAL_ERROR_CODE = "INTERNAL_ERROR";
    private static final String BAD_REQUEST_FALLBACK_DETAIL = "请求参数不合法";
    private static final String ERROR_FALLBACK_DETAIL = "请求处理失败";
    private static final String INTERNAL_ERROR_DETAIL = "服务器暂时不可用，请稍后重试";
    private static final String UPLOAD_SIZE_LIMIT_DETAIL = "上传文件总大小不能超过 500MB，请拆分后再上传";

    /**
     * 处理参数校验失败。
     *
     * @param ex 校验异常
     * @return 错误响应
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentNotValidException.class,
            MissingServletRequestPartException.class
    })
    public ResponseEntity<Map<String, String>> handleBadRequest(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(body(BAD_REQUEST_CODE, detail(ex, BAD_REQUEST_FALLBACK_DETAIL)));
    }

    /**
     * 处理 caller 分区键失败。
     *
     * @param ex caller 分区键异常
     * @param request HTTP 请求
     * @return 错误响应
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @ExceptionHandler(CallerCredentialException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(
            CallerCredentialException ex,
            HttpServletRequest request
    ) {
        log.warn("[调用方分区] 未授权, 分区键缺失或无效, path={}, detail={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(body(MISSING_PARTITION_CODE, detail(ex, ERROR_FALLBACK_DETAIL)));
    }

    /**
     * 处理可信网关认证失败。
     *
     * @param ex 可信网关异常
     * @param request HTTP 请求
     * @return 错误响应
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @ExceptionHandler(TrustedGatewayException.class)
    public ResponseEntity<Map<String, String>> handleTrustedGatewayUnauthorized(
            TrustedGatewayException ex,
            HttpServletRequest request
    ) {
        log.warn("[可信网关] 请求未通过可信网关认证, path={}, detail={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(body(TRUSTED_GATEWAY_UNAUTHORIZED_CODE, detail(ex, ERROR_FALLBACK_DETAIL)));
    }

    /**
     * 处理可信网关 principal 授权失败。
     *
     * @param ex 可信网关授权异常
     * @param request HTTP 请求
     * @return 错误响应
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @ExceptionHandler(TrustedGatewayAuthorizationException.class)
    public ResponseEntity<Map<String, String>> handleTrustedGatewayForbidden(
            TrustedGatewayAuthorizationException ex,
            HttpServletRequest request
    ) {
        log.warn("[可信网关授权] 请求未通过 principal 分区授权, path={}, detail={}",
                request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(body(PARTITION_FORBIDDEN_CODE, detail(ex, ERROR_FALLBACK_DETAIL)));
    }

    /**
     * 处理调用方接口组限流超额。
     *
     * @param ex 限流异常
     * @return 错误响应
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, String>> handleTooManyRequests(
            RateLimitExceededException ex,
            HttpServletRequest request
    ) {
        log.warn("[调用方流量] 接口组超限, path={}, group={}, limit={}, remaining={}, retryAfterSeconds={}",
                request.getRequestURI(), ex.trafficGroup(), ex.limit(), ex.remaining(), ex.retryAfterSeconds());
        HttpHeaders headers = new HttpHeaders();
        headers.add("Retry-After", String.valueOf(ex.retryAfterSeconds()));
        headers.add("X-DocLens-Traffic-Group", ex.trafficGroup());
        headers.add("X-DocLens-RateLimit-Limit", String.valueOf(ex.limit()));
        headers.add("X-DocLens-RateLimit-Remaining", String.valueOf(ex.remaining()));
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).headers(headers)
                .body(body(RATE_LIMITED_CODE, detail(ex, ERROR_FALLBACK_DETAIL)));
    }

    /**
     * 处理全局保护超限。
     *
     * @param ex 全局保护异常
     * @return 错误响应
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @ExceptionHandler(GlobalProtectionExceededException.class)
    public ResponseEntity<Map<String, String>> handleServiceUnavailable(
            GlobalProtectionExceededException ex,
            HttpServletRequest request
    ) {
        log.warn("[流量保护] 全局保护已触发, path={}, detail={}", request.getRequestURI(), ex.getMessage());
        HttpHeaders headers = new HttpHeaders();
        headers.add(GlobalProtectionInterceptor.headerName(), "enabled");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).headers(headers)
                .body(body(GLOBAL_PROTECTION_CODE, detail(ex, ERROR_FALLBACK_DETAIL)));
    }

    /**
     * 处理上传内容超过服务端限制。
     *
     * @param ex 上传大小异常
     * @param request HTTP 请求
     * @return 错误响应
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handlePayloadTooLarge(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request
    ) {
        log.warn("[上传大小] 上传大小超限, path={}, detail={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(body(PAYLOAD_TOO_LARGE_CODE, UPLOAD_SIZE_LIMIT_DETAIL));
    }

    /**
     * 处理资源不存在异常。
     *
     * @param ex 资源不存在异常
     * @return 错误响应
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body(NOT_FOUND_CODE, detail(ex, ERROR_FALLBACK_DETAIL)));
    }

    /**
     * 处理资源重复异常。
     *
     * @param ex 资源重复异常
     * @return 错误响应
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, String>> handleConflict(DuplicateResourceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body(CONFLICT_CODE, detail(ex, ERROR_FALLBACK_DETAIL)));
    }

    /**
     * 处理未预期异常。
     *
     * @param ex 未预期异常
     * @param request HTTP 请求
     * @return 错误响应
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleInternalError(RuntimeException ex, HttpServletRequest request) {
        log.error("[全局异常] 未预期异常, path={}, exceptionType={}",
                request.getRequestURI(), ex.getClass().getName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(INTERNAL_ERROR_CODE, INTERNAL_ERROR_DETAIL));
    }

    /**
     * 创建稳定错误响应体。
     *
     * @param code 稳定错误码
     * @param detail 错误详情
     * @return 错误响应体
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private Map<String, String> body(String code, String detail) {
        return Map.of(CODE_KEY, code, DETAIL_KEY, detail);
    }

    /**
     * 读取非空错误详情。
     *
     * @param ex 异常对象
     * @param fallbackDetail 兜底错误详情
     * @return 非空错误详情
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private String detail(Exception ex, String fallbackDetail) {
        return Optional.ofNullable(ex.getMessage()).orElse(fallbackDetail);
    }
}
