package io.github.lvdaxianer.doclens.j.adapter.application;

/**
 * OCR 路由执行异常。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public class OcrRouteExecutionException extends RuntimeException {

    /**
     * 创建 OCR 路由执行异常。
     *
     * @param message 异常消息
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public OcrRouteExecutionException(String message) {
        super(message);
    }

    /**
     * 创建带原因的 OCR 路由执行异常。
     *
     * @param message 异常消息
     * @param cause 原始异常
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public OcrRouteExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
