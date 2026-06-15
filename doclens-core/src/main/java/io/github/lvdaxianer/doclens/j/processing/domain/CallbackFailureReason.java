package io.github.lvdaxianer.doclens.j.processing.domain;

/**
 * 回调失败原因。
 *
 * @author lvdaxianerplus
 * @date 2026-06-15
 */
public enum CallbackFailureReason {
    TIMEOUT,
    NETWORK_ERROR,
    HTTP_STATUS,
    INVALID_PAYLOAD,
    UNEXPECTED_EXCEPTION
}
