package io.github.lvdaxianer.doclens.j.adapter.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrGovernanceConfig;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrGovernanceConfigRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrHealthGovernance;
import java.time.OffsetDateTime;
import java.util.function.Supplier;

/**
 * OCR 全局治理配置应用服务。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public class OcrGovernanceConfigService {

    private final OcrGovernanceConfigRepository repository;
    private final OcrHealthGovernance defaults;
    private final Supplier<OffsetDateTime> nowSupplier;

    /**
     * 创建 OCR 全局治理配置应用服务。
     *
     * @param repository 治理配置仓储
     * @param defaults 默认治理参数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public OcrGovernanceConfigService(
            OcrGovernanceConfigRepository repository,
            OcrHealthGovernance defaults
    ) {
        this(repository, defaults, OffsetDateTime::now);
    }

    /**
     * 创建带可控时钟的 OCR 全局治理配置应用服务。
     *
     * @param repository 治理配置仓储
     * @param defaults 默认治理参数
     * @param nowSupplier 当前时间提供器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public OcrGovernanceConfigService(
            OcrGovernanceConfigRepository repository,
            OcrHealthGovernance defaults,
            Supplier<OffsetDateTime> nowSupplier
    ) {
        this.repository = repository;
        this.defaults = defaults;
        this.nowSupplier = nowSupplier;
    }

    /**
     * 查询当前治理配置，未落库时返回系统默认值。
     *
     * @return 当前治理配置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public OcrGovernanceConfig getConfig() {
        return repository.find().orElseGet(this::defaultConfig);
    }

    /**
     * 查询当前生效的治理参数。
     *
     * @return 当前治理参数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public OcrHealthGovernance currentGovernance() {
        return getConfig().toGovernance();
    }

    /**
     * 保存 OCR 全局治理配置。
     *
     * @param governance 治理参数
     * @return 保存后的治理配置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public OcrGovernanceConfig saveGovernance(OcrHealthGovernance governance) {
        OcrGovernanceConfig current = getConfig();
        OcrGovernanceConfig updated = current.update(governance, nowSupplier.get());
        repository.save(updated);
        return updated;
    }

    /**
     * 创建默认治理配置。
     *
     * @return 默认治理配置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrGovernanceConfig defaultConfig() {
        return OcrGovernanceConfig.defaults(defaults, nowSupplier.get());
    }
}
