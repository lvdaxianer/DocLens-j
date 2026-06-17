package io.github.lvdaxianer.doclens.j.shared.web;

/**
 * 调用方流量接口组定义。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
public enum TrafficGroup {

    DASHBOARD_READ("dashboard-read"),
    DETAIL_READ("detail-read"),
    UPLOAD_WRITE("upload-write"),
    OCR_MUTATION("ocr-mutation"),
    CONFIG_MUTATION("config-mutation"),
    ADMIN_HEALTH("admin-health");

    private final String value;

    /**
     * 创建接口组枚举。
     *
     * @param value 对外接口组名称
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    TrafficGroup(String value) {
        this.value = value;
    }

    /**
     * 返回对外接口组名称。
     *
     * @return 接口组名称
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public String value() {
        return value;
    }
}
