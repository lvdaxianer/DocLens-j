package io.github.lvdaxianer.doclens.j.ingestion.infrastructure;

/**
 * caller 分区键异常。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
public class CallerCredentialException extends RuntimeException {

    /**
     * 创建 caller 分区键异常。
     *
     * @param message 异常消息
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public CallerCredentialException(String message) {
        super(message);
    }
}
