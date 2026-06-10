package io.github.lvdaxianer.doclens.j.health.domain;

/**
 * 模型健康失败类型。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public enum ModelHealthFailureType {
    TIMEOUT,
    CONNECTION_REFUSED,
    HTTP_5XX,
    AUTH_FAILED,
    RATE_LIMITED,
    BAD_RESPONSE,
    REQUEST_FAILED,
    UNKNOWN
}
