package io.github.lvdaxianer.doclens.j.health.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.health.application.ModelHeartbeatProbeResult;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthFailureType;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigSettings;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigTestResponse;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigTester;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownApiType;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * LLM Markdown 心跳探针测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
class LlmMarkdownHeartbeatProbeTest {

    private static final String SECRET_KEY = "sk-secret";

    /**
     * LLM 配置连通时探针应返回成功。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void reachableConfigReturnsSuccess() {
        LlmMarkdownHeartbeatProbe probe = new LlmMarkdownHeartbeatProbe(repository(config()),
                settings -> LlmMarkdownConfigTestResponse.reachable());

        ModelHeartbeatProbeResult result = probe.probe();

        assertThat(result.isSuccessful()).isTrue();
    }

    /**
     * LLM 未配置时探针应返回坏响应且不调用远端测试器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void unconfiguredConfigReturnsBadResponseWithoutRemoteProbe() {
        RecordingTester tester = new RecordingTester(LlmMarkdownConfigTestResponse.reachable());
        LlmMarkdownHeartbeatProbe probe = new LlmMarkdownHeartbeatProbe(repository(LlmMarkdownConfig.unconfigured()),
                tester);

        ModelHeartbeatProbeResult result = probe.probe();

        assertThat(result.failureType()).isEqualTo(ModelHealthFailureType.BAD_RESPONSE);
        assertThat(tester.isCalled).isFalse();
    }

    /**
     * LLM 配置不存在时探针应返回坏响应。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void missingConfigReturnsBadResponse() {
        LlmMarkdownHeartbeatProbe probe = new LlmMarkdownHeartbeatProbe(repository(null),
                settings -> LlmMarkdownConfigTestResponse.reachable());

        ModelHeartbeatProbeResult result = probe.probe();

        assertThat(result.failureType()).isEqualTo(ModelHealthFailureType.BAD_RESPONSE);
    }

    /**
     * LLM 连通测试失败时探针应返回坏响应且不泄露凭证。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void unreachableConfigReturnsBadResponseWithoutSecret() {
        LlmMarkdownHeartbeatProbe probe = new LlmMarkdownHeartbeatProbe(repository(config()),
                settings -> LlmMarkdownConfigTestResponse.unreachable("invalid api key " + SECRET_KEY));

        ModelHeartbeatProbeResult result = probe.probe();

        assertThat(result.failureType()).isEqualTo(ModelHealthFailureType.BAD_RESPONSE);
        assertThat(result.message()).doesNotContain(SECRET_KEY);
    }

    /**
     * LLM 连通测试抛出异常时探针应返回未知失败且不泄露凭证。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void testerExceptionReturnsUnknownWithoutSecret() {
        LlmMarkdownHeartbeatProbe probe = new LlmMarkdownHeartbeatProbe(repository(config()), settings -> {
            throw new IllegalStateException("bad credential " + SECRET_KEY);
        });

        ModelHeartbeatProbeResult result = probe.probe();

        assertThat(result.failureType()).isEqualTo(ModelHealthFailureType.UNKNOWN);
        assertThat(result.message()).doesNotContain(SECRET_KEY);
    }

    /**
     * 创建 LLM Markdown 配置仓储。
     *
     * @param config 当前配置
     * @return LLM Markdown 配置仓储
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private LlmMarkdownConfigRepository repository(LlmMarkdownConfig config) {
        return new LlmMarkdownConfigRepository() {
            /**
             * 查询当前测试配置。
             *
             * @return 当前测试配置
             * @author lvdaxianerplus
             * @date 2026-06-10
             */
            @Override
            public Optional<LlmMarkdownConfig> find() {
                return Optional.ofNullable(config);
            }

            /**
             * 心跳探针不应持久化 LLM 配置。
             *
             * @param saved 待保存配置
             * @author lvdaxianerplus
             * @date 2026-06-10
             */
            @Override
            public void save(LlmMarkdownConfig saved) {
                throw new UnsupportedOperationException("heartbeat probe must not persist config");
            }
        };
    }

    /**
     * 创建已配置的 LLM Markdown 配置。
     *
     * @return LLM Markdown 配置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private LlmMarkdownConfig config() {
        return LlmMarkdownConfig.configured("default", LlmMarkdownApiType.ANTHROPIC,
                "https://api.example.com/anthropic/v1/messages", "MiniMax-M3", SECRET_KEY);
    }

    /**
     * 记录调用状态的 LLM 配置测试器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static final class RecordingTester implements LlmMarkdownConfigTester {

        private final LlmMarkdownConfigTestResponse response;
        private boolean isCalled;

        /**
         * 创建记录型测试器。
         *
         * @param response 测试响应
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        private RecordingTester(LlmMarkdownConfigTestResponse response) {
            this.response = response;
        }

        @Override
        public LlmMarkdownConfigTestResponse test(LlmMarkdownConfigSettings settings) {
            isCalled = true;
            return response;
        }
    }
}
