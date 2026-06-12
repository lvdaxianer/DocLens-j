package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * PaddleOCR 原生响应映射器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
class PaddleOcrNativeResponseMapperTest {

    private static final int TEST_PAGE_NO = 3;
    private static final String TEXT_KEY = "text";
    private static final String OCR_PROVIDER_KEY = "ocr_provider";
    private static final String OCR_FORMAT_KEY = "ocr_format";
    private static final String PADDLE_OCR_PROVIDER = "paddle_ocr";
    private static final String MARKDOWN_FORMAT = "markdown";
    private static final String EXPECTED_MARKDOWN_TEXT = "标题\n\n第一段\n\n第二段";
    private static final String EMPTY_MARKDOWN_TEXT = "";
    private static final String EMPTY_RESULT_WARNING = "PaddleOCR returned no OCR results";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PaddleOcrNativeResponseMapper mapper = new PaddleOcrNativeResponseMapper(objectMapper);

    /**
     * PaddleOCR 文本块应确定性包装成 Markdown 段落，并标记输出格式。
     *
     * @throws IOException 测试 JSON 解析失败
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void mapsRecognizedTextsToMarkdownParagraphs() throws IOException {
        ImageOcrResult result = mapper.map(TEST_PAGE_NO, response());

        assertThat(result.pageText()).first().extracting(TEXT_KEY).isEqualTo(EXPECTED_MARKDOWN_TEXT);
        assertThat(result.rawOutput())
                .containsEntry(OCR_PROVIDER_KEY, PADDLE_OCR_PROVIDER)
                .containsEntry(OCR_FORMAT_KEY, MARKDOWN_FORMAT);
    }

    /**
     * 空 PaddleOCR 输出应保留空 Markdown 字符串和警告信息。
     *
     * @throws IOException 测试 JSON 解析失败
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void mapsEmptyRecognizedTextsToEmptyMarkdownWithWarning() throws IOException {
        ImageOcrResult result = mapper.map(TEST_PAGE_NO, emptyResponse());

        assertThat(result.pageText()).first().extracting(TEXT_KEY).isEqualTo(EMPTY_MARKDOWN_TEXT);
        assertThat(result.warnings()).containsExactly(EMPTY_RESULT_WARNING);
        assertThat(result.rawOutput()).containsEntry(OCR_FORMAT_KEY, MARKDOWN_FORMAT);
    }

    /**
     * 创建包含多行识别文本的 PaddleOCR 响应。
     *
     * @return PaddleOCR 响应 JSON
     * @throws IOException 测试 JSON 解析失败
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private JsonNode response() throws IOException {
        return objectMapper.readTree("""
                {
                  "errorCode": 0,
                  "errorMsg": "Success",
                  "result": {
                    "ocrResults": [
                      {
                        "prunedResult": {
                          "rec_texts": ["标题", "第一段", "第二段"],
                          "rec_scores": [0.99, 0.98, 0.97],
                          "rec_boxes": [],
                          "rec_polys": []
                        }
                      }
                    ]
                  }
                }
                """);
    }

    /**
     * 创建无识别文本的 PaddleOCR 响应。
     *
     * @return PaddleOCR 响应 JSON
     * @throws IOException 测试 JSON 解析失败
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private JsonNode emptyResponse() throws IOException {
        return objectMapper.readTree("""
                {
                  "errorCode": 0,
                  "errorMsg": "Success",
                  "result": {
                    "ocrResults": [
                      {
                        "prunedResult": {
                          "rec_texts": [],
                          "rec_scores": [],
                          "rec_boxes": [],
                          "rec_polys": []
                        }
                      }
                    ]
                  }
                }
                """);
    }
}
