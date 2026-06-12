package io.github.lvdaxianer.doclens.j.integration.interfaces;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Open WebUI 集成适配器异常处理器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
@RestControllerAdvice(assignableTypes = {
        OpenWebuiOcrIntegrationController.class,
        OpenWebuiOcrQueryIntegrationController.class,
        OpenWebuiOcrDocumentIntegrationController.class,
        OpenWebuiOcrHealthIntegrationController.class
})
public class OpenWebuiIntegrationExceptionHandler {

    private static final String CODE_FIELD = "code";
    private static final String MESSAGE_FIELD = "message";
    private static final String DETAILS_FIELD = "details";

    /**
     * 处理 Open WebUI 适配器契约异常。
     *
     * @param ex Open WebUI 适配器异常
     * @return 契约错误响应
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @ExceptionHandler(OpenWebuiIntegrationException.class)
    public ResponseEntity<Map<String, Object>> handleOpenWebuiException(OpenWebuiIntegrationException ex) {
        return ResponseEntity.status(ex.status()).body(Map.of(
                CODE_FIELD, ex.code(),
                MESSAGE_FIELD, ex.getMessage(),
                DETAILS_FIELD, ex.details()
        ));
    }
}
