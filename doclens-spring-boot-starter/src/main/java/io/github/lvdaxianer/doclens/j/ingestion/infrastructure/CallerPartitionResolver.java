package io.github.lvdaxianer.doclens.j.ingestion.infrastructure;

import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensSpringProperties.CallerCredentialProperties;
import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensSpringProperties.ClientsProperties;
import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import java.util.Map;
import java.util.Optional;
import org.springframework.util.StringUtils;

/**
 * 根据请求中的 caller 分区键解析数据隔离身份。
 *
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
public class CallerPartitionResolver {

    private static final String DEFAULT_SOURCE_APP = "dashboard";
    private static final String MISSING_CALLER_PARTITION_KEY_MESSAGE = "missing caller partition key";

    /**
     * 创建 caller 分区键解析器。
     *
     * @param clients 旧接入方配置，当前仅保留兼容入口
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    public CallerPartitionResolver(ClientsProperties clients) {
        // caller 分区键只做数据隔离，旧凭证配置不参与身份认证或白名单校验。
    }

    /**
     * 根据 caller 分区键解析调用方。
     *
     * @param callerPartitionKey caller 分区键请求头
     * @return 调用方身份
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    public CallerIdentity resolve(String callerPartitionKey) {
        String partitionKey = normalizePartitionKey(callerPartitionKey);
        return new CallerIdentity(partitionKey, DEFAULT_SOURCE_APP, Optional.of(partitionKey));
    }

    /**
     * 根据请求分区键同时返回 caller 身份与限流配置载体。
     *
     * @param callerPartitionKey caller 分区键请求头
     * @return caller 身份与限流配置载体
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    public ResolvedCallerPartition resolveWithPartition(String callerPartitionKey) {
        CallerIdentity callerIdentity = resolve(callerPartitionKey);
        return new ResolvedCallerPartition(callerIdentity, toSyntheticCredential(callerIdentity));
    }

    /**
     * 规整 caller 分区键。
     *
     * @param callerPartitionKey caller 分区键请求头
     * @return 规整后的分区键
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private String normalizePartitionKey(String callerPartitionKey) {
        // 请求头存在有效分区键时，使用去空白后的值作为数据隔离键。
        if (StringUtils.hasText(callerPartitionKey)) {
            return callerPartitionKey.trim();
        } else {
            // 缺少分区键时拒绝请求，避免多个调用方落入同一个共享数据空间。
            throw new CallerCredentialException(MISSING_CALLER_PARTITION_KEY_MESSAGE);
        }
    }

    /**
     * 构造兼容限流策略的 caller 配置载体。
     *
     * @param callerIdentity caller 身份
     * @return caller 配置载体
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private CallerCredentialProperties toSyntheticCredential(CallerIdentity callerIdentity) {
        return new CallerCredentialProperties(callerIdentity.clientId(), callerIdentity.sourceApp(),
                callerIdentity.tenantKey().orElse(""), "", "", Map.of());
    }

    /**
     * caller 身份与分区限流载体的解析结果。
     *
     * @param callerIdentity caller 身份
     * @param credential 限流配置载体
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    public record ResolvedCallerPartition(
            CallerIdentity callerIdentity,
            CallerCredentialProperties credential
    ) {
    }
}
