<script setup lang="ts">
import type { BatchOcrHitNode, BatchOcrRoutePolicy, BatchOcrRunningPageTask } from '@/types/dashboard'
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
  currentDocumentRunningHitNodes: BatchOcrHitNode[]
  currentDocumentFinalHitNodes: BatchOcrHitNode[]
  runningPageTasks?: BatchOcrRunningPageTask[]
  hitNodes: BatchOcrHitNode[]
}>()
</script>

<template>
  <section class="panel batch-ocr-route">
    <div class="panel__header">
      <div>
        <h2 class="panel__title">OCR 路由</h2>
        <span class="panel__hint">上传时路由策略、当前文件运行中分配、最终分配与批次级调度命中</span>
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
      <div class="batch-ocr-route__allocation-block">
        <div class="batch-ocr-route__subheader">
          <h4>运行中分配</h4>
          <span>仅展示当前仍在 OCR 的图片</span>
        </div>
        <div v-if="currentDocumentRunningHitNodes.length > 0" class="batch-ocr-route__hits">
          <article
            v-for="node in currentDocumentRunningHitNodes"
            :key="`running-${node.model_key}-${node.node_id}`"
            class="batch-ocr-route__hit"
          >
            <strong>{{ displayNodeName({ nodeId: node.node_id, nodeName: node.node_name }) }}</strong>
            <span>
              OCR 模型：{{ displayModelName({ modelKey: node.model_key, name: node.model_name }) }}
            </span>
            <span>
              {{ displayNodeSubtitle({ nodeId: node.node_id, imageCount: node.image_count }) }}
            </span>
          </article>
        </div>
        <p v-else class="batch-ocr-route__empty">当前文件暂无运行中的 OCR 图片</p>
      </div>
      <div class="batch-ocr-route__allocation-block">
        <div class="batch-ocr-route__subheader">
          <h4>最终分配结果</h4>
          <span>只统计每页最终成功归属</span>
        </div>
      <div v-if="currentDocumentFinalHitNodes.length > 0" class="batch-ocr-route__hits">
        <article
          v-for="node in currentDocumentFinalHitNodes"
          :key="`final-${node.model_key}-${node.node_id}`"
          class="batch-ocr-route__hit"
        >
          <strong>{{ displayNodeName({ nodeId: node.node_id, nodeName: node.node_name }) }}</strong>
          <span>
            OCR 模型：{{ displayModelName({ modelKey: node.model_key, name: node.model_name }) }}
          </span>
          <span>
            {{ displayNodeSubtitle({ nodeId: node.node_id, imageCount: node.image_count }) }}
          </span>
        </article>
      </div>
      <p v-else class="batch-ocr-route__empty">当前文件尚未完成 OCR，暂无最终分配结果</p>
      </div>
    </section>

    <section class="batch-ocr-route__section">
      <div class="batch-ocr-route__section-header">
        <h3>实时 OCR 图片任务</h3>
        <span>展示当前正在被 worker 线程消费的图片页</span>
      </div>
      <div v-if="(runningPageTasks?.length ?? 0) > 0" class="batch-ocr-route__live-table">
        <div class="batch-ocr-route__live-head">
          <span>页码</span>
          <span>OCR 节点</span>
          <span>Worker</span>
          <span>线程</span>
          <span>耗时</span>
        </div>
        <div
          v-for="task in runningPageTasks"
          :key="task.task_id"
          class="batch-ocr-route__live-row"
        >
          <span>第 {{ task.page_no }} 页</span>
          <span>{{ displayNodeName({ nodeId: task.node_id, nodeName: task.node_name }) }}</span>
          <span>{{ task.worker_id }}</span>
          <span>{{ task.thread_name }}</span>
          <span>{{ Math.max(0, Math.round(task.running_ms / 1000)) }} 秒</span>
        </div>
      </div>
      <p v-else class="batch-ocr-route__empty">当前没有正在执行的 OCR 图片页任务</p>
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
            OCR 模型：{{ displayModelName({ modelKey: node.model_key, name: node.model_name }) }}
          </span>
          <span>
            {{ displayNodeSubtitle({ nodeId: node.node_id, imageCount: node.image_count }) }}
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

.batch-ocr-route__allocation-block {
  display: grid;
  gap: 6px;
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

.batch-ocr-route__subheader {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.batch-ocr-route__subheader h4 {
  margin: 0;
  color: var(--ink-strong);
  font-size: 13px;
}

.batch-ocr-route__section-header span,
.batch-ocr-route__subheader span {
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

.batch-ocr-route__live-table {
  overflow: hidden;
  border: 1px solid var(--rail-border);
  border-radius: 8px;
  background: var(--surface-inset);
}

.batch-ocr-route__live-head,
.batch-ocr-route__live-row {
  display: grid;
  grid-template-columns: 80px minmax(150px, 1fr) minmax(110px, 0.8fr) minmax(180px, 1.2fr) 80px;
  gap: 8px;
  padding: 9px 10px;
  align-items: center;
}

.batch-ocr-route__live-head {
  color: var(--ink-muted);
  font-size: 12px;
  font-weight: 700;
}

.batch-ocr-route__live-row {
  border-top: 1px solid var(--rail-border);
  color: var(--ink-strong);
  font-size: 12px;
}

.batch-ocr-route__live-row span {
  min-width: 0;
  overflow-wrap: anywhere;
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

  .batch-ocr-route__live-head,
  .batch-ocr-route__live-row {
    grid-template-columns: 70px minmax(0, 1fr);
  }
}
</style>
