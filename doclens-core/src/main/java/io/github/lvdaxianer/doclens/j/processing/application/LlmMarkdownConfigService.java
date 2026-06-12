package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigRepository;
import java.util.ArrayList;
import java.util.List;

/**
 * LLM Markdown 后处理配置应用服务。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public class LlmMarkdownConfigService {

    private final LlmMarkdownConfigRepository repository;
    private final LlmMarkdownConfigMutationFactory mutationFactory;

    /**
     * 创建 LLM Markdown 配置服务。
     *
     * @param repository LLM Markdown 配置仓储
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public LlmMarkdownConfigService(LlmMarkdownConfigRepository repository) {
        this.repository = repository;
        this.mutationFactory = new LlmMarkdownConfigMutationFactory();
    }

    /**
     * 查询当前配置或未配置状态。
     *
     * @return 当前配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public LlmMarkdownConfig getConfig() {
        return repository.find().orElseGet(LlmMarkdownConfig::unconfigured);
    }

    /**
     * 查询全部 LLM Markdown 配置。
     *
     * @return 配置列表
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public List<LlmMarkdownConfig> listConfigs() {
        return repository.listConfigs();
    }

    /**
     * 保存 LLM Markdown 配置。
     *
     * @param settings 配置提交参数
     * @return 保存后的配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public LlmMarkdownConfig saveConfig(LlmMarkdownConfigSettings settings) {
        LlmMarkdownConfig current = getConfig();
        LlmMarkdownConfig updated = mutationFactory.buildUpdated(current, settings);
        repository.save(updated);
        return updated;
    }

    /**
     * 创建新的 LLM Markdown 配置。
     *
     * @param settings 配置提交参数
     * @return 新配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public LlmMarkdownConfig createConfig(LlmMarkdownConfigSettings settings) {
        LlmMarkdownConfig updated = mutationFactory.buildNew(settings);
        saveWithDefaultUniqueness(updated);
        return updated;
    }

    /**
     * 更新指定配置启停状态。
     *
     * @param id 配置 ID
     * @param enabled 是否启用
     * @return 更新后的配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public LlmMarkdownConfig updateEnabled(String id, boolean enabled) {
        LlmMarkdownConfig current = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("llm markdown config not found"));
        LlmMarkdownConfig updated = current.withEnabled(enabled);
        repository.save(updated);
        return updated;
    }

    /**
     * 构建用于测试连通性的配置参数。
     *
     * @param settings 原始测试参数
     * @return 规整并补齐凭证后的测试参数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public LlmMarkdownConfigSettings settingsForTest(LlmMarkdownConfigSettings settings) {
        LlmMarkdownConfig current = getConfig();
        return mutationFactory.settingsForTest(current, settings);
    }

    /**
     * 校验并规整 LLM Markdown 配置参数。
     *
     * @param settings 原始配置参数
     * @return 规整后的配置参数
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public LlmMarkdownConfigSettings normalizeSettings(LlmMarkdownConfigSettings settings) {
        return mutationFactory.normalizeSettings(settings);
    }

    /**
     * 保存配置并维护同用途默认唯一性。
     *
     * @param target 目标配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private void saveWithDefaultUniqueness(LlmMarkdownConfig target) {
        // 目标配置被设置为默认时，需要清理同用途其它默认标记。
        if (target.defaultConfig()) {
            // 新默认配置保存前，先清理同用途其它默认标记。
            List<LlmMarkdownConfig> configs = withoutOtherDefaults(target);
            configs.add(target);
            repository.saveAll(configs);
        } else {
            // 非默认配置不影响同用途已有默认项。
            repository.save(target);
        }
    }

    /**
     * 生成排除目标配置外的同用途非默认配置列表。
     *
     * @param target 目标配置
     * @return 待保存配置列表
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private List<LlmMarkdownConfig> withoutOtherDefaults(LlmMarkdownConfig target) {
        List<LlmMarkdownConfig> currentConfigs = repository.listByUsage(target.usageType());
        List<LlmMarkdownConfig> configs = new ArrayList<>(currentConfigs.size());
        for (LlmMarkdownConfig config : currentConfigs) {
            // 当前目标配置由调用方携带最新内容，不使用旧值覆盖。
            if (config.id().equals(target.id())) {
            } else {
                // 同用途其它配置统一清除默认标记，保证每个用途最多一个默认配置。
                configs.add(config.withDefaultConfig(false));
            }
        }
        return configs;
    }
}
