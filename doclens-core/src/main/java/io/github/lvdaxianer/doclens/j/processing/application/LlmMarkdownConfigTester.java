package io.github.lvdaxianer.doclens.j.processing.application;

/**
 * LLM Markdown 配置测试端口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public interface LlmMarkdownConfigTester {

    /**
     * 测试给定配置是否可连通。
     *
     * @param settings 配置参数
     * @return 测试结果
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    LlmMarkdownConfigTestResponse test(LlmMarkdownConfigSettings settings);
}
