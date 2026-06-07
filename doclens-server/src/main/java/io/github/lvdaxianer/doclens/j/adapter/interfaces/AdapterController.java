package io.github.lvdaxianer.doclens.j.adapter.interfaces;

import io.github.lvdaxianer.doclens.j.api.AdapterCapability;
import io.github.lvdaxianer.doclens.j.api.DocLensEngine;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 适配器能力 API 控制器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@RestController
@RequestMapping("/api/v1")
public class AdapterController {

    private final DocLensEngine docLensEngine;

    /**
     * 创建适配器控制器。
     *
     * @param docLensEngine DocLens 引擎
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public AdapterController(DocLensEngine docLensEngine) {
        this.docLensEngine = docLensEngine;
    }

    /**
     * 列出 OCR 适配器能力。
     *
     * @return 适配器响应
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @GetMapping("/adapters")
    public Map<String, List<AdapterCapability>> listAdapters() {
        return docLensEngine.listAdapters();
    }
}
