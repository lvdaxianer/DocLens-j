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

    /*
     * 这个结构测试是 code-review-spec 的自动化护栏。
     * 它不试图扫描整个仓库，否则会把历史遗留大测试一次性全部拦住。
     * 当前策略是：凡是本轮已经拆分或触碰的源码，
     * 都加入清单并持续约束在 350 行以内。
     *
     * 生产源码和测试源码分开维护，
     * 方便后续逐步扩大覆盖范围时判断失败来源。
     * 如果某个文件再次膨胀，失败信息会带上具体 path，
     * 便于继续按原子任务拆分。
     *
     * 这个测试只负责结构门禁，不负责业务正确性。
     * 业务正确性仍由各自用例覆盖。
     * 这样失败原因更清楚：
     * 行数失败看这里，行为失败看业务测试。
     * 新任务完成后只把本轮触碰文件加入清单，
     * 不把历史遗留文件一次性塞入同一个提交。
     * 这样每个结构失败都能对应一个可提交的拆分任务。
     * 后续扩大门禁范围时，也应按模块分批推进。
     * 该测试本身也被视为本轮触碰文件，需要遵守同样的注释和行数要求。
     */

    /** Core 生产源码根目录，用于定位被拆分的应用与领域源码。 */
    private static final Path CORE_SOURCE_ROOT = Path.of("src/main/java");
    /** Core 测试源码根目录，用于定位被拆分的测试源码。 */
    private static final Path CORE_TEST_ROOT = Path.of("src/test/java");
    /** code-review-spec 对单个源码文件的行数上限。 */
    private static final int MAX_SOURCE_FILE_LINES = 350;

    /** 本轮已经拆分并纳入持续约束的生产源码清单。 */
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
    /** 本轮已经拆分并纳入持续约束的测试源码清单。 */
    private static final List<Path> TOUCHED_PROCESSING_TESTS = List.of(
            // 批处理主流程测试已完成职责拆分，并持续受行数门禁保护。
            CORE_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseTest.java"),
            CORE_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseLlmMarkdownTest.java"),
            CORE_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseTestSupport.java"),
            CORE_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseRepositories.java"),
            CORE_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseEventRepositories.java"),
            CORE_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseExtractors.java"),
            CORE_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseMarkdownProcessors.java"),
            CORE_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseInfrastructure.java"),
            // 删除用例测试拆分为文档级、批次级和共享测试桩。
            // 文档级测试只覆盖单个 DocumentDeleteUseCase 入口。
            // 批次级测试只覆盖 BatchDeleteUseCase 的原子删除规则。
            // 共享 support 负责构造领域对象，避免测试类重复铺陈。
            // 仓储桩按接口拆开，避免单个测试辅助类重新膨胀。
            // 基础设施桩只保留对象存储记录器和内联事务器。
            // 每个新增测试桩都纳入清单，避免拆分后又悄悄膨胀。
            // 这里不使用目录扫描，是为了让每次扩围都能对应清晰提交。
            // 后续新拆出的测试文件，也应该在同一任务提交里追加到这里。
            CORE_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/processing/application/DocumentDeleteUseCaseTest.java"),
            CORE_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/processing/application/BatchDeleteUseCaseTest.java"),
            CORE_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/processing/application/DocumentDeleteUseCaseTestSupport.java"),
            CORE_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/processing/application/DocumentDeleteUseCaseRepositories.java"),
            CORE_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/processing/application/DocumentDeleteUseCaseInfrastructure.java"),
            CORE_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/processing/application/DeleteUseCaseDocumentJobRepository.java"),
            CORE_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/processing/application/DeleteUseCaseBatchRepository.java"),
            CORE_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/processing/application/DeleteUseCaseOcrResultRepository.java"),
            CORE_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/processing/application/DeleteUseCaseOcrEventRepository.java"),
            // Dashboard 查询测试按读模型职责拆分。
            // Summary 测试只覆盖总览聚合。
            // BatchDetail 测试只覆盖处理轨道状态。
            // OcrRouteDetail 测试只覆盖 OCR 路由展示。
            // 三个文件一起纳入门禁，防止后续继续回涨。
            CORE_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/query/application/DashboardQueryServiceTest.java"),
            CORE_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/query/application/DashboardQueryServiceBatchDetailTest.java"),
            CORE_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/query/application/DashboardQueryServiceOcrRouteDetailTest.java"),
            // Dashboard 基础夹具和 OCR 指标夹具分开纳入门禁。
            // 基础夹具只放领域对象与内存仓储。
            // 指标夹具只放 OCR 资源和命中节点 provider。
            CORE_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/query/application/DashboardQueryServiceFixtures.java"),
            CORE_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/query/application/DashboardOcrMetricsTestFixtures.java"),
            // OCR 路由测试按同步路由和运行态命中跟踪拆分。
            // OcrRoutingServiceTest 只保留重试、故障转移和槽位释放。
            // OcrRoutingRuntimeHitTest 只覆盖请求执行中的命中节点快照。
            CORE_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingServiceTest.java"),
            CORE_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingRuntimeHitTest.java"),
            // 路由基础夹具和批次命中夹具分开纳入门禁。
            // 这样测试桩也要持续保持单一职责，
            // 不会因为不是 Test 类就绕过 code-review-spec。
            CORE_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingTestFixtures.java"),
            CORE_TEST_ROOT.resolve(
                    "io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingHitTestFixtures.java")
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
            // 每个已拆分生产源码都保持在 code-review-spec 单文件行数限制内。
            assertThat(Files.readAllLines(source))
                    .as(source.toString())
                    .hasSizeLessThanOrEqualTo(MAX_SOURCE_FILE_LINES);
        }
    }

    /**
     * 本次拆分涉及的测试源码必须保持在规范允许的文件行数内。
     *
     * @throws IOException 读取测试源文件失败
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void touchedProcessingTestsStayWithinCodeReviewSpecLineLimit() throws IOException {
        for (Path source : TOUCHED_PROCESSING_TESTS) {
            // 每个已拆分测试源码都保持在 code-review-spec 单文件行数限制内。
            assertThat(Files.readAllLines(source))
                    .as(source.toString())
                    .hasSizeLessThanOrEqualTo(MAX_SOURCE_FILE_LINES);
        }
    }
}
