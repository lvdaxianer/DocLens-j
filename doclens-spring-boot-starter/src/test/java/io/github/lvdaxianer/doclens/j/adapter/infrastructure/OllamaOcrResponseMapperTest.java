package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Ollama OCR 响应映射器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
class OllamaOcrResponseMapperTest {

    private static final String TEST_MODEL = "deepseek-ocr:latest";

    private final OllamaOcrResponseMapper mapper = new OllamaOcrResponseMapper(new ObjectMapper());

    /**
     * Ollama response 字段应作为 Markdown 文本进入归一化 OCR 结果。
     *
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void mapsResponseTextToMarkdownResult() throws IOException {
        ImageOcrResult result = mapper.map(request(), TEST_MODEL,
                "{\"response\":\"# 标题\\n\\n正文\",\"done\":true}");

        assertThat(result.pageNo()).isEqualTo(2);
        assertThat(result.pageText()).first().extracting("text").isEqualTo("# 标题\n\n正文");
        assertThat(result.rawOutput())
                .containsEntry("ocr_provider", "ollama")
                .containsEntry("ocr_format", "markdown")
                .containsEntry("ocr_model", TEST_MODEL);
    }

    /**
     * 空 Markdown 响应应被视为模型异常，避免产生看似成功的空结果。
     *
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void rejectsEmptyResponseText() {
        assertThatThrownBy(() -> mapper.map(request(), TEST_MODEL, "{\"response\":\"   \",\"done\":true}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ollama OCR returned empty response");
    }

    /**
     * 创建图片 OCR 请求。
     *
     * @return 图片 OCR 请求
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private ImageOcrRequest request() {
        return new ImageOcrRequest("batch-test", "doc-test", "page.png", 2,
                "image".getBytes(StandardCharsets.UTF_8), JsonPayload.empty());
    }
}
