package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownApiType;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmUsageType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * LLM Markdown 配置仓储 MyBatis-Plus 实现。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
@Repository
public class MybatisPlusLlmMarkdownConfigRepository
        extends ServiceImpl<LlmMarkdownConfigMapper, LlmMarkdownConfigEntity>
        implements LlmMarkdownConfigRepository {

    private static final String CONFIG_LIST_LIMIT_SQL = "LIMIT 1000";

    /**
     * 查询当前 LLM Markdown 配置。
     *
     * @return 当前配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Override
    public Optional<LlmMarkdownConfig> find() {
        List<LlmMarkdownConfig> configs = listConfigs();
        return configs.stream().filter(LlmMarkdownConfig::defaultConfig).findFirst()
                .or(() -> configs.stream().findFirst());
    }

    /**
     * 查询全部 LLM Markdown 配置。
     *
     * @return 配置列表
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Override
    public List<LlmMarkdownConfig> listConfigs() {
        return lambdaQuery().orderByAsc(LlmMarkdownConfigEntity::getPriority)
                .orderByAsc(LlmMarkdownConfigEntity::getCreatedAt)
                .last(CONFIG_LIST_LIMIT_SQL)
                .list()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    /**
     * 按用途查询 LLM Markdown 配置。
     *
     * @param usageType 配置用途
     * @return 配置列表
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Override
    public List<LlmMarkdownConfig> listByUsage(LlmUsageType usageType) {
        return lambdaQuery().eq(LlmMarkdownConfigEntity::getUsageType, usageType.name())
                .orderByAsc(LlmMarkdownConfigEntity::getPriority)
                .orderByAsc(LlmMarkdownConfigEntity::getCreatedAt)
                .last(CONFIG_LIST_LIMIT_SQL)
                .list()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    /**
     * 按 ID 查询 LLM Markdown 配置。
     *
     * @param id 配置 ID
     * @return 配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Override
    public Optional<LlmMarkdownConfig> findById(String id) {
        return Optional.ofNullable(getById(id)).map(this::toDomain);
    }

    /**
     * 保存 LLM Markdown 配置。
     *
     * @param config LLM Markdown 配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Override
    public void save(LlmMarkdownConfig config) {
        saveOrUpdate(toEntity(config));
    }

    /**
     * 批量保存 LLM Markdown 配置。
     *
     * @param configs 配置列表
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Override
    public void saveAll(List<LlmMarkdownConfig> configs) {
        saveOrUpdateBatch(configs.stream().map(this::toEntity).toList());
    }

    /**
     * 删除 LLM Markdown 配置。
     *
     * @param id 配置 ID
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Override
    public void deleteById(String id) {
        removeById(id);
    }

    /**
     * 更新 LLM Markdown 健康状态。
     *
     * @param config LLM Markdown 配置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public void updateHealth(LlmMarkdownConfig config) {
        saveOrUpdate(toEntity(config));
    }

    /**
     * 将领域配置转换为持久化实体。
     *
     * @param config LLM Markdown 配置
     * @return 持久化实体
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private LlmMarkdownConfigEntity toEntity(LlmMarkdownConfig config) {
        LlmMarkdownConfigEntity entity = new LlmMarkdownConfigEntity();
        entity.setId(config.id());
        entity.setName(config.name());
        entity.setApiType(config.apiType().value());
        entity.setUrl(config.url());
        entity.setModel(config.model());
        entity.setCredentialRef(config.credentialRef().orElse(null));
        entity.setCredentialConfigured(config.credentialConfigured());
        entity.setUsageType(config.usageType().name());
        entity.setPriority(config.priority());
        entity.setDefaultFlag(config.defaultConfig());
        entity.setEnabled(config.enabled());
        entity.setHealthy(config.healthy());
        entity.setHealthMessage(config.healthMessage());
        entity.setLastHealthAt(config.lastHealthAt().orElse(null));
        entity.setCreatedAt(config.createdAt());
        entity.setUpdatedAt(config.updatedAt());
        return entity;
    }

    /**
     * 将持久化实体转换为领域配置。
     *
     * @param entity 持久化实体
     * @return LLM Markdown 配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private LlmMarkdownConfig toDomain(LlmMarkdownConfigEntity entity) {
        return LlmMarkdownConfig.builder(entity.getId(), normalizeName(entity.getName()),
                        LlmMarkdownApiType.from(entity.getApiType()))
                .endpoint(entity.getUrl(), entity.getModel())
                .credential(entity.getCredentialRef())
                .usage(LlmUsageType.from(entity.getUsageType()), entity.getPriority())
                .defaultConfig(entity.isDefaultFlag())
                .enabled(entity.isEnabled())
                .healthy(entity.isHealthy())
                .healthMessage(normalize(entity.getHealthMessage()))
                .lastHealthAt(Optional.ofNullable(entity.getLastHealthAt()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * 标准化配置名称。
     *
     * @param value 原始名称
     * @return 配置名称
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private String normalizeName(String value) {
        return normalize(value).isBlank() ? "默认 LLM 配置" : normalize(value);
    }

    /**
     * 标准化可选文本。
     *
     * @param value 原始文本
     * @return 标准化文本
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private String normalize(String value) {
        return value == null ? "" : value;
    }
}
