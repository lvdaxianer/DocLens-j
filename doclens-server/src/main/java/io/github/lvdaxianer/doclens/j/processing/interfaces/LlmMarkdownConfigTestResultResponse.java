package io.github.lvdaxianer.doclens.j.processing.interfaces;

import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigTestResponse;

/**
 * LLM Markdown 配置连通性测试的 HTTP 响应。
 *
 * @param healthy 是否连通
 * @param message 测试结果消息
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record LlmMarkdownConfigTestResultResponse(boolean healthy, String message) {

    /**
     * 将应用层测试结果转换为 HTTP 响应。
     *
     * @param response 应用层测试结果
     * @return HTTP 响应
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public static LlmMarkdownConfigTestResultResponse from(LlmMarkdownConfigTestResponse response) {
        return new LlmMarkdownConfigTestResultResponse(response.healthy(), response.message());
    }
}
