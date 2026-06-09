package io.github.lvdaxianer.doclens.j.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingRequest;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessor;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.HttpMarkdownPostProcessor;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 批次处理自动配置测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
class DocLensProcessingAutoConfigurationTest {

    /**
     * 未配置 LLM Markdown 时应使用直通后处理器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void createsNoopMarkdownPostProcessorWhenLlmMarkdownIsNotConfigured() {
        MarkdownPostProcessor processor = new DocLensProcessingAutoConfiguration()
                .markdownPostProcessor(new ObjectMapper(), new DocLensSpringProperties(null, false, null, null, null,
                        null, null, null, null, null, null, null, null));

        assertThat(processor.process(request()).markdown()).isEqualTo("OCR 文本");
    }

    /**
     * 配置 URL 和模型后应启用 HTTP LLM Markdown 后处理器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void createsHttpMarkdownPostProcessorWhenLlmMarkdownIsConfigured() {
        DocLensSpringProperties.LlmMarkdownProperties llmMarkdown =
                new DocLensSpringProperties.LlmMarkdownProperties("http://127.0.0.1:1/v1/chat/completions",
                        "markdown-model", "sk-configured");
        DocLensSpringProperties properties = new DocLensSpringProperties(null, false, null, null, null, null, null,
                null, null, null, null, llmMarkdown, null);

        MarkdownPostProcessor processor = new DocLensProcessingAutoConfiguration()
                .markdownPostProcessor(new ObjectMapper(), properties);

        assertThat(processor).isInstanceOf(HttpMarkdownPostProcessor.class);
    }

    /**
     * 创建 Markdown 后处理请求。
     *
     * @return Markdown 后处理请求
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private MarkdownPostProcessingRequest request() {
        return new MarkdownPostProcessingRequest("doc-1", "demo.txt", Map.of(), "OCR 文本");
    }
}
