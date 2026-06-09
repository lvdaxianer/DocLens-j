package io.github.lvdaxianer.doclens.j.adapter.domain;

import java.util.Optional;

/**
 * OCR 全局治理配置仓储接口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public interface OcrGovernanceConfigRepository {

    /**
     * 查询当前 OCR 全局治理配置。
     *
     * @return 当前治理配置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    Optional<OcrGovernanceConfig> find();

    /**
     * 保存 OCR 全局治理配置。
     *
     * @param config 治理配置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    void save(OcrGovernanceConfig config);
}
