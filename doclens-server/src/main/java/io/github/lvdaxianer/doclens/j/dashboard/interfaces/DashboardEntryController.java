package io.github.lvdaxianer.doclens.j.dashboard.interfaces;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Dashboard 静态入口控制器。
 *
 * @author lvdaxianer@yeah.net
 * @date 2026-07-19
 */
@Controller
public class DashboardEntryController {

    private static final String DASHBOARD_INDEX_FORWARD = "forward:/dashboard/index.html";

    /**
     * 将 dashboard 根路径转发到打包后的静态入口页。
     *
     * @return dashboard 静态入口页转发
     * @author lvdaxianer@yeah.net
     * @date 2026-07-19
     */
    @GetMapping({"/dashboard", "/dashboard/"})
    public String dashboardRoot() {
        return DASHBOARD_INDEX_FORWARD;
    }
}
