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
 * Maps application exceptions to stable API error responses.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation failures.
     *
     * @param ex validation exception
     * @return error response
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
     * Handles missing resources.
     *
     * @param ex not-found exception
     * @return error response
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", ex.getMessage()));
    }

    /**
     * Handles duplicate resources.
     *
     * @param ex duplicate exception
     * @return error response
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, String>> handleConflict(DuplicateResourceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("detail", ex.getMessage()));
    }
}
