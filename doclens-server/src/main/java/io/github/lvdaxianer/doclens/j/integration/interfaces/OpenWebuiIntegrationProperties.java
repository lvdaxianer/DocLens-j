package io.github.lvdaxianer.doclens.j.integration.interfaces;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Open WebUI 集成适配器配置。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
@Component
@ConfigurationProperties(prefix = "doclens.integrations.open-webui")
public class OpenWebuiIntegrationProperties {

    /** Open WebUI 内部调用 token。 */
    private String internalToken;

    /**
     * 获取内部调用 token。
     *
     * @return 内部调用 token
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public String getInternalToken() {
        return internalToken;
    }

    /**
     * 设置内部调用 token。
     *
     * @param internalToken 内部调用 token
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public void setInternalToken(String internalToken) {
        this.internalToken = internalToken;
    }
}
