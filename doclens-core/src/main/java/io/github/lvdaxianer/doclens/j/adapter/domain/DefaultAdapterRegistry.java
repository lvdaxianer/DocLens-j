package io.github.lvdaxianer.doclens.j.adapter.domain;

import io.github.lvdaxianer.doclens.j.api.AdapterCapability;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 进程内 OCR 适配器注册表。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public class DefaultAdapterRegistry implements AdapterRegistry {

    private final Map<String, OcrAdapter> adapters;

    /**
     * 创建适配器注册表。
     *
     * @param adapters OCR 适配器集合
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public DefaultAdapterRegistry(List<OcrAdapter> adapters) {
        this.adapters = adapters.stream()
                .collect(Collectors.toMap(adapter -> adapter.capability().adapterKey(), Function.identity()));
    }

    @Override
    public Optional<OcrAdapter> find(String adapterKey) {
        return Optional.ofNullable(adapters.get(adapterKey));
    }

    @Override
    public List<AdapterCapability> listCapabilities() {
        return adapters.values().stream().map(OcrAdapter::capability).toList();
    }
}
