package io.github.lvdaxianer.doclens.j.ingestion.infrastructure;

import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensSpringProperties.CallerCredentialProperties;
import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensSpringProperties.ClientsProperties;
import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import java.util.Map;
import java.util.Optional;
import org.springframework.util.StringUtils;

/**
 * 根据原生上传请求中的 caller 分区键解析接入方身份。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
public class CallerCredentialResolver {

    private static final String DEFAULT_SOURCE_APP = "dashboard";
    private static final String MISSING_CALLER_PARTITION_KEY_MESSAGE = "missing caller partition key";

    /**
     * 创建接入方分区键解析器。
     *
     * @param clients 接入方配置，当前仅保留兼容入口
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public CallerCredentialResolver(ClientsProperties clients) {
        // caller key 是请求维度的隔离键，旧配置不再参与密钥白名单校验。
    }

    /**
     * 根据 caller 分区键解析调用方。
     *
     * @param callerPartitionKey caller 分区键请求头
     * @param authorization 旧 Authorization 参数，保留用于兼容调用签名
     * @return 调用方身份
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public CallerIdentity resolve(String callerPartitionKey, String authorization) {
        return resolve(callerPartitionKey);
    }

    /**
     * 根据 caller 分区键解析调用方。
     *
     * @param callerPartitionKey caller 分区键请求头
     * @return 调用方身份
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public CallerIdentity resolve(String callerPartitionKey) {
        String partitionKey = normalizePartitionKey(callerPartitionKey);
        return new CallerIdentity(partitionKey, DEFAULT_SOURCE_APP, Optional.of(partitionKey));
    }

    /**
     * 根据请求分区键同时返回 caller 身份与限流配置载体。
     *
     * @param callerPartitionKey caller 分区键请求头
     * @param authorization 旧 Authorization 参数，保留用于兼容调用签名
     * @return caller 身份与限流配置载体
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public ResolvedCallerCredential resolveWithCredential(String callerPartitionKey, String authorization) {
        CallerIdentity callerIdentity = resolve(callerPartitionKey);
        return new ResolvedCallerCredential(callerIdentity, toSyntheticCredential(callerIdentity));
    }

    /**
     * 规整 caller 分区键。
     *
     * @param callerPartitionKey caller 分区键请求头
     * @return 规整后的分区键
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private String normalizePartitionKey(String callerPartitionKey) {
        if (StringUtils.hasText(callerPartitionKey)) {
            return callerPartitionKey.trim();
        } else {
            // 缺少分区键时拒绝请求，避免多个匿名调用方落入同一个共享数据空间。
            throw new CallerCredentialException(MISSING_CALLER_PARTITION_KEY_MESSAGE);
        }
    }

    /**
     * 构造兼容限流策略的 caller 配置载体。
     *
     * @param callerIdentity caller 身份
     * @return caller 配置载体
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private CallerCredentialProperties toSyntheticCredential(CallerIdentity callerIdentity) {
        return new CallerCredentialProperties(callerIdentity.clientId(), callerIdentity.sourceApp(),
                callerIdentity.tenantKey().orElse(""), "", "", Map.of());
    }

    /**
     * caller 身份与限流配置载体的解析结果。
     *
     * @param callerIdentity caller 身份
     * @param credential 限流配置载体
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public record ResolvedCallerCredential(
            CallerIdentity callerIdentity,
            CallerCredentialProperties credential
    ) {
    }
}
