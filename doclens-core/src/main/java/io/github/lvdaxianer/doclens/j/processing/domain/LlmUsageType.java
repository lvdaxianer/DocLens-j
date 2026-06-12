package io.github.lvdaxianer.doclens.j.processing.domain;

/**
 * LLM 配置用途类型。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
public enum LlmUsageType {

    /** OCR Markdown 后处理用途。 */
    MARKDOWN_POST_PROCESSING;

    /**
     * 从外部文本解析用途类型。
     *
     * @param value 外部用途文本
     * @return 用途类型
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public static LlmUsageType from(String value) {
        if (value == null || value.isBlank()) {
            // 老客户端和历史数据默认用于 Markdown 后处理。
            return MARKDOWN_POST_PROCESSING;
        } else {
            // 外部协议统一使用枚举名称，避免大小写差异导致配置无法识别。
            return LlmUsageType.valueOf(value.trim().toUpperCase());
        }
    }
}
