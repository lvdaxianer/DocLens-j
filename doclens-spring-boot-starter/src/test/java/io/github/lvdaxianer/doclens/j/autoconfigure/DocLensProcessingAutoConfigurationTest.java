package io.github.lvdaxianer.doclens.j.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingRequest;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessor;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigRepository;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.ConfigurableMarkdownPostProcessor;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.HttpMarkdownPostProcessor;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.HttpMarkdownPostProcessor.HttpMarkdownPostProcessorOptions;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        MarkdownPostProcessor processor = new DocLensLlmMarkdownAutoConfiguration()
                .fallbackMarkdownPostProcessor(new ObjectMapper(), new DocLensSpringProperties(null, false, null, null, null,
                        null, null, null, null, null, null, null, null, null));

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
                null, null, null, null, null, llmMarkdown, null);

        MarkdownPostProcessor processor = new DocLensLlmMarkdownAutoConfiguration()
                .fallbackMarkdownPostProcessor(new ObjectMapper(), properties);

        assertThat(processor).isInstanceOf(HttpMarkdownPostProcessor.class);
    }

    /**
     * Spring 配置中的 LLM URL 应按用户配置原样使用。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void createsHttpMarkdownPostProcessorWithConfiguredUrlUnchanged() {
        DocLensSpringProperties.LlmMarkdownProperties llmMarkdown =
                new DocLensSpringProperties.LlmMarkdownProperties(
                        "https://dashscope.aliyuncs.com/compatible-mode/v1",
                        "qwen-vl-ocr-2025-11-20", "sk-configured");
        DocLensSpringProperties properties = new DocLensSpringProperties(null, false, null, null, null, null, null,
                null, null, null, null, null, llmMarkdown, null);

        MarkdownPostProcessor processor = new DocLensLlmMarkdownAutoConfiguration()
                .fallbackMarkdownPostProcessor(new ObjectMapper(), properties);

        assertThat(processor).isInstanceOf(HttpMarkdownPostProcessor.class);
        assertThat(readEndpoint((HttpMarkdownPostProcessor) processor).toString())
                .isEqualTo("https://dashscope.aliyuncs.com/compatible-mode/v1");
    }

    /**
     * 默认 Markdown 后处理器应支持运行时配置覆盖。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void createsConfigurableMarkdownPostProcessor() {
        LlmMarkdownConfigRepository repository = new EmptyConfigRepository();

        MarkdownPostProcessor processor = new DocLensLlmMarkdownAutoConfiguration()
                .markdownPostProcessor(new ObjectMapper(), new DocLensSpringProperties(null, false, null, null, null,
                        null, null, null, null, null, null, null, null, null), repository);

        assertThat(processor).isInstanceOf(ConfigurableMarkdownPostProcessor.class);
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

    /**
     * 读取 HTTP Markdown 后处理器内部 endpoint。
     *
     * @param processor 被测处理器
     * @return 规范化后的 endpoint
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private java.net.URI readEndpoint(HttpMarkdownPostProcessor processor) {
        try {
            Field optionsField = HttpMarkdownPostProcessor.class.getDeclaredField("options");
            optionsField.setAccessible(true);
            HttpMarkdownPostProcessorOptions options =
                    (HttpMarkdownPostProcessorOptions) optionsField.get(processor);
            return options.endpoint();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("read endpoint failed", ex);
        }
    }

    /**
     * 空配置仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static final class EmptyConfigRepository implements LlmMarkdownConfigRepository {

        /**
         * 查询当前配置。
         *
         * @return 当前配置
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        @Override
        public Optional<io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig> find() {
            return Optional.empty();
        }

        /**
         * 保存配置。
         *
         * @param config LLM Markdown 配置
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        @Override
        public void save(io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig config) {
        }

        /**
         * 批量保存配置。
         *
         * @param configs LLM Markdown 配置列表
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        @Override
        public void saveAll(List<io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig> configs) {
        }
    }
}
