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

    /*
     * 结构测试按生产源码和测试源码分组维护。
     * 生产源码拆分任务先行，测试源码拆分任务随后补上。
     *
     * 这样可以让 RED 失败定位到当前职责边界，
     * 避免一次性扫描历史遗留文件导致任务范围失控。
     *
     * 每个新拆分文件都要在对应原子提交中加入清单，
     * 后续如果某个文件重新膨胀，失败会直接指向该文件。
     */

    /** Starter 生产源码根目录。 */
    private static final Path STARTER_SOURCE_ROOT = Path.of("src/main/java");
    /** Starter 测试源码根目录。 */
    private static final Path STARTER_TEST_ROOT = Path.of("src/test/java");
    /** code-review-spec 对单文件行数的上限。 */
    private static final int MAX_SOURCE_FILE_LINES = 350;
    /** 已拆分并纳入持续约束的 Starter 生产源码清单。 */
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
                    "io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrHealthChecker.java"),
            STARTER_SOURCE_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/query/infrastructure/OcrDashboardMetricsProvider.java")
    );
    /** 已拆分并纳入持续约束的 Starter 测试源码清单。 */
    private static final List<Path> TOUCHED_TEST_SOURCES = List.of(
            STARTER_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/query/infrastructure/OcrDashboardMetricsProviderTest.java"),
            STARTER_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/query/infrastructure/OcrDashboardHitNodesMetricsProviderTest.java"),
            STARTER_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/query/infrastructure/OcrDashboardMetricsProviderTestSupport.java")
    );

    /**
     * 批次处理自动配置必须保持在规范允许的文件行数内。
     * 该护栏只覆盖已经拆过的生产源码，
     * 不把未处理遗留文件混进当前任务。
     *
     * @throws IOException 读取源文件失败
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void touchedAutoConfigurationsStayWithinCodeReviewSpecLineLimit() throws IOException {
        for (Path source : TOUCHED_AUTO_CONFIGURATIONS) {
            // 生产源码已完成职责拆分后，必须持续保持在规范行数内。
            assertThat(Files.readAllLines(source))
                    .as(source.toString())
                    .hasSizeLessThanOrEqualTo(MAX_SOURCE_FILE_LINES);
        }
    }

    /**
     * 本次拆分涉及的 Starter 测试源码必须保持在规范允许的文件行数内。
     * 测试文件同样需要拆分职责，
     * 避免一个契约测试同时承担过多场景。
     *
     * @throws IOException 读取测试源文件失败
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void touchedTestSourcesStayWithinCodeReviewSpecLineLimit() throws IOException {
        for (Path source : TOUCHED_TEST_SOURCES) {
            // 测试源码同样受行数护栏约束，防止契约测试再次膨胀。
            assertThat(Files.readAllLines(source))
                    .as(source.toString())
                    .hasSizeLessThanOrEqualTo(MAX_SOURCE_FILE_LINES);
        }
    }
}
