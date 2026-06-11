package io.github.lvdaxianer.doclens.j.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Core 模块 code-review-spec 结构约束测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
class CoreCodeReviewSpecStructureTest {

    private static final Path CORE_SOURCE_ROOT = Path.of("src/main/java");
    private static final int MAX_SOURCE_FILE_LINES = 350;

    private static final List<Path> TOUCHED_PROCESSING_SOURCES = List.of(
            CORE_SOURCE_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCase.java"),
            CORE_SOURCE_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/processing/application/DocumentOcrResultBuilder.java"),
            CORE_SOURCE_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/processing/application/DocumentProcessingEventBuilder.java"),
            CORE_SOURCE_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/processing/application/DocumentMarkdownPostProcessingService.java"),
            CORE_SOURCE_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/processing/application/DocumentPostProcessedText.java"),
            CORE_SOURCE_ROOT.resolve("io/github/lvdaxianer/doclens/j/adapter/domain/OcrNode.java"),
            CORE_SOURCE_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/adapter/application/OcrNodeManagementService.java")
    );

    /**
     * 本次拆分涉及的生产源码必须保持在规范允许的文件行数内。
     *
     * @throws IOException 读取源文件失败
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void touchedProcessingSourcesStayWithinCodeReviewSpecLineLimit() throws IOException {
        for (Path source : TOUCHED_PROCESSING_SOURCES) {
            assertThat(Files.readAllLines(source))
                    .as(source.toString())
                    .hasSizeLessThanOrEqualTo(MAX_SOURCE_FILE_LINES);
        }
    }
}
