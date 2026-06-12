package io.github.lvdaxianer.doclens.j.processing.application;

/**
 * LLM Markdown 后处理配置提交参数。
 *
 * @param name 配置名称
 * @param apiType API 协议类型
 * @param url LLM 接口地址
 * @param model 模型名称
 * @param apiKey API Key，可为空
 * @param usageType 配置用途
 * @param priority 优先级
 * @param defaultConfig 是否默认配置
 * @param enabled 是否启用 LLM Markdown 后处理，可为空表示沿用当前状态
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
public record LlmMarkdownConfigSettings(
        String name,
        String apiType,
        String url,
        String model,
        String apiKey,
        String usageType,
        int priority,
        boolean defaultConfig,
        Boolean enabled
) {

    private static final int DEFAULT_PRIORITY = 100;

    /**
     * 兼容不关心启停状态的调用方。
     *
     * @param apiType API 协议类型
     * @param url LLM 接口地址
     * @param model 模型名称
     * @param apiKey API Key，可为空
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public LlmMarkdownConfigSettings(String apiType, String url, String model, String apiKey) {
        this(baseBuilder(apiType, url, model, apiKey));
    }

    /**
     * 兼容只提交启停状态的调用方。
     *
     * @param apiType API 协议类型
     * @param url LLM 接口地址
     * @param model 模型名称
     * @param apiKey API Key，可为空
     * @param enabled 是否启用
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public LlmMarkdownConfigSettings(String apiType, String url, String model, String apiKey, Boolean enabled) {
        this(baseBuilder(apiType, url, model, apiKey).enabled(enabled));
    }

    /**
     * 从构建器创建完整提交参数。
     *
     * @param builder 配置构建器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private LlmMarkdownConfigSettings(Builder builder) {
        this(builder.name, builder.apiType, builder.url, builder.model, builder.apiKey, builder.usageType,
                builder.priority, builder.defaultConfig, builder.enabled);
    }

    /**
     * 创建多配置提交参数构建器。
     *
     * @param name 配置名称
     * @param apiType API 协议类型
     * @param url LLM 接口地址
     * @return 配置构建器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public static Builder builder(String name, String apiType, String url) {
        return new Builder(name, apiType, url);
    }

    /**
     * 创建基础兼容构建器。
     *
     * @param apiType API 协议类型
     * @param url LLM 接口地址
     * @param model 模型名称
     * @param apiKey API Key，可为空
     * @return 配置构建器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private static Builder baseBuilder(String apiType, String url, String model, String apiKey) {
        return builder("", apiType, url).model(model).apiKey(apiKey);
    }

    /**
     * LLM Markdown 配置提交参数构建器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public static final class Builder {

        private final String name;
        private final String apiType;
        private final String url;
        private String model = "";
        private String apiKey = "";
        private String usageType = "";
        private int priority = DEFAULT_PRIORITY;
        private boolean defaultConfig = true;
        private Boolean enabled;

        /**
         * 创建基础构建器。
         *
         * @param name 配置名称
         * @param apiType API 协议类型
         * @param url LLM 接口地址
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        private Builder(String name, String apiType, String url) {
            this.name = name;
            this.apiType = apiType;
            this.url = url;
        }

        /**
         * 设置模型名称。
         *
         * @param model 模型名称
         * @return 当前构建器
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        public Builder model(String model) {
            this.model = model;
            return this;
        }

        /**
         * 设置 API Key。
         *
         * @param apiKey API Key，可为空
         * @return 当前构建器
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * 设置配置用途。
         *
         * @param usageType 配置用途
         * @return 当前构建器
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        public Builder usageType(String usageType) {
            this.usageType = usageType;
            return this;
        }

        /**
         * 设置配置优先级。
         *
         * @param priority 配置优先级
         * @return 当前构建器
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * 设置是否默认配置。
         *
         * @param defaultConfig 是否默认配置
         * @return 当前构建器
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        public Builder defaultConfig(boolean defaultConfig) {
            this.defaultConfig = defaultConfig;
            return this;
        }

        /**
         * 设置是否启用。
         *
         * @param enabled 是否启用
         * @return 当前构建器
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        public Builder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * 构建提交参数。
         *
         * @return 配置提交参数
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        public LlmMarkdownConfigSettings build() {
            return new LlmMarkdownConfigSettings(this);
        }
    }
}
