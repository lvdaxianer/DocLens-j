package io.github.lvdaxianer.doclens.j.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.processing.application.ChunkStrategy;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunk;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpoint;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpointPlan;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpointStore;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigRepository;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.FileSystemMarkdownChunkCheckpointStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;

/**
 * LLM Markdown checkpoint 自动配置测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
@SpringBootTest(classes = DocLensLlmMarkdownCheckpointAutoConfigurationTest.TestApplication.class)
@TestPropertySource(properties = DocLensLlmMarkdownCheckpointAutoConfigurationTest.STORAGE_ROOT_PROPERTY)
class DocLensLlmMarkdownCheckpointAutoConfigurationTest {

    private static final String TEST_STORAGE_ROOT = "/tmp/doclens-checkpoint-test";
    static final String STORAGE_ROOT_PROPERTY = "doclens.storage-root=" + TEST_STORAGE_ROOT;
    private static final String CHECKPOINT_MARKDOWN = "自动配置 checkpoint";

    @Autowired
    private MarkdownChunkCheckpointStore checkpointStore;

    @Autowired
    @Qualifier("doclensLlmMarkdownCheckpointExecutor")
    private ExecutorService checkpointExecutor;

    /**
     * 默认 checkpoint 存储应使用文件系统实现。
     *
     * @throws Exception 测试文件读取失败
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void createsDefaultFilesystemCheckpointStoreUnderConfiguredStorageRoot() throws Exception {
        MarkdownChunkCheckpointPlan plan = checkpointPlan();
        MarkdownChunk chunk = checkpointChunk();

        checkpointStore.save(new MarkdownChunkCheckpoint(plan, chunk, CHECKPOINT_MARKDOWN, true));

        Path checkpointFile = Path.of(TEST_STORAGE_ROOT)
                .resolve("llm-markdown-chunks/auto-doc/01/README.md");
        assertThat(checkpointStore).isInstanceOf(FileSystemMarkdownChunkCheckpointStore.class);
        assertThat(Files.readString(checkpointFile)).isEqualTo(CHECKPOINT_MARKDOWN);
    }

    /**
     * checkpoint 写入执行器应有独立业务线程名。
     *
     * @throws Exception 线程任务失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void createsCheckpointExecutorWithBusinessThreadName() throws Exception {
        Future<String> threadName = checkpointExecutor.submit(() -> Thread.currentThread().getName());

        assertThat(threadName.get()).startsWith("doclens-llm-markdown-checkpoint-");
    }

    /**
     * 创建测试 checkpoint 计划。
     *
     * @return 测试 checkpoint 计划
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private MarkdownChunkCheckpointPlan checkpointPlan() {
        return new MarkdownChunkCheckpointPlan("auto-doc", "auto.md", 2, ChunkStrategy.general(), 16000,
                "auto-fingerprint");
    }

    /**
     * 创建测试 Markdown 分片。
     *
     * @return 测试 Markdown 分片
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private MarkdownChunk checkpointChunk() {
        return new MarkdownChunk(0, 2, "", "原始 chunk", "", 100);
    }

    /**
     * LLM Markdown checkpoint 测试应用。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @SpringBootConfiguration
    @EnableConfigurationProperties(DocLensSpringProperties.class)
    @ImportAutoConfiguration(DocLensLlmMarkdownAutoConfiguration.class)
    static class TestApplication {

        /**
         * 创建触发 LLM Markdown 自动配置的测试仓储。
         *
         * @return 测试 LLM Markdown 配置仓储
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Bean
        LlmMarkdownConfigRepository llmMarkdownConfigRepository() {
            return new EmptyConfigRepository();
        }

        /**
         * 创建测试 JSON 映射器。
         *
         * @return 测试 JSON 映射器
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    /**
     * 空 LLM Markdown 配置仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private static final class EmptyConfigRepository implements LlmMarkdownConfigRepository {

        /**
         * 查询当前配置。
         *
         * @return 当前配置
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public Optional<LlmMarkdownConfig> find() {
            return Optional.empty();
        }

        /**
         * 保存配置。
         *
         * @param config LLM Markdown 配置
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public void save(LlmMarkdownConfig config) {
        }

        /**
         * 批量保存配置。
         *
         * @param configs LLM Markdown 配置列表
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public void saveAll(List<LlmMarkdownConfig> configs) {
        }
    }
}
