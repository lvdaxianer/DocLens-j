package io.github.lvdaxianer.doclens.j.processing.application;

/**
 * LLM Markdown 后处理配置提交参数。
 *
 * @param url OpenAI compatible 接口地址
 * @param model 模型名称
 * @param apiKey API Key，可为空
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public record LlmMarkdownConfigSettings(String url, String model, String apiKey) {
}
