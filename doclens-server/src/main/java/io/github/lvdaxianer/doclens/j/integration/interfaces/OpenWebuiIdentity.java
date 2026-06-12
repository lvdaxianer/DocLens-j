package io.github.lvdaxianer.doclens.j.integration.interfaces;

/**
 * Open WebUI 内部请求身份。
 *
 * @param userId Open WebUI 用户 ID
 * @param userEmail Open WebUI 用户邮箱
 * @param userRole Open WebUI 用户角色
 * @param requestId Open WebUI 请求 ID
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
public record OpenWebuiIdentity(String userId, String userEmail, String userRole, String requestId) {
}
