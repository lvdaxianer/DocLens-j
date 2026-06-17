package io.github.lvdaxianer.doclens.j.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/**
 * 根据请求方法和路径解析调用方流量接口组。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
@Component
public class TrafficGroupResolver {

    private static final String API_V1_PREFIX = "/api/v1";
    private static final String DASHBOARD_PREFIX = API_V1_PREFIX + "/dashboard";
    private static final String BATCHES_PATH = API_V1_PREFIX + "/batches";
    private static final String DOCUMENTS_PATH = API_V1_PREFIX + "/documents";
    private static final String OCR_GOVERNANCE_CONFIG_PATH = API_V1_PREFIX + "/ocr-governance-config";
    private static final String HEALTH_PATH = API_V1_PREFIX + "/health";
    private static final String HEARTBEAT_PATH = API_V1_PREFIX + "/heartbeat";

    /**
     * 解析请求对应的流量接口组。
     *
     * @param request HTTP 请求
     * @return 流量接口组
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public TrafficGroup resolve(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if (isAdminHealth(method, path)) {
            return TrafficGroup.ADMIN_HEALTH;
        } else if (isUploadWrite(method, path)) {
            return TrafficGroup.UPLOAD_WRITE;
        } else if (isOcrMutation(method, path)) {
            return TrafficGroup.OCR_MUTATION;
        } else if (isConfigMutation(method, path)) {
            return TrafficGroup.CONFIG_MUTATION;
        } else if (isDashboardRead(method, path)) {
            return TrafficGroup.DASHBOARD_READ;
        } else {
            return TrafficGroup.DETAIL_READ;
        }
    }

    /**
     * 判断是否为健康检查接口组。
     *
     * @param method 请求方法
     * @param path 请求路径
     * @return 是否为健康检查
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private boolean isAdminHealth(String method, String path) {
        if (HttpMethod.GET.matches(method) || HttpMethod.HEAD.matches(method)) {
            return HEALTH_PATH.equals(path) || HEARTBEAT_PATH.equals(path);
        } else {
            return false;
        }
    }

    /**
     * 判断是否为上传写入接口组。
     *
     * @param method 请求方法
     * @param path 请求路径
     * @return 是否为上传写入
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private boolean isUploadWrite(String method, String path) {
        if (HttpMethod.POST.matches(method)) {
            return BATCHES_PATH.equals(path);
        } else {
            return false;
        }
    }

    /**
     * 判断是否为 OCR 变更接口组。
     *
     * @param method 请求方法
     * @param path 请求路径
     * @return 是否为 OCR 变更
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private boolean isOcrMutation(String method, String path) {
        if (HttpMethod.POST.matches(method)) {
            return path.startsWith(DOCUMENTS_PATH + "/") && path.endsWith("/retry");
        } else if (HttpMethod.DELETE.matches(method)) {
            return path.startsWith(DOCUMENTS_PATH + "/") || path.startsWith(BATCHES_PATH + "/");
        } else {
            return false;
        }
    }

    /**
     * 判断是否为配置变更接口组。
     *
     * @param method 请求方法
     * @param path 请求路径
     * @return 是否为配置变更
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private boolean isConfigMutation(String method, String path) {
        if (HttpMethod.PUT.matches(method) || HttpMethod.POST.matches(method)
                || HttpMethod.PATCH.matches(method) || HttpMethod.DELETE.matches(method)) {
            return path.startsWith(OCR_GOVERNANCE_CONFIG_PATH)
                    || path.startsWith(API_V1_PREFIX + "/llm-markdown-config")
                    || path.startsWith(API_V1_PREFIX + "/ocr-nodes");
        } else {
            return false;
        }
    }

    /**
     * 判断是否为 Dashboard 读接口组。
     *
     * @param method 请求方法
     * @param path 请求路径
     * @return 是否为 Dashboard 读
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private boolean isDashboardRead(String method, String path) {
        if (HttpMethod.GET.matches(method)) {
            return path.startsWith(DASHBOARD_PREFIX);
        } else {
            return false;
        }
    }
}
