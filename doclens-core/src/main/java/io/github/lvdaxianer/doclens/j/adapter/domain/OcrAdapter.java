package io.github.lvdaxianer.doclens.j.adapter.domain;

import io.github.lvdaxianer.doclens.j.api.AdapterCapability;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import java.util.Map;

/**
 * OCR adapter anti-corruption interface.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public interface OcrAdapter {

    /**
     * Returns adapter capability.
     *
     * @return adapter capability
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    AdapterCapability capability();

    /**
     * Parses a document job into a vendor-neutral raw output.
     *
     * @param document document job
     * @return vendor output
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    Map<String, Object> parse(DocumentJob document);
}
