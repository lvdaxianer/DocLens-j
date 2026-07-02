package io.github.lvdaxianer.doclens.j.ingestion.infrastructure;

import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensSpringProperties.CallerCredentialProperties;
import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensSpringProperties.ClientsProperties;
import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;

/**
 * 兼容旧 caller 凭证解析命名的分区解析适配器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
public class CallerCredentialResolver {

    private final CallerPartitionResolver callerPartitionResolver;

    /**
     * 创建旧 caller 凭证解析适配器。
     *
     * @param clients 接入方配置，当前仅保留兼容入口
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public CallerCredentialResolver(ClientsProperties clients) {
        this.callerPartitionResolver = new CallerPartitionResolver(clients);
    }

    /**
     * 创建旧 caller 凭证解析适配器。
     *
     * @param callerPartitionResolver caller 分区键解析器
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    public CallerCredentialResolver(CallerPartitionResolver callerPartitionResolver) {
        this.callerPartitionResolver = callerPartitionResolver;
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
        return callerPartitionResolver.resolve(callerPartitionKey);
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
        CallerPartitionResolver.ResolvedCallerPartition partition =
                callerPartitionResolver.resolveWithPartition(callerPartitionKey);
        return new ResolvedCallerCredential(partition.callerIdentity(), partition.credential());
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
