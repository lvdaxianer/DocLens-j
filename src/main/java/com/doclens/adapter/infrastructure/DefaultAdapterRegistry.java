package com.doclens.adapter.infrastructure;

import com.doclens.adapter.domain.AdapterCapability;
import com.doclens.adapter.domain.OcrAdapter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * In-process OCR adapter registry.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@Component
public class DefaultAdapterRegistry {

    private final Map<String, OcrAdapter> adapters;

    /**
     * Creates adapter registry.
     *
     * @param adapters OCR adapters
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public DefaultAdapterRegistry(List<OcrAdapter> adapters) {
        this.adapters = adapters.stream()
                .collect(Collectors.toMap(adapter -> adapter.capability().adapterKey(), Function.identity()));
    }

    /**
     * Finds an adapter by key.
     *
     * @param adapterKey adapter key
     * @return optional adapter
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public Optional<OcrAdapter> find(String adapterKey) {
        return Optional.ofNullable(adapters.get(adapterKey));
    }

    /**
     * Lists adapter capabilities.
     *
     * @return adapter capabilities
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public List<AdapterCapability> listCapabilities() {
        return adapters.values().stream().map(OcrAdapter::capability).toList();
    }
}
