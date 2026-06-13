package io.github.lvdaxianer.doclens.j.health.domain;

import java.util.Optional;

/**
 * 模型健康失败摘要。
 *
 * @param lastError 最近错误摘要
 * @param lastFailureType 最近失败类型
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record ModelHealthFailure(
        Optional<String> lastError,
        Optional<ModelHealthFailureType> lastFailureType
) {

    /**
     * 创建模型健康失败摘要。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public ModelHealthFailure {
        lastError = normalizeError(lastError);
        lastFailureType = optional(lastFailureType);
    }

    /**
     * 创建空失败摘要。
     *
     * @return 空失败摘要
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public static ModelHealthFailure empty() {
        return new ModelHealthFailure(Optional.empty(), Optional.empty());
    }

    /**
     * 创建失败摘要。
     *
     * @param failureType 失败类型
     * @param error 错误摘要
     * @return 失败摘要
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public static ModelHealthFailure of(ModelHealthFailureType failureType, String error) {
        return new ModelHealthFailure(Optional.ofNullable(error), Optional.ofNullable(failureType));
    }

    /**
     * 规范化可选值。
     *
     * @param value 可选值
     * @param <T> 值类型
     * @return 非空可选值
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static <T> Optional<T> optional(Optional<T> value) {
        if (value == null) {
            return Optional.empty();
        } else {
            return value;
        }
    }

    /**
     * 规范化错误摘要。
     *
     * @param value 错误摘要
     * @return 非空错误摘要
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static Optional<String> normalizeError(Optional<String> value) {
        return optional(value).map(String::trim).filter(error -> !error.isBlank());
    }
}
