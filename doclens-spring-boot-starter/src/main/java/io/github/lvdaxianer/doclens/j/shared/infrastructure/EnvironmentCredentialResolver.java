package io.github.lvdaxianer.doclens.j.shared.infrastructure;

import java.util.Map;
import java.util.Objects;

/**
 * 环境变量凭证解析器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
public class EnvironmentCredentialResolver {

    private final Map<String, String> environmentValues;

    /**
     * 使用当前进程环境变量创建解析器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    public EnvironmentCredentialResolver() {
        this(System.getenv());
    }

    /**
     * 使用指定环境变量映射创建解析器。
     *
     * @param environmentValues 环境变量映射
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    public EnvironmentCredentialResolver(Map<String, String> environmentValues) {
        this.environmentValues = Map.copyOf(Objects.requireNonNull(environmentValues, "environmentValues"));
    }

    /**
     * 按环境变量名解析真实凭证。
     *
     * @param credentialEnvVar 凭证环境变量名，可为空
     * @return 真实凭证，空环境变量名返回空字符串
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    public String resolve(String credentialEnvVar) {
        String envVarName = normalize(credentialEnvVar);
        // 空环境变量名用于本地无鉴权模型，保持无凭证调用。
        if (envVarName.isBlank()) {
            return "";
        } else {
            return requireConfiguredValue(envVarName);
        }
    }

    /**
     * 获取必需的环境变量值。
     *
     * @param envVarName 环境变量名
     * @return 环境变量值
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private String requireConfiguredValue(String envVarName) {
        String credential = normalize(environmentValues.get(envVarName));
        // 环境变量存在且非空时，返回真实凭证。
        if (!credential.isBlank()) {
            return credential;
        } else {
            throw new IllegalStateException("credential environment variable " + envVarName + " is not configured");
        }
    }

    /**
     * 标准化文本。
     *
     * @param value 原始文本
     * @return 标准化文本
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
