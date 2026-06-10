package io.github.lvdaxianer.doclens.j.processing.domain;

/**
 * LLM Markdown 后处理 API 协议类型。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public enum LlmMarkdownApiType {

    OPENAI("openai"),
    ANTHROPIC("anthropic");

    private final String value;

    /**
     * 创建协议类型。
     *
     * @param value 外部传输值
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    LlmMarkdownApiType(String value) {
        this.value = value;
    }

    /**
     * 读取外部传输值。
     *
     * @return 外部传输值
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public String value() {
        return value;
    }

    /**
     * 从外部输入解析协议类型。
     *
     * @param value 外部输入
     * @return 协议类型
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public static LlmMarkdownApiType from(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            return OPENAI;
        } else if (ANTHROPIC.value.equalsIgnoreCase(normalized)) {
            return ANTHROPIC;
        } else if (OPENAI.value.equalsIgnoreCase(normalized)) {
            return OPENAI;
        } else {
            throw new IllegalArgumentException("llm markdown api type must be openai or anthropic");
        }
    }
}
