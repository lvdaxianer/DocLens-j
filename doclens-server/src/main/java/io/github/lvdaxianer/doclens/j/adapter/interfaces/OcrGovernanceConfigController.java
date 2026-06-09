package io.github.lvdaxianer.doclens.j.adapter.interfaces;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrGovernanceConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OCR 全局治理配置接口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/api/v1/ocr-governance-config")
public class OcrGovernanceConfigController {

    private final OcrGovernanceConfigService configService;

    /**
     * 创建 OCR 全局治理配置接口。
     *
     * @param configService OCR 全局治理配置服务
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public OcrGovernanceConfigController(OcrGovernanceConfigService configService) {
        this.configService = configService;
    }

    /**
     * 查询 OCR 全局治理配置。
     *
     * @return OCR 全局治理配置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @GetMapping
    public OcrGovernanceConfigResponse getConfig() {
        return OcrGovernanceConfigResponse.from(configService.getConfig());
    }

    /**
     * 保存 OCR 全局治理配置。
     *
     * @param request OCR 全局治理配置请求
     * @return 更新后的 OCR 全局治理配置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @PutMapping
    public OcrGovernanceConfigResponse updateConfig(@RequestBody OcrGovernanceConfigRequest request) {
        return OcrGovernanceConfigResponse.from(configService.saveGovernance(request.toGovernance()));
    }
}
