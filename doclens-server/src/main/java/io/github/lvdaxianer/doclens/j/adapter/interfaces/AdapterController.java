package io.github.lvdaxianer.doclens.j.adapter.interfaces;

import io.github.lvdaxianer.doclens.j.api.AdapterCapability;
import io.github.lvdaxianer.doclens.j.api.DocLensEngine;
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

    private final DocLensEngine docLensEngine;

    /**
     * Creates adapter controller.
     *
     * @param docLensEngine DocLens engine
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public AdapterController(DocLensEngine docLensEngine) {
        this.docLensEngine = docLensEngine;
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
        return docLensEngine.listAdapters();
    }
}
