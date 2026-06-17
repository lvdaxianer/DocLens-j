package io.github.lvdaxianer.doclens.j.shared.web;

/**
 * 全局保护超限异常。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
public class GlobalProtectionExceededException extends RuntimeException {

    /**
     * 创建全局保护超限异常。
     *
     * @param message 异常消息
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public GlobalProtectionExceededException(String message) {
        super(message);
    }
}
