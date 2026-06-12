package io.github.lvdaxianer.doclens.j.processing.interfaces;

/**
 * LLM Markdown 配置启停请求。
 *
 * @param enabled 是否启用
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
public record LlmMarkdownConfigEnabledRequest(boolean enabled) {
}
