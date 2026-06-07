package io.github.lvdaxianer.doclens.j.shared.web;

import io.github.lvdaxianer.doclens.j.shared.domain.DuplicateResourceException;
import io.github.lvdaxianer.doclens.j.shared.domain.ResourceNotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * 将应用异常映射为稳定的 API 错误响应。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

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
