<script setup lang="ts">
import { computed } from 'vue'
import { Activity } from '@lucide/vue'
import { NIcon, NTag } from 'naive-ui'

import type { OcrNode, OcrNodeActionById, OcrNodeActionKind, OcrNodeStatus } from '@/types/ocrResources'
import OcrNodeActionCell from '@/components/ocr/OcrNodeActionCell.vue'
import OcrNodeEnabledSwitch from '@/components/ocr/OcrNodeEnabledSwitch.vue'
import { formatDateTime, formatDuration, formatNumber } from '@/utils/formatters'
import { displayModelName, displayNodeName, summarizeOcrNode } from '@/utils/ocrDisplayRules'
import {
  OCR_NODE_TABLE_ACTION_COLUMN_WIDTH,
  OCR_NODE_TABLE_ACTION_GAP,
  shouldStickOcrNodeTableActions
} from '@/utils/ocrNodeTableLayout'

const props = defineProps<{
  nodes: OcrNode[]
  loading?: boolean
  nodeActionById?: OcrNodeActionById
}>()

const emit = defineEmits<{
  edit: [node: OcrNode]
  delete: [node: OcrNode]
  detail: [node: OcrNode]
  test: [node: OcrNode]
  reconnect: [node: OcrNode]
  toggleEnabled: [node: OcrNode, enabled: boolean]
}>()

const sortedNodes = computed(() => [...props.nodes].sort((left, right) => left.name.localeCompare(right.name)))
const tableStyle = computed(() => ({
  '--ocr-node-action-column-width': `${OCR_NODE_TABLE_ACTION_COLUMN_WIDTH}px`,
  '--ocr-node-action-gap': `${OCR_NODE_TABLE_ACTION_GAP}px`
}))
const isActionColumnSticky = shouldStickOcrNodeTableActions()

/**
 * 获取节点状态标签类型。
 *
 * @param status - 节点状态
 * @returns Naive UI 标签类型
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function statusTagType(status: OcrNodeStatus): 'success' | 'warning' | 'error' | 'default' {
  if (status === 'UP') {
    return 'success'
  } else if (status === 'RECOVERING') {
    return 'warning'
  } else if (status === 'DOWN') {
    return 'error'
  } else {
    return 'default'
  }
}

/**
 * 获取节点状态中文标签。
 *
 * @param status - 节点状态
 * @returns 节点状态标签
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function statusLabel(status: OcrNodeStatus): string {
  const labels: Record<OcrNodeStatus, string> = {
    UP: '健康',
    DOWN: '异常',
    RECOVERING: '恢复中',
    DISABLED: '停用'
  }
  return labels[status]
}

/**
 * 触发节点启停。
 *
 * @param node - OCR 节点
 * @param enabled - 是否启用
 * @returns 触发完成信号
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function toggleEnabled(node: OcrNode, enabled: boolean): void {
  emit('toggleEnabled', node, enabled)
}

/**
 * 获取节点资源地址或在线渠道展示文本。
 *
 * @param node - OCR 节点
 * @returns 节点地址展示文本
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function nodeEndpointLabel(node: OcrNode): string {
  if (node.deployment_type === 'ONLINE') {
    return `${node.channel_key || '在线渠道'} · ${node.provider_model || '未配置模型'}`
  } else {
    return `${node.host}:${node.port}`
  }
}

/**
 * 获取节点治理摘要。
 *
 * @param node - OCR 节点
 * @returns 节点治理摘要
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
function governanceSummary(node: OcrNode) {
  return summarizeOcrNode(node)
}

/**
 * 获取节点当前动作类型。
 *
 * @param node - OCR 节点
 * @returns 当前动作类型
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
function nodeActionKind(node: OcrNode): OcrNodeActionKind | undefined {
  return props.nodeActionById?.[node.id]
}
</script>

<template>
  <section class="ocr-node-table">
    <div v-if="loading" class="ocr-node-table__state">
      正在加载节点
    </div>

    <div v-else-if="sortedNodes.length === 0" class="ocr-node-table__state">
      当前模型还没有配置节点
    </div>

    <div v-else class="ocr-node-table__scroller" :style="tableStyle">
      <table class="ocr-node-table__grid">
        <thead>
          <tr>
            <th>OCR</th>
            <th>节点</th>
            <th>部署</th>
            <th>地址</th>
            <th>状态</th>
            <th>治理</th>
            <th>启用</th>
            <th>全局</th>
            <th>解析中</th>
            <th>最大并发</th>
            <th>排队</th>
            <th>今日处理</th>
            <th>成功</th>
            <th>失败</th>
            <th>平均耗时</th>
            <th>P95</th>
            <th>最近检查</th>
            <th :class="{ 'ocr-node-table__action-cell--sticky': isActionColumnSticky }">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="node in sortedNodes" :key="node.id">
            <td>{{ displayModelName({ modelKey: node.model_key }) }}</td>
            <td>
              <button class="ocr-node-table__name" type="button" @click="emit('detail', node)">
                <NIcon :component="Activity" />
                <span>{{ displayNodeName({ id: node.id, name: node.name }) }}</span>
              </button>
            </td>
            <td>
              <NTag size="small" :type="node.deployment_type === 'ONLINE' ? 'info' : 'default'">
                {{ node.deployment_type === 'ONLINE' ? '在线' : '离线' }}
              </NTag>
            </td>
            <td>{{ nodeEndpointLabel(node) }}</td>
            <td>
              <NTag size="small" :type="statusTagType(node.status)">
                {{ statusLabel(node.status) }}
              </NTag>
            </td>
            <td>
              <div class="ocr-node-table__governance">
                <span>{{ governanceSummary(node).queueLabel }}</span>
                <span>{{ governanceSummary(node).circuitLabel }}</span>
              </div>
            </td>
            <td>
              <OcrNodeEnabledSwitch
                :value="node.enabled"
                :action-kind="nodeActionKind(node)"
                @update:value="toggleEnabled(node, $event)"
              />
            </td>
            <td>
              <NTag size="small" :type="node.participate_global ? 'success' : 'default'">
                {{ node.participate_global ? '参与' : '不参与' }}
              </NTag>
            </td>
            <td>{{ formatNumber(node.inflight_images) }}</td>
            <td>{{ formatNumber(node.max_concurrency) }}</td>
            <td>{{ formatNumber(node.queued_images) }}</td>
            <td>{{ formatNumber(node.processed_images_today) }}</td>
            <td>{{ formatNumber(node.success_images) }}</td>
            <td>{{ formatNumber(node.failed_images) }}</td>
            <td>{{ formatDuration(node.avg_latency_ms) }}</td>
            <td>{{ formatDuration(node.p95_latency_ms) }}</td>
            <td>{{ formatDateTime(node.last_health_at) }}</td>
            <td :class="{ 'ocr-node-table__action-cell--sticky': isActionColumnSticky }">
              <OcrNodeActionCell
                :node="node"
                :action-kind="nodeActionKind(node)"
                @edit="emit('edit', node)"
                @delete="emit('delete', node)"
                @detail="emit('detail', node)"
                @test="emit('test', node)"
                @reconnect="emit('reconnect', node)"
                @toggle-enabled="toggleEnabled(node, !node.enabled)"
              />
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<style scoped>
.ocr-node-table {
  min-width: 0;
}

.ocr-node-table__state {
  display: grid;
  min-height: 180px;
  place-items: center;
  color: var(--ink-muted);
  font-size: 14px;
}

.ocr-node-table__scroller {
  max-width: 100%;
  overflow-x: auto;
}

.ocr-node-table__grid {
  width: 100%;
  min-width: 1180px;
  border-collapse: collapse;
  table-layout: fixed;
}

.ocr-node-table__grid th,
.ocr-node-table__grid td {
  padding: 10px 8px;
  border-bottom: 1px solid var(--rail-border);
  color: var(--ink-soft);
  font-size: 12px;
  text-align: left;
  vertical-align: middle;
  overflow-wrap: anywhere;
}

.ocr-node-table__grid th {
  color: var(--ink-muted);
  font-weight: 700;
  background: var(--surface-inset);
}

.ocr-node-table__governance {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ocr-node-table__name {
  display: inline-flex;
  max-width: 100%;
  align-items: center;
  gap: 6px;
  padding: 0;
  border: 0;
  color: var(--active);
  font: inherit;
  font-weight: 700;
  background: transparent;
}

.ocr-node-table__name span {
  overflow-wrap: anywhere;
}

.ocr-node-table__action-cell--sticky {
  position: sticky;
  right: 0;
  width: var(--ocr-node-action-column-width);
  min-width: var(--ocr-node-action-column-width);
  max-width: var(--ocr-node-action-column-width);
  background: var(--surface-raised);
  box-shadow: -8px 0 12px rgba(16, 32, 42, 0.06);
}

.ocr-node-table__grid th.ocr-node-table__action-cell--sticky {
  background: var(--surface-inset);
}

@media (max-width: 1440px) {
  .ocr-node-table__grid th,
  .ocr-node-table__grid td {
    padding: 9px 6px;
    font-size: 11px;
  }
}
</style>
