package io.github.lvdaxianer.doclens.j.adapter.domain;

import io.github.lvdaxianer.doclens.j.api.AdapterCapability;
import java.util.List;
import java.util.Optional;

/**
 * OCR 适配器注册表端口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public interface AdapterRegistry {

    /**
     * 根据键查找 OCR 适配器。
     *
     * @param adapterKey 适配器键
     * @return 可选 OCR 适配器
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    Optional<OcrAdapter> find(String adapterKey);

    /**
     * 列出公开的适配器能力。
     *
     * @return 适配器能力集合
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    List<AdapterCapability> listCapabilities();
}
