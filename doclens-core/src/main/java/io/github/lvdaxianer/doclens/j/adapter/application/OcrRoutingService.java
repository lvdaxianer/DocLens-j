package io.github.lvdaxianer.doclens.j.adapter.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallStatus;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutingMode;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OCR 图片请求统一路由服务。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public class OcrRoutingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OcrRoutingService.class);
    private static final String OCR_FAILED_CODE = "OCR_ROUTE_FAILED";
    private static final int MIN_EXCLUDED_NODE_CAPACITY = 1;

    private final OcrDispatchCoordinator dispatchCoordinator;
    private final OcrNodeImageExecutor nodeExecutor;
    private final OcrNodeCallRecorder callRecorder;
    private final OcrBatchHitTracker batchHitTracker;
    private final OcrRoutingServiceProperties properties;
    private final OcrDocumentAffinityTracker documentAffinityTracker;

    /**
     * 创建 OCR 路由服务。
     *
     * @param dependencies OCR 路由服务依赖
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public OcrRoutingService(OcrRoutingDependencies dependencies) {
        this.dispatchCoordinator = dependencies.dispatchCoordinator();
        this.nodeExecutor = dependencies.nodeExecutor();
        this.callRecorder = new OcrNodeCallRecorder(dependencies.callRepository(), dependencies.callIdGenerator());
        this.batchHitTracker = dependencies.batchHitTracker();
        this.properties = dependencies.properties();
        this.documentAffinityTracker = dependencies.documentAffinityTracker();
    }

    /**
     * 根据路由策略识别单张图片。
     *
     * @param request 图片 OCR 请求
     * @param requestedPolicy 请求路由策略
     * @return OCR 路由执行结果
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public OcrRouteExecutionResult recognize(ImageOcrRequest request, OcrRoutePolicy requestedPolicy) {
        OffsetDateTime startedAt = OffsetDateTime.now();
        OcrRoutePolicy policy = documentAffinityPolicy(request, effectivePolicy(requestedPolicy));
        Set<String> excludedNodeIds = new HashSet<>(Math.max(MIN_EXCLUDED_NODE_CAPACITY,
                properties.requestRetryTimes()));
        OcrRouteAccumulator accumulator = new OcrRouteAccumulator(startedAt);
        return routeUntilSuccess(request, policy, excludedNodeIds, accumulator);
    }

    /**
     * 释放文档级模型亲和力。
     *
     * @param documentId 文档 ID
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public void releaseDocumentAffinity(String documentId) {
        documentAffinityTracker.release(documentId);
    }

    /**
     * 持续选择候选节点直到成功或候选耗尽。
     *
     * @param request 图片 OCR 请求
     * @param policy 有效路由策略
     * @param excludedNodeIds 已失败节点 ID
     * @param accumulator 路由累计状态
     * @return OCR 路由执行结果
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private OcrRouteExecutionResult routeUntilSuccess(
            ImageOcrRequest request,
            OcrRoutePolicy policy,
            Set<String> excludedNodeIds,
            OcrRouteAccumulator accumulator
    ) {
        return executeSelectedNode(request, policy, excludedNodeIds, accumulator);
    }

    /**
     * 执行已选中的 OCR 节点。
     *
     * @param request 图片 OCR 请求
     * @param policy 有效路由策略
     * @param node 选中节点
     * @param excludedNodeIds 已失败节点 ID
     * @param accumulator 路由累计状态
     * @return OCR 路由执行结果
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private OcrRouteExecutionResult executeSelectedNode(
            ImageOcrRequest request,
            OcrRoutePolicy policy,
            Set<String> excludedNodeIds,
            OcrRouteAccumulator accumulator
    ) {
        OcrDispatchAcquireResult acquireResult = dispatchCoordinator.acquire(request, policy, excludedNodeIds);
        OcrRuntimeNodeView node = dispatchedNode(acquireResult);
        OcrRoutePolicy effectivePolicy = confirmedAffinityPolicy(request, policy, node);
        if (!node.modelKey().equals(effectivePolicy.modelKey().orElse(node.modelKey()))) {
            dispatchCoordinator.release(node.nodeId());
            return routeUntilSuccess(request, effectivePolicy, excludedNodeIds, accumulator);
        } else {
            // 选中节点与文档最终绑定模型一致，继续执行 OCR。
        }
        return executeConfirmedNode(request, effectivePolicy, excludedNodeIds, accumulator, node);
    }

    /**
     * 执行已通过文档模型亲和力确认的 OCR 节点。
     *
     * @param request 图片 OCR 请求
     * @param effectivePolicy 文档亲和力确认后的策略
     * @param excludedNodeIds 已失败节点 ID
     * @param accumulator 路由累计状态
     * @param node 选中节点
     * @return OCR 路由执行结果
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private OcrRouteExecutionResult executeConfirmedNode(
            ImageOcrRequest request,
            OcrRoutePolicy effectivePolicy,
            Set<String> excludedNodeIds,
            OcrRouteAccumulator accumulator,
            OcrRuntimeNodeView node
    ) {
        OcrBatchNodeHitCommand hitCommand = hitCommand(request, node);
        batchHitTracker.recordDispatch(hitCommand);
        try {
            NodeAttemptResult result = executeWithRetry(request, effectivePolicy, node, accumulator);
            if (result.result().isPresent()) {
                return result.result().get();
            } else if (canFailover(effectivePolicy)) {
                excludedNodeIds.add(node.nodeId());
                return routeUntilSuccess(request, effectivePolicy, excludedNodeIds, accumulator);
            } else {
                throw routeException(accumulator.lastFailure());
            }
        } finally {
            batchHitTracker.recordCompletion(hitCommand);
            dispatchCoordinator.release(node.nodeId());
        }
    }

    /**
     * 创建 OCR 批次运行中节点命中计数命令。
     *
     * @param request 图片 OCR 请求
     * @param node 选中节点
     * @return 运行中节点命中计数命令
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private OcrBatchNodeHitCommand hitCommand(ImageOcrRequest request, OcrRuntimeNodeView node) {
        return new OcrBatchNodeHitCommand(request.batchId(), request.documentId(), node.modelKey(), node.nodeId());
    }

    /**
     * 在同一节点上按配置重试。
     *
     * @param request 图片 OCR 请求
     * @param policy 有效路由策略
     * @param node 选中节点
     * @param accumulator 路由累计状态
     * @return 节点尝试结果
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private NodeAttemptResult executeWithRetry(
            ImageOcrRequest request,
            OcrRoutePolicy policy,
            OcrRuntimeNodeView node,
            OcrRouteAccumulator accumulator
    ) {
        int maxAttempts = Math.max(1, properties.requestRetryTimes());
        for (int attemptIndex = 1; attemptIndex <= maxAttempts; attemptIndex++) {
            Optional<OcrRouteExecutionResult> result = tryOnce(request, policy, node, attemptIndex, accumulator);
            if (result.isPresent()) {
                return new NodeAttemptResult(result);
            } else {
                // 当前节点本次尝试失败，继续同节点重试或由外层决定故障转移。
            }
        }
        saveFailureCall(request, policy, node, accumulator);
        return new NodeAttemptResult(Optional.empty());
    }

    /**
     * 执行单次 OCR 节点调用。
     *
     * @param request 图片 OCR 请求
     * @param policy 有效路由策略
     * @param node 选中节点
     * @param attemptIndex 当前尝试序号
     * @param accumulator 路由累计状态
     * @return 成功时返回路由结果
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private Optional<OcrRouteExecutionResult> tryOnce(
            ImageOcrRequest request,
            OcrRoutePolicy policy,
            OcrRuntimeNodeView node,
            int attemptIndex,
            OcrRouteAccumulator accumulator
    ) {
        try {
            ImageOcrResult imageResult = nodeExecutor.recognize(node, request);
            return Optional.of(successResult(request, policy, node, imageResult, accumulator));
        } catch (RuntimeException ex) {
            accumulator.recordFailure(ex);
            logNodeFailure(node, attemptIndex, ex);
            return Optional.empty();
        }
    }

    /**
     * 构建成功路由结果并写入调用记录。
     *
     * @param request 图片 OCR 请求
     * @param policy 有效路由策略
     * @param node 命中节点
     * @param imageResult 图片 OCR 结果
     * @param accumulator 路由累计状态
     * @return OCR 路由执行结果
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private OcrRouteExecutionResult successResult(
            ImageOcrRequest request,
            OcrRoutePolicy policy,
            OcrRuntimeNodeView node,
            ImageOcrResult imageResult,
            OcrRouteAccumulator accumulator
    ) {
        OcrRouteExecutionResult result = new OcrRouteExecutionResult(imageResult, node.modelKey(), node.nodeId(),
                accumulator.elapsedMs(), accumulator.retryCount());
        saveSuccessCall(request, policy, node, result, accumulator.startedAt());
        return result;
    }

    /**
     * 解析派发结果中的最终节点。
     *
     * @param acquireResult 派发占槽结果
     * @return 最终命中的节点
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrRuntimeNodeView dispatchedNode(OcrDispatchAcquireResult acquireResult) {
        if (acquireResult.queued()) {
            return acquireResult.awaitDispatch();
        } else {
            return acquireResult.node().orElseThrow(() -> new OcrRouteExecutionException("no healthy ocr candidates"));
        }
    }

    /**
     * 保存成功调用记录。
     *
     * @param request 图片 OCR 请求
     * @param policy 有效路由策略
     * @param node 命中节点
     * @param result 路由结果
     * @param startedAt 路由开始时间
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private void saveSuccessCall(
            ImageOcrRequest request,
            OcrRoutePolicy policy,
            OcrRuntimeNodeView node,
            OcrRouteExecutionResult result,
            OffsetDateTime startedAt
    ) {
        callRecorder.save(new OcrNodeCallRecordRequest(request, policy, node, OcrNodeCallStatus.SUCCESS,
                result.retryCount(), result.elapsedMs(), Optional.empty(), Optional.empty(), startedAt));
    }

    /**
     * 保存失败调用记录。
     *
     * @param request 图片 OCR 请求
     * @param policy 有效路由策略
     * @param node 失败节点
     * @param accumulator 路由累计状态
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private void saveFailureCall(
            ImageOcrRequest request,
            OcrRoutePolicy policy,
            OcrRuntimeNodeView node,
            OcrRouteAccumulator accumulator
    ) {
        callRecorder.save(new OcrNodeCallRecordRequest(request, policy, node, OcrNodeCallStatus.FAILED,
                accumulator.retryCount(), accumulator.elapsedMs(), Optional.of(OCR_FAILED_CODE),
                accumulator.lastFailure().map(Throwable::getMessage), accumulator.startedAt()));
    }

    /**
     * 解析默认策略。
     *
     * @param requestedPolicy 请求策略
     * @return 有效策略
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private OcrRoutePolicy effectivePolicy(OcrRoutePolicy requestedPolicy) {
        if (requestedPolicy.routingMode() == OcrRoutingMode.DEFAULT) {
            return properties.defaultPolicy();
        } else {
            return requestedPolicy;
        }
    }

    /**
     * 根据文档已有亲和力收窄路由策略。
     *
     * @param request 图片 OCR 请求
     * @param policy 有效路由策略
     * @return 文档亲和力收窄后的策略
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private OcrRoutePolicy documentAffinityPolicy(ImageOcrRequest request, OcrRoutePolicy policy) {
        Optional<String> boundModelKey = documentAffinityTracker.boundModelKey(request.documentId());
        if (boundModelKey.isPresent()) {
            return boundPolicy(boundModelKey.get(), policy);
        } else {
            return policy;
        }
    }

    /**
     * 根据已绑定模型生成路由策略。
     *
     * @param boundModelKey 已绑定模型标识
     * @param policy 原策略
     * @return 收窄后的路由策略
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private OcrRoutePolicy boundPolicy(String boundModelKey, OcrRoutePolicy policy) {
        if (policy.routingMode() == OcrRoutingMode.SPECIFIC_NODE
                && policy.modelKey().filter(boundModelKey::equals).isPresent()) {
            return policy;
        } else {
            return modelPolicy(boundModelKey, policy);
        }
    }

    /**
     * 确认节点命中后的文档最终模型亲和力。
     *
     * @param request 图片 OCR 请求
     * @param policy 当前策略
     * @param node 已占槽节点
     * @return 最终绑定模型对应的策略
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private OcrRoutePolicy confirmedAffinityPolicy(
            ImageOcrRequest request,
            OcrRoutePolicy policy,
            OcrRuntimeNodeView node
    ) {
        String boundModelKey = documentAffinityTracker.bindIfAbsent(request.documentId(), node.modelKey());
        if (!boundModelKey.equals(node.modelKey())) {
            return modelPolicy(boundModelKey, policy);
        } else if (policy.routingMode() == OcrRoutingMode.SPECIFIC_NODE) {
            return policy;
        } else {
            return modelPolicy(boundModelKey, policy);
        }
    }

    /**
     * 创建指定模型负载均衡策略并保留原负载均衡算法。
     *
     * @param modelKey 模型标识
     * @param policy 原策略
     * @return 指定模型策略
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private OcrRoutePolicy modelPolicy(String modelKey, OcrRoutePolicy policy) {
        return OcrRoutePolicy.modelLoadBalance(modelKey,
                policy.loadBalanceStrategy().orElse(OcrRoutingServiceProperties.DEFAULT_LOAD_BALANCE_STRATEGY));
    }

    /**
     * 判断当前策略是否允许故障转移。
     *
     * @param policy 有效路由策略
     * @return 是否允许故障转移
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private boolean canFailover(OcrRoutePolicy policy) {
        if (policy.routingMode() == OcrRoutingMode.SPECIFIC_NODE) {
            return properties.specificNodeFallbackEnabled();
        } else {
            return true;
        }
    }

    /**
     * 构建路由异常。
     *
     * @param cause 原始异常
     * @return OCR 路由执行异常
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private OcrRouteExecutionException routeException(Optional<RuntimeException> cause) {
        if (cause.isPresent()) {
            return new OcrRouteExecutionException("no healthy ocr candidates", cause.get());
        } else {
            return new OcrRouteExecutionException("no healthy ocr candidates");
        }
    }

    /**
     * 记录 OCR 节点失败日志。
     *
     * @param node OCR 节点
     * @param attemptIndex 尝试序号
     * @param ex 失败异常
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private void logNodeFailure(OcrRuntimeNodeView node, int attemptIndex, RuntimeException ex) {
        LOGGER.warn("[OCR路由] 节点调用失败, modelKey={}, nodeId={}, attempt={}, error={}",
                node.modelKey(), node.nodeId(), attemptIndex, ex.getMessage(), ex);
    }

    /**
     * 节点尝试结果。
     *
     * @param result 成功路由结果
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private record NodeAttemptResult(Optional<OcrRouteExecutionResult> result) {
    }

}
