package io.github.lvdaxianer.doclens.j.ingestion.infrastructure;

import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensSpringProperties.CallerCredentialProperties;
import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensSpringProperties.ClientsProperties;
import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import java.util.List;
import java.util.Optional;
import org.springframework.util.StringUtils;

/**
 * 根据原生上传请求凭证解析接入方身份。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
public class CallerCredentialResolver {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String UNAUTHORIZED_MESSAGE = "unauthorized caller credential";

    private final List<CallerCredentialProperties> credentials;

    /**
     * 创建接入方凭证解析器。
     *
     * @param clients 接入方配置
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public CallerCredentialResolver(ClientsProperties clients) {
        this.credentials = clients == null ? List.of() : clients.credentials();
        validateCredentials();
    }

    /**
     * 根据 API Key 或 Bearer Token 解析调用方。
     *
     * @param apiKey API Key 请求头
     * @param authorization Authorization 请求头
     * @return 调用方身份
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public CallerIdentity resolve(String apiKey, String authorization) {
        return matchCredential(apiKey, authorization)
                .map(this::toCallerIdentity)
                .orElseThrow(() -> new CallerCredentialException(UNAUTHORIZED_MESSAGE));
    }

    /**
     * 匹配配置凭证。
     *
     * @param apiKey API Key 请求头
     * @param authorization Authorization 请求头
     * @return 匹配到的配置凭证
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private Optional<CallerCredentialProperties> matchCredential(String apiKey, String authorization) {
        return credentials.stream()
                .filter(credential -> matchesApiKey(credential, apiKey) || matchesBearer(credential, authorization))
                .findFirst();
    }

    /**
     * 校验配置凭证具备可解析身份和至少一种密钥。
     *
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private void validateCredentials() {
        credentials.forEach(this::validateCredential);
    }

    /**
     * 校验单个配置凭证。
     *
     * @param credential 配置凭证
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private void validateCredential(CallerCredentialProperties credential) {
        if (!StringUtils.hasText(credential.clientId()) || !StringUtils.hasText(credential.sourceApp())) {
            throw new IllegalArgumentException("caller credential client-id and source-app are required");
        } else if (!StringUtils.hasText(credential.apiKey()) && !StringUtils.hasText(credential.bearerToken())) {
            throw new IllegalArgumentException("caller credential api-key or bearer-token is required");
        } else {
            // 配置凭证具备可用身份与至少一种匹配密钥。
        }
    }

    /**
     * 判断 API Key 是否匹配。
     *
     * @param credential 配置凭证
     * @param apiKey 请求 API Key
     * @return 是否匹配
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private boolean matchesApiKey(CallerCredentialProperties credential, String apiKey) {
        if (StringUtils.hasText(credential.apiKey())) {
            return credential.apiKey().equals(apiKey);
        } else {
            return false;
        }
    }

    /**
     * 判断 Bearer Token 是否匹配。
     *
     * @param credential 配置凭证
     * @param authorization Authorization 请求头
     * @return 是否匹配
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private boolean matchesBearer(CallerCredentialProperties credential, String authorization) {
        if (StringUtils.hasText(credential.bearerToken()) && StringUtils.hasText(authorization)) {
            return authorization.equals(BEARER_PREFIX + credential.bearerToken());
        } else {
            return false;
        }
    }

    /**
     * 将配置凭证转换为调用方身份。
     *
     * @param credential 配置凭证
     * @return 调用方身份
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private CallerIdentity toCallerIdentity(CallerCredentialProperties credential) {
        return new CallerIdentity(credential.clientId(), credential.sourceApp(),
                Optional.ofNullable(credential.tenantKey()));
    }
}
