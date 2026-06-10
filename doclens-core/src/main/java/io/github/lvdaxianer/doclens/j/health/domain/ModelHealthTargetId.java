package io.github.lvdaxianer.doclens.j.health.domain;

/**
 * 模型健康目标唯一标识。
 *
 * @param targetType 目标类型
 * @param modelKey 模型标识
 * @param targetId 目标实例标识
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record ModelHealthTargetId(
        ModelHealthTargetType targetType,
        String modelKey,
        String targetId
) {

    /**
     * 创建模型健康目标标识。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public ModelHealthTargetId {
        if (targetType == null) {
            throw new IllegalArgumentException("model health target type is required");
        } else {
            // 目标类型已提供，可以继续校验文本标识。
        }
        modelKey = requiredText(modelKey, "model health model key is required");
        targetId = requiredText(targetId, "model health target id is required");
    }

    /**
     * 校验必填文本。
     *
     * @param value 文本值
     * @param message 异常消息
     * @return 去除首尾空白后的文本
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        } else {
            return value.trim();
        }
    }
}
