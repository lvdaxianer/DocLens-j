package io.github.lvdaxianer.doclens.j.shared.domain;

/**
 * 请求资源不存在时抛出的异常。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * 创建资源不存在异常。
     *
     * @param message 异常消息
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
