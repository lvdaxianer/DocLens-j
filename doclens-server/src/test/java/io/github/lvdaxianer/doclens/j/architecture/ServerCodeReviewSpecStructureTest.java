package io.github.lvdaxianer.doclens.j.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Server 模块 code-review-spec 结构约束测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
class ServerCodeReviewSpecStructureTest {

    /*
     * 这个测试为 Server 模块补齐和 Core/Starter 一致的结构护栏。
     * 它只约束本轮已经触碰或拆分的测试文件，
     * 不一次性扫描所有历史遗留超限文件。
     *
     * 每次拆分 server 契约测试时，
     * 都应该在对应原子提交中把文件加入该清单。
     * 这样 RED 失败能精确对应当前任务，
     * GREEN 通过也能持续防止同一文件再次膨胀。
     */

    /** Server 测试源码根目录。 */
    private static final Path SERVER_TEST_ROOT = Path.of("src/test/java");
    /** code-review-spec 对单文件行数的上限。 */
    private static final int MAX_SOURCE_FILE_LINES = 350;
    /** 本轮已经拆分并纳入持续约束的 Server 测试源码清单。 */
    private static final List<Path> TOUCHED_SERVER_TESTS = List.of(
            SERVER_TEST_ROOT.resolve("io/github/lvdaxianer/doclens/j/contract/OcrNodeApiContractTest.java"),
            SERVER_TEST_ROOT.resolve("io/github/lvdaxianer/doclens/j/contract/OcrNodeCredentialApiContractTest.java"),
            SERVER_TEST_ROOT.resolve("io/github/lvdaxianer/doclens/j/contract/OcrNodeGovernanceApiContractTest.java"),
            SERVER_TEST_ROOT.resolve("io/github/lvdaxianer/doclens/j/contract/OcrNodeApiContractSupport.java"),
            SERVER_TEST_ROOT.resolve("io/github/lvdaxianer/doclens/j/contract/DocLensOcrApiContractTest.java"),
            SERVER_TEST_ROOT.resolve("io/github/lvdaxianer/doclens/j/contract/DocLensOcrDeleteApiContractTest.java"),
            SERVER_TEST_ROOT.resolve("io/github/lvdaxianer/doclens/j/contract/DocLensOcrUploadApiContractTest.java"),
            SERVER_TEST_ROOT.resolve("io/github/lvdaxianer/doclens/j/contract/DocLensOcrApiContractSupport.java"),
            SERVER_TEST_ROOT.resolve("io/github/lvdaxianer/doclens/j/contract/LlmMarkdownConfigApiContractTest.java"),
            SERVER_TEST_ROOT.resolve("io/github/lvdaxianer/doclens/j/contract/LlmMarkdownConfigValidationApiContractTest.java"),
            SERVER_TEST_ROOT.resolve("io/github/lvdaxianer/doclens/j/contract/LlmMarkdownConfigTestApiContractTest.java"),
            SERVER_TEST_ROOT.resolve("io/github/lvdaxianer/doclens/j/contract/LlmMarkdownConfigApiContractSupport.java")
    );

    /**
     * 本次拆分涉及的 Server 测试源码必须保持在规范允许的文件行数内。
     *
     * @throws IOException 读取测试源文件失败
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void touchedServerTestsStayWithinCodeReviewSpecLineLimit() throws IOException {
        for (Path source : TOUCHED_SERVER_TESTS) {
            // 每个已拆分 Server 测试源码都保持在 code-review-spec 单文件行数限制内。
            assertThat(Files.readAllLines(source))
                    .as(source.toString())
                    .hasSizeLessThanOrEqualTo(MAX_SOURCE_FILE_LINES);
        }
    }
}
