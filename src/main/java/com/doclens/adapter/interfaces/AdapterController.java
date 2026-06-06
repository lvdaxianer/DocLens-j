package com.doclens.adapter.interfaces;

import com.doclens.adapter.domain.AdapterCapability;
import com.doclens.adapter.infrastructure.DefaultAdapterRegistry;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adapter capability API controller.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@RestController
@RequestMapping("/api/v1")
public class AdapterController {

    private final DefaultAdapterRegistry adapterRegistry;

    /**
     * Creates adapter controller.
     *
     * @param adapterRegistry adapter registry
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public AdapterController(DefaultAdapterRegistry adapterRegistry) {
        this.adapterRegistry = adapterRegistry;
    }

    /**
     * Lists OCR adapter capabilities.
     *
     * @return adapter response
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @GetMapping("/adapters")
    public Map<String, List<AdapterCapability>> listAdapters() {
        return Map.of("adapters", adapterRegistry.listCapabilities());
    }
}
