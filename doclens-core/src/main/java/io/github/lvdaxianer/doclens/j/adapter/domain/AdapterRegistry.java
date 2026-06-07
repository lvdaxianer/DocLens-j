package io.github.lvdaxianer.doclens.j.adapter.domain;

import io.github.lvdaxianer.doclens.j.api.AdapterCapability;
import java.util.List;
import java.util.Optional;

/**
 * Registry port for OCR adapters.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public interface AdapterRegistry {

    /**
     * Finds an OCR adapter by key.
     *
     * @param adapterKey adapter key
     * @return optional OCR adapter
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    Optional<OcrAdapter> find(String adapterKey);

    /**
     * Lists public adapter capabilities.
     *
     * @return adapter capabilities
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    List<AdapterCapability> listCapabilities();
}
