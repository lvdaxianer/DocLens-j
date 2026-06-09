package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrGovernanceConfig;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrGovernanceConfigRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * OCR 全局治理配置仓储 MyBatis-Plus 实现。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
@Repository
public class MybatisPlusOcrGovernanceConfigRepository
        extends ServiceImpl<OcrGovernanceConfigMapper, OcrGovernanceConfigEntity>
        implements OcrGovernanceConfigRepository {

    /**
     * 查询当前 OCR 全局治理配置。
     *
     * @return 当前治理配置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public Optional<OcrGovernanceConfig> find() {
        return Optional.ofNullable(getById(OcrGovernanceConfig.SINGLETON_ID)).map(this::toDomain);
    }

    /**
     * 保存 OCR 全局治理配置。
     *
     * @param config 治理配置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public void save(OcrGovernanceConfig config) {
        saveOrUpdate(toEntity(config));
    }

    /**
     * 将领域治理配置转换为持久化实体。
     *
     * @param config 领域治理配置
     * @return 持久化实体
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrGovernanceConfigEntity toEntity(OcrGovernanceConfig config) {
        OcrGovernanceConfigEntity entity = new OcrGovernanceConfigEntity();
        entity.setId(config.id());
        entity.setFailureThreshold(config.failureThreshold());
        entity.setProbeIntervalSeconds(config.probeIntervalSeconds());
        entity.setCircuitOpenSeconds(config.circuitOpenSeconds());
        entity.setRecoverySuccessThreshold(config.recoverySuccessThreshold());
        entity.setManualRecoveryAttempts(config.manualRecoveryAttempts());
        entity.setCreatedAt(config.createdAt());
        entity.setUpdatedAt(config.updatedAt());
        return entity;
    }

    /**
     * 将持久化实体转换为领域治理配置。
     *
     * @param entity 持久化实体
     * @return 领域治理配置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrGovernanceConfig toDomain(OcrGovernanceConfigEntity entity) {
        return new OcrGovernanceConfig(entity.getId(), entity.getFailureThreshold(),
                entity.getProbeIntervalSeconds(), entity.getCircuitOpenSeconds(),
                entity.getRecoverySuccessThreshold(), entity.getManualRecoveryAttempts(), entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
