package io.github.lvdaxianer.doclens.j.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallStatus;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * OCR 节点治理 API 契约测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
class OcrNodeGovernanceApiContractTest extends OcrNodeApiContractSupport {

    /*
     * 该测试类聚焦 Dashboard 节点治理所需字段。
     * 这些断言依赖节点池排队计数、节点健康状态和调用记录，
     * 与基础 CRUD 契约拆开后更容易定位治理字段回归。
     */

    /**
     * 节点列表接口应暴露真实排队数与健康治理字段，供 Dashboard 节点治理视图展示。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void listNodesReturnsQueuedAndHealthGovernanceMetrics() throws Exception {
        String nodeId = createNode("paddle-api-governance", "10.100.30.222", 8080);
        markNodeWithHealthGovernance(nodeId);
        nodePool.incrementQueued(nodeId);
        nodePool.incrementQueued(nodeId);

        JsonNode node = findNodeByName(listNodeItems(), "paddle-api-governance");

        assertThat(node.get("queued_images").asInt()).isEqualTo(2);
        assertThat(node.get("weight").asInt()).isEqualTo(100);
        assertThat(node.get("max_concurrency").asInt()).isEqualTo(4);
        assertThat(node.get("failure_count").asLong()).isEqualTo(3L);
        assertThat(node.get("circuit_open_until").asText()).isNotBlank();
    }

    /**
     * 节点详情接口应返回最近调用记录，供 Dashboard 详情抽屉展示。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void nodeRecentCallsReturnsLatestCallRecords() throws Exception {
        String nodeId = createNode("paddle-api-calls", "10.100.30.221", 8080);
        saveCall(successCall(nodeId));
        saveCall(failedCall(nodeId));

        mockMvc.perform(get("/api/v1/ocr-nodes/{nodeId}/calls", nodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].document_id").value("doc_call_2"))
                .andExpect(jsonPath("$.items[0].image_index").value(2))
                .andExpect(jsonPath("$.items[0].status").value("FAILED"))
                .andExpect(jsonPath("$.items[0].retry_count").value(1))
                .andExpect(jsonPath("$.items[0].duration_ms").value(520))
                .andExpect(jsonPath("$.items[0].error_message").value("timeout"))
                .andExpect(jsonPath("$.items[1].document_id").value("doc_call_1"));
    }

    /**
     * 写入节点健康治理状态，供节点列表契约测试复用。
     *
     * @param nodeId 节点 ID
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void markNodeWithHealthGovernance(String nodeId) {
        OcrNode current = nodeRepository.findById(nodeId).orElseThrow();
        OcrNode updated = nodeWithHealthGovernance(current);
        nodeRepository.update(updated);
        nodePool.refresh();
    }

    /**
     * 创建带健康治理状态的节点快照。
     *
     * @param current 当前节点
     * @return 更新后的节点
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private OcrNode nodeWithHealthGovernance(OcrNode current) {
        return new OcrNode(current.id(), current.modelKey(), current.deploymentType(), current.name(),
                current.host(), current.port(), current.channelKey(), current.providerModel(), current.credentialRef(),
                current.credentialConfigured(), current.enabled(), current.participateGlobal(), current.weight(),
                current.maxConcurrency(), OcrNodeStatus.DOWN, 3L, 1L, current.avgLatencyMs(), current.p95LatencyMs(),
                Optional.of(BASE_TIME), Optional.of(BASE_TIME.minusMinutes(2)), Optional.of(BASE_TIME.minusMinutes(1)),
                Optional.of("timeout"), Optional.of(BASE_TIME.plusHours(1)), Optional.of(BASE_TIME), current.createdAt(),
                BASE_TIME);
    }

    /**
     * 创建成功调用记录参数。
     *
     * @param nodeId 节点 ID
     * @return 调用记录参数
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private OcrCallSeed successCall(String nodeId) {
        return new OcrCallSeed(nodeId, "doc_call_1", 1, OcrNodeCallStatus.SUCCESS, 0, 180L, Optional.empty());
    }

    /**
     * 创建失败调用记录参数。
     *
     * @param nodeId 节点 ID
     * @return 调用记录参数
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private OcrCallSeed failedCall(String nodeId) {
        return new OcrCallSeed(nodeId, "doc_call_2", 2, OcrNodeCallStatus.FAILED, 1, 520L, Optional.of("timeout"));
    }
}
