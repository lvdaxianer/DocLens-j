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

    private final OcrDispatchCoordinator dispatchCoordinator;
    private final OcrNodeImageExecutor nodeExecutor;
    private final OcrNodeCallRecorder callRecorder;
    private final OcrRoutingServiceProperties properties;

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
        this.properties = dependencies.properties();
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
        OcrRoutePolicy policy = effectivePolicy(requestedPolicy);
        Set<String> excludedNodeIds = new HashSet<>();
        OcrRouteAccumulator accumulator = new OcrRouteAccumulator(startedAt);
        return routeUntilSuccess(request, policy, excludedNodeIds, accumulator);
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
        try {
            NodeAttemptResult result = executeWithRetry(request, policy, node, accumulator);
            if (result.result().isPresent()) {
                return result.result().get();
            } else if (canFailover(policy)) {
                excludedNodeIds.add(node.nodeId());
                return routeUntilSuccess(request, policy, excludedNodeIds, accumulator);
            } else {
                throw routeException(accumulator.lastFailure());
            }
        } finally {
            dispatchCoordinator.release(node.nodeId());
        }
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
