package io.github.lvdaxianer.doclens.j.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.ingestion.application.BatchProcessingScheduler;
import io.github.lvdaxianer.doclens.j.ingestion.application.BatchStartupRecoveryService;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingRequest;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessor;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigRepository;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.ConfigurableMarkdownPostProcessor;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.HttpMarkdownPostProcessor;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.HttpMarkdownPostProcessor.HttpMarkdownPostProcessorOptions;
import io.github.lvdaxianer.doclens.j.shared.config.DocLensProperties;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

/**
 * 批次处理自动配置测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
class DocLensProcessingAutoConfigurationTest {

    private static final int STARTUP_RECOVERY_LIMIT = 100;

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
                        null, null, null, null, null, null, null, null, null, null, null));

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
                null, null, null, null, null, llmMarkdown, null, null, null);

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
                null, null, null, null, null, llmMarkdown, null, null, null);

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
        ExecutorService chunkExecutor = Executors.newSingleThreadExecutor();

        try {
            MarkdownPostProcessor processor = new DocLensLlmMarkdownAutoConfiguration()
                    .markdownPostProcessor(new ObjectMapper(), new DocLensSpringProperties(null, false, null, null, null,
                            null, null, null, null, null, null, null, null, null, null, null), repository, chunkExecutor);

            assertThat(processor).isInstanceOf(ConfigurableMarkdownPostProcessor.class);
        } finally {
            chunkExecutor.shutdownNow();
        }
    }

    /**
     * 自动处理开启时，启动 runner 应触发批次恢复服务。
     *
     * @throws Exception runner 执行异常
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void startupRecoveryRunnerRecoversWhenAutoProcessingIsEnabled() throws Exception {
        RecordingBatchStartupRecoveryService service = recoveryService();

        new DocLensProcessingAutoConfiguration()
                .batchStartupRecoveryRunner(service, properties(true))
                .run(null);

        assertThat(service.limits).containsExactly(STARTUP_RECOVERY_LIMIT);
    }

    /**
     * 自动处理关闭时，启动 runner 不应自动调度遗留排队批次。
     *
     * @throws Exception runner 执行异常
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void startupRecoveryRunnerSkipsWhenAutoProcessingIsDisabled() throws Exception {
        RecordingBatchStartupRecoveryService service = recoveryService();

        new DocLensProcessingAutoConfiguration()
                .batchStartupRecoveryRunner(service, properties(false))
                .run(null);

        assertThat(service.limits).isEmpty();
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
     * 创建启动恢复测试用配置。
     *
     * @param autoProcessOnUpload 是否自动处理上传批次
     * @return DocLens 运行时配置
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private DocLensProperties properties(boolean autoProcessOnUpload) {
        return new DocLensProperties("target/test-storage", autoProcessOnUpload, "worker-test",
                new DocLensProperties.CallbackProperties(1, 5),
                new DocLensProperties.AdapterProperties("stub_ocr"),
                new DocLensProperties.PaddleOcrProperties(false, "http://127.0.0.1:8080/ocr", 5, false),
                new DocLensProperties.OcrHealthProperties(3, 2),
                new DocLensProperties.ExtractionProperties(1),
                new DocLensProperties.PdfRenderProperties(72, "png"),
                new DocLensProperties.WordConversionProperties("soffice", 5), threadPools());
    }

    /**
     * 创建恢复服务测试实例。
     *
     * @return 记录调用的批次启动恢复服务
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private RecordingBatchStartupRecoveryService recoveryService() {
        return new RecordingBatchStartupRecoveryService(new EmptyDocumentJobRepository(),
                batchId -> {
                });
    }

    /**
     * 创建测试线程池配置。
     *
     * @return 线程池配置
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private DocLensProperties.ThreadPoolsProperties threadPools() {
        DocLensProperties.ThreadPoolProperties pool = new DocLensProperties.ThreadPoolProperties(1, 1, 1, 1,
                "test-");
        return new DocLensProperties.ThreadPoolsProperties(pool, pool, pool, pool);
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

    /**
     * 记录启动恢复调用的测试服务。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private static final class RecordingBatchStartupRecoveryService extends BatchStartupRecoveryService {

        private final List<Integer> limits = new ArrayList<>(1);

        /**
         * 创建记录启动恢复调用的测试服务。
         *
         * @param documentRepository 文档任务仓储
         * @param batchProcessingScheduler 批次处理调度器
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        private RecordingBatchStartupRecoveryService(
                DocumentJobRepository documentRepository,
                BatchProcessingScheduler batchProcessingScheduler
        ) {
            super(documentRepository, batchProcessingScheduler);
        }

        /**
         * 记录恢复批次限制。
         *
         * @param limit 最大恢复批次数量
         * @return 记录后的恢复数量
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public int recoverQueuedBatches(int limit) {
            limits.add(limit);
            return limits.size();
        }
    }

    /**
     * 空文档任务仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private static final class EmptyDocumentJobRepository implements DocumentJobRepository {
    }
}
