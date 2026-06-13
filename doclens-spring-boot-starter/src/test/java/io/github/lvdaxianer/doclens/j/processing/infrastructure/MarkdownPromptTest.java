package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunk;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * LLM Markdown 提示词测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
class MarkdownPromptTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int FIRST_CHUNK_INDEX = 0;
    private static final int ONLY_ONE_CHUNK = 1;
    private static final int ESTIMATED_TOKENS = 3;

    /**
     * 分片提示词应明确上下文仅辅助理解且禁止输出思考过程。
     *
     * @throws Exception 元数据序列化失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Test
    void chunkPromptMarksOverlapAsContextOnly() throws Exception {
        MarkdownPostProcessingRequest request = new MarkdownPostProcessingRequest("doc-1", "demo.txt",
                Map.of("source", "test"), "正文");
        MarkdownChunk chunk = new MarkdownChunk(FIRST_CHUNK_INDEX, ONLY_ONE_CHUNK, "上文", "正文", "下文",
                ESTIMATED_TOKENS);

        String prompt = MarkdownPrompt.chunkUserPrompt(OBJECT_MAPPER, request, chunk);

        assertThat(prompt)
                .contains("previous_context")
                .contains("main_content")
                .contains("next_context")
                .contains("只输出 main_content")
                .contains("不要输出思考过程");
    }
}
