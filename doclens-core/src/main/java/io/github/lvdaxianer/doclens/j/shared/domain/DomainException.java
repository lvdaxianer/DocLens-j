package io.github.lvdaxianer.doclens.j.shared.domain;

/**
 * 领域规则违反场景的基础异常。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public class DomainException extends RuntimeException {

    /**
     * 创建领域异常。
     *
     * @param message 异常消息
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public DomainException(String message) {
        super(message);
    }
}
