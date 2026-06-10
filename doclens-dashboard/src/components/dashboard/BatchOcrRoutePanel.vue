<script setup lang="ts">
import type { BatchOcrHitNode, BatchOcrRoutePolicy } from '@/types/dashboard'
import {
  displayLoadBalanceStrategy,
  displayModelName,
  displayNodeName,
  displayNodeSubtitle,
  displayRoutingMode
} from '@/utils/ocrDisplayRules'

defineProps<{
  routePolicy?: BatchOcrRoutePolicy | null
  currentDocumentName?: string
  currentDocumentFinalHitNodes: BatchOcrHitNode[]
  hitNodes: BatchOcrHitNode[]
}>()
</script>

<template>
  <section class="panel batch-ocr-route">
    <div class="panel__header">
      <div>
        <h2 class="panel__title">OCR 路由</h2>
        <span class="panel__hint">上传时路由策略、当前文件最终分配与批次级调度命中</span>
      </div>
    </div>
    <dl class="batch-ocr-route__policy">
      <div>
        <dt>路由模式</dt>
        <dd>{{ displayRoutingMode(routePolicy?.routing_mode) }}</dd>
      </div>
      <div>
        <dt>OCR 模型</dt>
        <dd>{{ displayModelName({ modelKey: routePolicy?.model_key, name: routePolicy?.model_name }) }}</dd>
      </div>
      <div>
        <dt>节点</dt>
        <dd>{{ displayNodeName({ nodeId: routePolicy?.node_id, nodeName: routePolicy?.node_name }) }}</dd>
      </div>
      <div>
        <dt>负载均衡</dt>
        <dd>{{ displayLoadBalanceStrategy(routePolicy?.load_balance_strategy) }}</dd>
      </div>
    </dl>

    <section class="batch-ocr-route__section">
      <div class="batch-ocr-route__section-header">
        <h3>当前文件实际分配</h3>
        <span>{{ currentDocumentName || '当前文件未确定' }}</span>
      </div>
      <div v-if="currentDocumentFinalHitNodes.length > 0" class="batch-ocr-route__hits">
        <article
          v-for="node in currentDocumentFinalHitNodes"
          :key="`final-${node.model_key}-${node.node_id}`"
          class="batch-ocr-route__hit"
        >
          <strong>{{ displayNodeName({ nodeId: node.node_id, nodeName: node.node_name }) }}</strong>
          <span>
            {{ displayNodeSubtitle({
              nodeId: node.node_id,
              modelKey: displayModelName({ modelKey: node.model_key, name: node.model_name }),
              imageCount: node.image_count
            }) }}
          </span>
        </article>
      </div>
      <p v-else class="batch-ocr-route__empty">当前文件尚未完成 OCR，暂无最终分配结果</p>
    </section>

    <section class="batch-ocr-route__section">
      <div class="batch-ocr-route__section-header">
        <h3>批次 OCR 调度命中</h3>
        <span>包含重试、故障转移和运行中请求，仅反映批次调度情况</span>
      </div>
      <div v-if="hitNodes.length > 0" class="batch-ocr-route__hits">
        <article v-for="node in hitNodes" :key="`${node.model_key}-${node.node_id}`" class="batch-ocr-route__hit">
          <strong>{{ displayNodeName({ nodeId: node.node_id, nodeName: node.node_name }) }}</strong>
          <span>
            {{ displayNodeSubtitle({
              nodeId: node.node_id,
              modelKey: displayModelName({ modelKey: node.model_key, name: node.model_name }),
              imageCount: node.image_count
            }) }}
          </span>
        </article>
      </div>
      <p v-else class="batch-ocr-route__empty">暂无批次 OCR 调度命中记录</p>
    </section>
  </section>
</template>

<style scoped>
.batch-ocr-route {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.batch-ocr-route__section {
  display: grid;
  gap: 8px;
}

.batch-ocr-route__section-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.batch-ocr-route__section-header h3 {
  margin: 0;
  color: var(--ink-strong);
  font-size: 14px;
}

.batch-ocr-route__section-header span {
  color: var(--ink-muted);
  font-size: 12px;
}

.batch-ocr-route__policy {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin: 0;
}

.batch-ocr-route__policy div,
.batch-ocr-route__hit,
.batch-ocr-route__empty {
  padding: 10px;
  border: 1px solid var(--rail-border);
  border-radius: 8px;
  background: var(--surface-inset);
}

.batch-ocr-route__policy dt,
.batch-ocr-route__hit span,
.batch-ocr-route__empty {
  color: var(--ink-muted);
  font-size: 12px;
}

.batch-ocr-route__policy dd,
.batch-ocr-route__hit strong {
  margin: 0;
  overflow-wrap: anywhere;
  color: var(--ink-strong);
  font-weight: 700;
}

.batch-ocr-route__hits {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 8px;
}

.batch-ocr-route__hit {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.batch-ocr-route__empty {
  margin: 0;
}

@media (max-width: 900px) {
  .batch-ocr-route__policy {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .batch-ocr-route__section-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
