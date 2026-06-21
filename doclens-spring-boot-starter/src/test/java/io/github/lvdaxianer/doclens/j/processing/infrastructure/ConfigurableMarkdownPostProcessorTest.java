package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.lvdaxianer.doclens.j.processing.application.ApproximateTokenEstimator;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunk;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpoint;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpointPlan;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpointPlanSource;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpointStore;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkPlan;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunker;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingRequest;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingResult;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessor;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownApiType;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigBuilder;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmUsageType;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 可配置 LLM Markdown 后处理器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
class ConfigurableMarkdownPostProcessorTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final OffsetDateTime CHECKED_AT = OffsetDateTime.parse("2026-06-12T12:00:00+08:00");
    private static final String RUNTIME_API_KEY_ENV_VAR = "RUNTIME_LLM_API_KEY";
    private static final String PROCESS_API_KEY_ENV_VAR = "DOCLENS_LLM_KEY";
    private static final String RUNTIME_API_KEY = "sk-runtime";
    private static final String TEST_CHUNK_THREAD_PREFIX = "doclens-test-chunk-";
    private static final String TEST_CHECKPOINT_THREAD_PREFIX = "doclens-test-checkpoint-";
    private static final int SMALL_MAX_CONTEXT_TOKENS = 2000;
    private static final String LARGE_DOCUMENT = "段落内容\n\n".repeat(5000);

    @TempDir
    private Path storageRoot;

    /**
     * 存在数据库配置时应优先使用运行时配置调用 LLM。
     *
     * @throws Exception 测试 HTTP 服务异常时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void usesRuntimeConfigBeforeFallbackProcessor() throws Exception {
        try (MockLlmServer server = MockLlmServer.start()) {
            LlmMarkdownConfig config = LlmMarkdownConfig.configured(
                    "default", server.endpoint().toString(), "runtime-model", RUNTIME_API_KEY_ENV_VAR)
                    .updateHealth(true, "", CHECKED_AT);
            ConfigurableMarkdownPostProcessor processor = new ConfigurableMarkdownPostProcessor(OBJECT_MAPPER,
                    new FixedConfigRepository(config), new FallbackProcessor("fallback text"),
                    Map.of(RUNTIME_API_KEY_ENV_VAR, RUNTIME_API_KEY), Executors.newSingleThreadExecutor());

            MarkdownPostProcessingResult result = processor.process(request());

            assertThat(result.markdown()).isEqualTo("# 运行时 Markdown");
            assertThat(server.lastAuthorization()).isEqualTo("Bearer " + RUNTIME_API_KEY);
            assertThat(server.lastBody()).contains("\"model\":\"runtime-model\"");
        }
    }

    /**
     * 生产选项未传显式环境变量映射时，应读取服务进程环境。
     *
     * @throws Exception 测试 HTTP 服务异常时抛出
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Test
    void productionOptionsResolveCredentialsFromProcessEnvironmentWhenMapIsAbsent() throws Exception {
        try (MockLlmServer server = MockLlmServer.start()) {
            LlmMarkdownConfig config = LlmMarkdownConfig.configured(
                    "default", server.endpoint().toString(), "runtime-model", PROCESS_API_KEY_ENV_VAR)
                    .updateHealth(true, "", CHECKED_AT);
            ExecutorService chunkExecutor = Executors.newSingleThreadExecutor(
                    namedThreadFactory(TEST_CHUNK_THREAD_PREFIX));
            try {
                ConfigurableMarkdownRuntimeOptions runtimeOptions = runtimeOptions(chunkExecutor, chunkExecutor,
                        MarkdownChunkCheckpointStore.noop());
                ConfigurableMarkdownPostProcessorOptions options = new ConfigurableMarkdownPostProcessorOptions(
                        OBJECT_MAPPER, new FixedConfigRepository(config), new FallbackProcessor("fallback text"),
                        null, runtimeOptions);

                MarkdownPostProcessingResult result = new ConfigurableMarkdownPostProcessor(options)
                        .process(request());

                assertThat(result.markdown()).isEqualTo("# 运行时 Markdown");
                assertThat(server.lastAuthorization()).isEqualTo("Bearer " + System.getenv(PROCESS_API_KEY_ENV_VAR));
            } finally {
                chunkExecutor.shutdownNow();
            }
        }
    }

    /**
     * 没有数据库配置时应直通 OCR 原文。
     *
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void returnsOriginalOcrTextWhenRuntimeConfigIsAbsent() {
        ConfigurableMarkdownPostProcessor processor = new ConfigurableMarkdownPostProcessor(OBJECT_MAPPER,
                new FixedConfigRepository(null), new FallbackProcessor("fallback text"),
                Executors.newSingleThreadExecutor());

        MarkdownPostProcessingResult result = processor.process(request());

        assertThat(result.markdown()).isEqualTo("OCR 文本");
        assertThat(result.markdownApplied()).isFalse();
        assertThat(result.warnings()).contains("no_available_llm_config");
    }

    /**
     * 运行时配置暂停使用时应直通 OCR 原文且不调用兜底 LLM。
     *
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void returnsPassthroughWhenRuntimeConfigIsDisabled() {
        LlmMarkdownConfig disabledConfig = LlmMarkdownConfig.configured(
                "default", "https://llm.example.com/v1/chat/completions", "runtime-model",
                RUNTIME_API_KEY_ENV_VAR)
                .withEnabled(false);
        ConfigurableMarkdownPostProcessor processor = new ConfigurableMarkdownPostProcessor(OBJECT_MAPPER,
                new FixedConfigRepository(disabledConfig), new FallbackProcessor("fallback text"),
                Executors.newSingleThreadExecutor());

        MarkdownPostProcessingResult result = processor.process(request());

        assertThat(result.markdown()).isEqualTo("OCR 文本");
        assertThat(result.markdownApplied()).isFalse();
    }

    /**
     * 运行时配置全部暂停时应标记暂停直通，不应解析缺失凭证为失败。
     *
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Test
    void returnsPausedPassthroughWhenAllRuntimeConfigsAreDisabledWithoutResolvingCredentials() {
        LlmMarkdownConfig pausedConfig = LlmMarkdownConfig.configured(
                "paused", "https://llm.example.com/v1/chat/completions", "runtime-model",
                RUNTIME_API_KEY_ENV_VAR)
                .withEnabled(false);
        ExecutorService chunkExecutor = Executors.newSingleThreadExecutor(namedThreadFactory(TEST_CHUNK_THREAD_PREFIX));
        try {
            ConfigurableMarkdownPostProcessor processor = new ConfigurableMarkdownPostProcessor(OBJECT_MAPPER,
                    new ListConfigRepository(List.of(pausedConfig)), new FallbackProcessor("fallback text"),
                    Map.of(), chunkExecutor);

            MarkdownPostProcessingResult result = processor.process(request());

            assertThat(result.markdown()).isEqualTo("OCR 文本");
            assertThat(result.markdownApplied()).isFalse();
            assertThat(result.warnings()).contains("llm_markdown_paused")
                    .doesNotContain("llm_markdown_post_processing_failed");
        } finally {
            chunkExecutor.shutdownNow();
        }
    }

    /**
     * 运行时配置处理器应将 checkpoint store 传给分片处理器。
     *
     * @throws Exception 测试 HTTP 服务异常时抛出
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void runtimeProcessorUsesInjectedChunkCheckpointStore() throws Exception {
        try (MockLlmServer server = MockLlmServer.start()) {
            ExecutorService chunkExecutor = Executors.newSingleThreadExecutor();
            LlmMarkdownConfig config = runtimeChunkedConfig(server).updateHealth(true, "", CHECKED_AT);
            FileSystemMarkdownChunkCheckpointStore checkpointStore = new FileSystemMarkdownChunkCheckpointStore(
                    storageRoot);
            try {
                MarkdownPostProcessingRequest request = largeRequest();
                seedAllChunkCheckpoints(checkpointStore, request, "cached-runtime");
                ConfigurableMarkdownPostProcessor processor = configurableProcessor(config, checkpointStore,
                        runtimeOptions(chunkExecutor, chunkExecutor, checkpointStore));

                MarkdownPostProcessingResult result = processor.process(request);

                assertThat(result.markdown()).contains("cached-runtime-0");
                assertThat(server.requestCount()).isZero();
            } finally {
                chunkExecutor.shutdownNow();
            }
        }
    }

    /**
     * 运行时配置处理器应把独立 checkpoint 执行器传给分片处理器。
     *
     * @throws Exception 测试 HTTP 服务异常时抛出
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void runtimeProcessorUsesInjectedCheckpointExecutor() throws Exception {
        try (MockLlmServer server = MockLlmServer.start()) {
            ExecutorService chunkExecutor = Executors.newSingleThreadExecutor(
                    namedThreadFactory(TEST_CHUNK_THREAD_PREFIX));
            ExecutorService checkpointExecutor = Executors.newSingleThreadExecutor(
                    namedThreadFactory(TEST_CHECKPOINT_THREAD_PREFIX));
            RecordingCheckpointStore checkpointStore = new RecordingCheckpointStore();
            try {
                LlmMarkdownConfig config = runtimeChunkedConfig(server).updateHealth(true, "", CHECKED_AT);
                ConfigurableMarkdownPostProcessor processor = configurableProcessor(config, checkpointStore,
                        runtimeOptions(chunkExecutor, checkpointExecutor, checkpointStore));

                processor.process(largeRequest());

                assertThat(checkpointStore.saveThreadName()).startsWith(TEST_CHECKPOINT_THREAD_PREFIX);
            } finally {
                checkpointExecutor.shutdownNow();
                chunkExecutor.shutdownNow();
            }
        }
    }

    /**
     * 创建 Markdown 后处理请求。
     *
     * @return Markdown 后处理请求
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private MarkdownPostProcessingRequest request() {
        return new MarkdownPostProcessingRequest("doc-1", "demo.txt", Map.of("source", "test"), "OCR 文本");
    }

    /**
     * 创建大文档 Markdown 后处理请求。
     *
     * @return Markdown 后处理请求
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private MarkdownPostProcessingRequest largeRequest() {
        return new MarkdownPostProcessingRequest("doc-runtime", "demo.txt", Map.of("source", "test"),
                LARGE_DOCUMENT);
    }

    /**
     * 创建带 checkpoint store 的可配置处理器。
     *
     * @param config LLM Markdown 配置
     * @param checkpointStore checkpoint 存储
     * @return 可配置处理器
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private ConfigurableMarkdownPostProcessor configurableProcessor(
            LlmMarkdownConfig config,
            MarkdownChunkCheckpointStore checkpointStore,
            ConfigurableMarkdownRuntimeOptions runtimeOptions
    ) {
        ConfigurableMarkdownPostProcessorOptions options = new ConfigurableMarkdownPostProcessorOptions(OBJECT_MAPPER,
                new FixedConfigRepository(config), new FallbackProcessor("fallback text"),
                Map.of(RUNTIME_API_KEY_ENV_VAR, RUNTIME_API_KEY), runtimeOptions);
        return new ConfigurableMarkdownPostProcessor(options);
    }

    /**
     * 创建可配置 Markdown 运行时选项。
     *
     * @param chunkExecutor 分片执行器
     * @param checkpointExecutor checkpoint 执行器
     * @param checkpointStore checkpoint 存储
     * @return 可配置 Markdown 运行时选项
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private ConfigurableMarkdownRuntimeOptions runtimeOptions(
            ExecutorService chunkExecutor,
            ExecutorService checkpointExecutor,
            MarkdownChunkCheckpointStore checkpointStore
    ) {
        return new ConfigurableMarkdownRuntimeOptions(chunkExecutor, checkpointExecutor, checkpointStore);
    }

    /**
     * 创建带名称前缀的测试线程工厂。
     *
     * @param prefix 线程名前缀
     * @return 测试线程工厂
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private ThreadFactory namedThreadFactory(String prefix) {
        return runnable -> new Thread(runnable, prefix + "1");
    }

    /**
     * 创建小上下文运行时配置。
     *
     * @param server 测试 LLM 服务
     * @return 运行时配置
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private LlmMarkdownConfig runtimeChunkedConfig(MockLlmServer server) {
        LlmMarkdownConfigBuilder builder = LlmMarkdownConfig.builder("default", "默认 LLM 配置",
                LlmMarkdownApiType.OPENAI);
        return builder.endpoint(server.endpoint().toString(), "runtime-model")
                .credential(RUNTIME_API_KEY_ENV_VAR)
                .usage(LlmUsageType.MARKDOWN_POST_PROCESSING, 100)
                .runtimeLimits(SMALL_MAX_CONTEXT_TOKENS, 1, 0)
                .defaultConfig(true)
                .enabled(true)
                .build();
    }

    /**
     * 写入当前计划的全部 chunk checkpoint。
     *
     * @param checkpointStore checkpoint 存储
     * @param request Markdown 后处理请求
     * @param prefix Markdown 前缀
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private void seedAllChunkCheckpoints(
            FileSystemMarkdownChunkCheckpointStore checkpointStore,
            MarkdownPostProcessingRequest request,
            String prefix
    ) {
        MarkdownChunkPlan plan = chunkPlan(request);
        MarkdownChunkCheckpointPlan checkpointPlan = MarkdownChunkCheckpointPlan.from(
                new MarkdownChunkCheckpointPlanSource(request, plan, SMALL_MAX_CONTEXT_TOKENS));
        for (MarkdownChunk chunk : plan.chunks()) {
            checkpointStore.save(new MarkdownChunkCheckpoint(checkpointPlan, chunk,
                    prefix + "-" + chunk.chunkIndex(), true));
        }
    }

    /**
     * 生成当前请求的 chunk 计划。
     *
     * @param request Markdown 后处理请求
     * @return chunk 计划
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private MarkdownChunkPlan chunkPlan(MarkdownPostProcessingRequest request) {
        MarkdownChunker chunker = new MarkdownChunker(new ApproximateTokenEstimator());
        return chunker.plan(request.ocrText(), SMALL_MAX_CONTEXT_TOKENS, request.chunkStrategy());
    }

    /**
     * 固定配置仓储。
     *
     * @param config LLM Markdown 配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private record FixedConfigRepository(LlmMarkdownConfig config) implements LlmMarkdownConfigRepository {

        /**
         * 查询当前配置。
         *
         * @return 当前配置
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        @Override
        public Optional<LlmMarkdownConfig> find() {
            return Optional.ofNullable(config);
        }

        /**
         * 按用途查询配置。
         *
         * @param usageType 配置用途
         * @return 配置列表
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        @Override
        public List<LlmMarkdownConfig> listByUsage(LlmUsageType usageType) {
            return Optional.ofNullable(config)
                    .filter(current -> current.usageType() == usageType)
                    .stream()
                    .toList();
        }

        /**
         * 保存配置。
         *
         * @param config LLM Markdown 配置
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        @Override
        public void save(LlmMarkdownConfig config) {
        }

        /**
         * 批量保存配置。
         *
         * @param configs LLM Markdown 配置列表
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        @Override
        public void saveAll(List<LlmMarkdownConfig> configs) {
        }
    }

    /**
     * 固定配置列表仓储。
     *
     * @param configs LLM Markdown 配置列表
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private record ListConfigRepository(List<LlmMarkdownConfig> configs) implements LlmMarkdownConfigRepository {

        /**
         * 查询当前配置。
         *
         * @return 当前配置
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        @Override
        public Optional<LlmMarkdownConfig> find() {
            return configs.stream().findFirst();
        }

        /**
         * 按用途查询配置。
         *
         * @param usageType 配置用途
         * @return 配置列表
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        @Override
        public List<LlmMarkdownConfig> listByUsage(LlmUsageType usageType) {
            return configs.stream().filter(config -> config.usageType() == usageType).toList();
        }

        /**
         * 保存配置。
         *
         * @param config LLM Markdown 配置
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        @Override
        public void save(LlmMarkdownConfig config) {
        }

        /**
         * 批量保存配置。
         *
         * @param configs LLM Markdown 配置列表
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        @Override
        public void saveAll(List<LlmMarkdownConfig> configs) {
        }
    }

    /**
     * 固定兜底处理器。
     *
     * @param markdown Markdown 文本
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private record FallbackProcessor(String markdown) implements MarkdownPostProcessor {

        /**
         * 返回固定 Markdown 文本。
         *
         * @param request Markdown 后处理请求
         * @return Markdown 后处理结果
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        @Override
        public MarkdownPostProcessingResult process(MarkdownPostProcessingRequest request) {
            return MarkdownPostProcessingResult.markdown(markdown);
        }
    }

    /**
     * 记录 checkpoint 保存线程的测试存储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private static final class RecordingCheckpointStore implements MarkdownChunkCheckpointStore {

        private final AtomicReference<String> saveThreadName = new AtomicReference<>("");

        /**
         * 保存 checkpoint 并记录线程名。
         *
         * @param checkpoint chunk checkpoint 内容
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public void save(MarkdownChunkCheckpoint checkpoint) {
            saveThreadName.set(Thread.currentThread().getName());
        }

        /**
         * 不返回已有 checkpoint。
         *
         * @param plan checkpoint 计划
         * @param chunk Markdown chunk
         * @return 空 checkpoint
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public Optional<String> load(MarkdownChunkCheckpointPlan plan, MarkdownChunk chunk) {
            return Optional.empty();
        }

        /**
         * 获取保存线程名。
         *
         * @return 保存线程名
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        String saveThreadName() {
            return saveThreadName.get();
        }
    }

    /**
     * 测试用 LLM HTTP 服务。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static final class MockLlmServer implements AutoCloseable {

        private final HttpServer server;
        private String lastAuthorization = "";
        private String lastBody = "";
        private int requestCount;

        /**
         * 创建测试 HTTP 服务。
         *
         * @param server HTTP 服务
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        private MockLlmServer(HttpServer server) {
            this.server = server;
        }

        /**
         * 启动测试 HTTP 服务。
         *
         * @return 测试 HTTP 服务
         * @throws IOException 启动失败时抛出
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        static MockLlmServer start() throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            MockLlmServer mockServer = new MockLlmServer(server);
            server.createContext("/v1/chat/completions", mockServer::handle);
            server.start();
            return mockServer;
        }

        /**
         * 读取接口地址。
         *
         * @return 接口地址
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        URI endpoint() {
            return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions");
        }

        /**
         * 读取最后一次 Authorization Header。
         *
         * @return Authorization Header
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        String lastAuthorization() {
            return lastAuthorization;
        }

        /**
         * 读取最后一次请求体。
         *
         * @return 请求体
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        String lastBody() {
            return lastBody;
        }

        /**
         * 读取请求次数。
         *
         * @return 请求次数
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        int requestCount() {
            return requestCount;
        }

        /**
         * 处理 LLM 请求。
         *
         * @param exchange HTTP 交换对象
         * @throws IOException 读写失败时抛出
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        private void handle(HttpExchange exchange) throws IOException {
            requestCount++;
            lastAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
            lastBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] response = """
                    {"choices":[{"message":{"content":"# 运行时 Markdown"}}]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        }

        /**
         * 关闭测试 HTTP 服务。
         *
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        @Override
        public void close() {
            server.stop(0);
        }
    }
}
