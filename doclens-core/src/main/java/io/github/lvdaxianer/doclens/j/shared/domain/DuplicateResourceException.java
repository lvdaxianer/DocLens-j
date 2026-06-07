package io.github.lvdaxianer.doclens.j.shared.domain;

/**
 * 违反唯一性约束时抛出的异常。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public class DuplicateResourceException extends RuntimeException {

    /**
     * 创建资源重复异常。
     *
     * @param message 异常消息
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public DuplicateResourceException(String message) {
        super(message);
    }
}
