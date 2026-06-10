package io.github.lvdaxianer.doclens.j.processing.application;

/**
 * LLM Markdown 后处理配置提交参数。
 *
 * @param apiType API 协议类型
 * @param url LLM 接口地址
 * @param model 模型名称
 * @param apiKey API Key，可为空
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public record LlmMarkdownConfigSettings(String apiType, String url, String model, String apiKey) {
}
