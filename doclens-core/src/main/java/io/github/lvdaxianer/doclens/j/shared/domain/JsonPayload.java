package io.github.lvdaxianer.doclens.j.shared.domain;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 以 Map 表示的不可变 JSON 对象载荷。
 *
 * @param values JSON 对象值
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record JsonPayload(Map<String, Object> values) {

    /**
     * 创建 JSON 载荷并复制输入值。
     *
     * @param values JSON 对象值
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public JsonPayload {
        if (values == null) {
            values = new LinkedHashMap<>();
        } else {
            values = new LinkedHashMap<>(values);
        }
    }

    /**
     * 创建空载荷。
     *
     * @return 空 JSON 载荷
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public static JsonPayload empty() {
        return new JsonPayload(Map.of());
    }
}
