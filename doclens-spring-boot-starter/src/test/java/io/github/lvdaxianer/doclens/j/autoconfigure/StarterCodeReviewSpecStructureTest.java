package io.github.lvdaxianer.doclens.j.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Starter 模块 code-review-spec 结构约束测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
class StarterCodeReviewSpecStructureTest {

    private static final Path STARTER_SOURCE_ROOT = Path.of("src/main/java");
    private static final int MAX_SOURCE_FILE_LINES = 350;
    private static final List<Path> TOUCHED_AUTO_CONFIGURATIONS = List.of(
            STARTER_SOURCE_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/autoconfigure/DocLensProcessingAutoConfiguration.java"),
            STARTER_SOURCE_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/autoconfigure/DocLensLlmMarkdownAutoConfiguration.java"),
            STARTER_SOURCE_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/autoconfigure/DocLensDocumentLifecycleAutoConfiguration.java"),
            STARTER_SOURCE_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/autoconfigure/DocLensStaleDocumentRecoveryAutoConfiguration.java"),
            STARTER_SOURCE_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/adapter/infrastructure/DashScopeOnlineOcrClient.java"),
            STARTER_SOURCE_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrHealthChecker.java")
    );

    /**
     * 批次处理自动配置必须保持在规范允许的文件行数内。
     *
     * @throws IOException 读取源文件失败
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void touchedAutoConfigurationsStayWithinCodeReviewSpecLineLimit() throws IOException {
        for (Path source : TOUCHED_AUTO_CONFIGURATIONS) {
            assertThat(Files.readAllLines(source))
                    .as(source.toString())
                    .hasSizeLessThanOrEqualTo(MAX_SOURCE_FILE_LINES);
        }
    }
}
