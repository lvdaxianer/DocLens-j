package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownApiType;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigRepository;
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

    /**
     * 查询当前 LLM Markdown 配置。
     *
     * @return 当前配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Override
    public Optional<LlmMarkdownConfig> find() {
        return Optional.ofNullable(getById(LlmMarkdownConfig.SINGLETON_ID)).map(this::toDomain);
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
        entity.setApiType(config.apiType().value());
        entity.setUrl(config.url());
        entity.setModel(config.model());
        entity.setCredentialRef(config.credentialRef().orElse(null));
        entity.setCredentialConfigured(config.credentialConfigured());
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
        return new LlmMarkdownConfig(entity.getId(), LlmMarkdownApiType.from(entity.getApiType()),
                entity.getUrl(), entity.getModel(), Optional.ofNullable(entity.getCredentialRef()),
                entity.isCredentialConfigured(), entity.isHealthy(), normalize(entity.getHealthMessage()),
                Optional.ofNullable(entity.getLastHealthAt()),
                entity.getCreatedAt(), entity.getUpdatedAt());
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
