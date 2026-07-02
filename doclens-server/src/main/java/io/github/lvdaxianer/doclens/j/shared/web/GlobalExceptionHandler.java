package io.github.lvdaxianer.doclens.j.shared.web;

import io.github.lvdaxianer.doclens.j.shared.domain.DuplicateResourceException;
import io.github.lvdaxianer.doclens.j.shared.domain.ResourceNotFoundException;
import io.github.lvdaxianer.doclens.j.ingestion.infrastructure.CallerCredentialException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
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
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("detail", ex.getMessage()));
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
        log.warn("[调用方分区] 分区键缺失或无效, path={}, detail={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("detail", ex.getMessage()));
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
                .body(Map.of("detail", ex.getMessage()));
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
                .body(Map.of("detail", ex.getMessage()));
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
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("detail", UPLOAD_SIZE_LIMIT_DETAIL));
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
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", ex.getMessage()));
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
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("detail", ex.getMessage()));
    }
}
