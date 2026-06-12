package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigSettings;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigTestResponse;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigTester;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownApiType;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * LLM Markdown 健康检查器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
class LlmMarkdownHealthCheckerTest {

    private static final OffsetDateTime CHECKED_AT = OffsetDateTime.parse("2026-06-10T12:00:00+08:00");

    /**
     * 连通成功时应持久化健康状态并清空失败消息。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void healthyCheckPersistsHealthyState() {
        InMemoryConfigRepository repository = new InMemoryConfigRepository(config());
        LlmMarkdownHealthChecker checker = new LlmMarkdownHealthChecker(repository,
                settings -> LlmMarkdownConfigTestResponse.reachable(), () -> CHECKED_AT);

        Optional<LlmMarkdownConfig> checked = checker.checkOnce();

        assertThat(checked).get().extracting(LlmMarkdownConfig::healthy).isEqualTo(true);
        assertThat(repository.config).extracting(LlmMarkdownConfig::healthMessage).isEqualTo("");
        assertThat(repository.config.lastHealthAt()).contains(CHECKED_AT);
    }

    /**
     * 连通失败时应持久化失败状态和可展示原因。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void failedCheckPersistsFailureMessage() {
        InMemoryConfigRepository repository = new InMemoryConfigRepository(config());
        LlmMarkdownHealthChecker checker = new LlmMarkdownHealthChecker(repository,
                settings -> LlmMarkdownConfigTestResponse.unreachable("LLM Markdown returned HTTP 401"),
                () -> CHECKED_AT);

        Optional<LlmMarkdownConfig> checked = checker.checkOnce();

        assertThat(checked).get().extracting(LlmMarkdownConfig::healthy).isEqualTo(false);
        assertThat(repository.config).extracting(LlmMarkdownConfig::healthMessage)
                .isEqualTo("LLM Markdown returned HTTP 401");
        assertThat(repository.config.lastHealthAt()).contains(CHECKED_AT);
    }

    /**
     * 健康检查应携带保存的协议类型和凭证。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void checkUsesConfiguredApiTypeAndCredential() {
        InMemoryConfigRepository repository = new InMemoryConfigRepository(config());
        RecordingTester tester = new RecordingTester();
        LlmMarkdownHealthChecker checker = new LlmMarkdownHealthChecker(repository, tester, () -> CHECKED_AT);

        checker.checkOnce();

        assertThat(tester.settings).isNotNull();
        assertThat(tester.settings.apiType()).isEqualTo("anthropic");
        assertThat(tester.settings.apiKey()).isEqualTo("sk-secret");
    }

    /**
     * 没有任何 LLM 配置时健康检查应空跑。
     *
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void checkReturnsEmptyWhenNoConfigExists() {
        InMemoryConfigRepository repository = new InMemoryConfigRepository(null);
        RecordingTester tester = new RecordingTester();
        LlmMarkdownHealthChecker checker = new LlmMarkdownHealthChecker(repository, tester, () -> CHECKED_AT);

        Optional<LlmMarkdownConfig> checked = checker.checkOnce();

        assertThat(checked).isEmpty();
        assertThat(tester.settings).isNull();
    }

    /**
     * 创建测试配置。
     *
     * @return LLM Markdown 配置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private LlmMarkdownConfig config() {
        return LlmMarkdownConfig.configured("default", LlmMarkdownApiType.ANTHROPIC,
                "https://api.example.com/anthropic/v1/messages", "MiniMax-M3", "sk-secret");
    }

    /**
     * 内存配置仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static final class InMemoryConfigRepository implements LlmMarkdownConfigRepository {

        private LlmMarkdownConfig config;

        /**
         * 创建内存配置仓储。
         *
         * @param config 初始配置
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        private InMemoryConfigRepository(LlmMarkdownConfig config) {
            this.config = config;
        }

        /**
         * 查询当前测试配置。
         *
         * @return 当前测试配置
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        @Override
        public Optional<LlmMarkdownConfig> find() {
            return Optional.ofNullable(config);
        }

        /**
         * 查询全部测试配置。
         *
         * @return 配置列表
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        @Override
        public List<LlmMarkdownConfig> listConfigs() {
            return config == null ? List.of() : List.of(config);
        }

        /**
         * 保存测试配置。
         *
         * @param config LLM Markdown 配置
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        @Override
        public void save(LlmMarkdownConfig config) {
            this.config = config;
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
            // 批量输入非空时，单配置测试仓储取最后一条作为最终状态。
            if (!configs.isEmpty()) {
                this.config = configs.get(configs.size() - 1);
            }
        }
    }

    /**
     * 记录测试入参的 LLM 配置测试器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static final class RecordingTester implements LlmMarkdownConfigTester {

        private LlmMarkdownConfigSettings settings;

        /**
         * 记录健康测试请求并返回成功。
         *
         * @param settings LLM Markdown 测试配置
         * @return 连通成功响应
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        @Override
        public LlmMarkdownConfigTestResponse test(LlmMarkdownConfigSettings settings) {
            this.settings = settings;
            return LlmMarkdownConfigTestResponse.reachable();
        }
    }
}
