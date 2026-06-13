package io.github.lvdaxianer.doclens.j.contract;

/**
 * LLM Markdown API 契约测试配置数据对象。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
final class LlmMarkdownTestConfig {

    /** 默认 OpenAI 兼容接口地址。 */
    private static final String DEFAULT_LLM_URL = "https://llm.example.com/v1/chat/completions";
    /** 备用 Anthropic 接口地址。 */
    private static final String ANTHROPIC_URL = "https://api.minimaxi.com/anthropic";
    /** 测试专用凭证环境变量名。 */
    private static final String TEST_CREDENTIAL_ENV_VAR = "MINIMAX_API_KEY";
    /** 测试专用旧凭证环境变量名。 */
    private static final String OLD_TEST_CREDENTIAL_ENV_VAR = "OLD_MINIMAX_API_KEY";
    /** 默认用途类型。 */
    private static final String MARKDOWN_POST_PROCESSING = "MARKDOWN_POST_PROCESSING";
    /** OpenAI 协议类型。 */
    private static final String OPENAI_API_TYPE = "openai";
    /** Anthropic 协议类型。 */
    private static final String ANTHROPIC_API_TYPE = "anthropic";
    /** 默认模型名称。 */
    private static final String MARKDOWN_MODEL = "markdown-model";
    /** Anthropic 模型名称。 */
    private static final String ANTHROPIC_MODEL = "MiniMax-M3";
    /** 新建配置默认优先级。 */
    private static final int CREATED_PRIORITY = 100;
    /** 备用配置优先级。 */
    private static final int BACKUP_PRIORITY = 10;

    /** 配置名称。 */
    private String name;
    /** API 协议类型。 */
    private String apiType;
    /** 完整接口地址。 */
    private String url;
    /** 模型名称。 */
    private String model;
    /** 凭证环境变量名。 */
    private String credentialEnvVar;
    /** 用途类型。 */
    private String usageType = MARKDOWN_POST_PROCESSING;
    /** 优先级。 */
    private int priority;
    /** 是否默认配置。 */
    private boolean isDefault;
    /** 是否启用。 */
    private boolean isEnabled = true;

    /**
     * 限制通过静态工厂创建测试配置。
     *
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private LlmMarkdownTestConfig() {
    }

    /**
     * 创建 OpenAI 测试配置。
     *
     * @param name 配置名称
     * @return OpenAI 测试配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    static LlmMarkdownTestConfig openAi(String name) {
        LlmMarkdownTestConfig config = new LlmMarkdownTestConfig();
        config.name = name;
        config.apiType = OPENAI_API_TYPE;
        config.url = DEFAULT_LLM_URL;
        config.model = MARKDOWN_MODEL;
        config.credentialEnvVar = TEST_CREDENTIAL_ENV_VAR;
        config.priority = CREATED_PRIORITY;
        config.isDefault = true;
        return config;
    }

    /**
     * 创建 Anthropic 测试配置。
     *
     * @param name 配置名称
     * @return Anthropic 测试配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    static LlmMarkdownTestConfig anthropic(String name) {
        LlmMarkdownTestConfig config = new LlmMarkdownTestConfig();
        config.name = name;
        config.apiType = ANTHROPIC_API_TYPE;
        config.url = ANTHROPIC_URL;
        config.model = ANTHROPIC_MODEL;
        config.credentialEnvVar = OLD_TEST_CREDENTIAL_ENV_VAR;
        config.priority = BACKUP_PRIORITY;
        config.isDefault = false;
        return config;
    }

    /**
     * 复制为无凭证环境变量名的配置。
     *
     * @return 无凭证环境变量名的配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    LlmMarkdownTestConfig withoutCredentialEnvVar() {
        credentialEnvVar = "";
        return this;
    }

    /**
     * 复制为指定优先级的配置。
     *
     * @param priority 优先级
     * @return 指定优先级的配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    LlmMarkdownTestConfig priority(int priority) {
        this.priority = priority;
        return this;
    }

    /**
     * 复制为指定模型的配置。
     *
     * @param model 模型名称
     * @return 指定模型的配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    LlmMarkdownTestConfig model(String model) {
        this.model = model;
        return this;
    }

    /**
     * 读取配置名称。
     *
     * @return 配置名称
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    String name() {
        return name;
    }

    /**
     * 读取 API 协议类型。
     *
     * @return API 协议类型
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    String apiType() {
        return apiType;
    }

    /**
     * 读取接口地址。
     *
     * @return 完整接口地址
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    String url() {
        return url;
    }

    /**
     * 读取模型名称。
     *
     * @return 模型名称
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    String model() {
        return model;
    }

    /**
     * 读取凭证环境变量名。
     *
     * @return 凭证环境变量名
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    String credentialEnvVar() {
        return credentialEnvVar;
    }

    /**
     * 读取用途类型。
     *
     * @return 用途类型
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    String usageType() {
        return usageType;
    }

    /**
     * 读取优先级。
     *
     * @return 优先级
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    int priority() {
        return priority;
    }

    /**
     * 判断是否默认配置。
     *
     * @return true 表示默认配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    boolean isDefault() {
        return isDefault;
    }

    /**
     * 判断是否启用。
     *
     * @return true 表示启用
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    boolean isEnabled() {
        return isEnabled;
    }
}
