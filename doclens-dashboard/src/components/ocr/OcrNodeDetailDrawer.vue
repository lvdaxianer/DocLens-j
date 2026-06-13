<script setup lang="ts">
import { computed } from 'vue'
import { Activity, AlertTriangle, Clock3, Image, Timer } from '@lucide/vue'
import { NDrawer, NDrawerContent, NIcon, NTag } from 'naive-ui'

import type { OcrNode, OcrNodeCall } from '@/types/ocrResources'
import { formatDateTime, formatDuration, formatNumber } from '@/utils/formatters'
import { displayModelName, displayNodeName, summarizeOcrNode } from '@/utils/ocrDisplayRules'

const props = defineProps<{
  visible: boolean
  node?: OcrNode | null
  calls?: OcrNodeCall[]
}>()

const emit = defineEmits<{
  close: []
}>()

const recentCalls = computed(() => props.calls ?? [])
const failedRate = computed(() => {
  const total = (props.node?.success_images ?? 0) + (props.node?.failed_images ?? 0)
  if (total > 0) {
    return `${(((props.node?.failed_images ?? 0) / total) * 100).toFixed(1)}%`
  } else {
    return '0.0%'
  }
})

const endpointLabel = computed(() => {
  if (props.node?.deployment_type === 'ONLINE') {
    return `${props.node.channel_key || '在线渠道'} · ${props.node.provider_model || '未配置模型'}`
  } else if (props.node) {
    return `${props.node.host}:${props.node.port}`
  } else {
    return ''
  }
})

const governanceSummary = computed(() => summarizeOcrNode(props.node ?? {}))
</script>

<template>
  <NDrawer :show="visible" :width="520" class="ocr-node-detail-drawer" @update:show="emit('close')">
    <NDrawerContent :title="node ? displayNodeName({ id: node.id, name: node.name }) : 'OCR 节点详情'" closable>
      <section v-if="node" class="ocr-node-detail">
        <div class="ocr-node-detail__summary">
          <div>
            <span>模型</span>
            <strong>{{ displayModelName({ modelKey: node.model_key }) }}</strong>
          </div>
          <div>
            <span>{{ node.deployment_type === 'ONLINE' ? '渠道' : '地址' }}</span>
            <strong>{{ endpointLabel }}</strong>
          </div>
          <div>
            <span>状态</span>
            <NTag size="small" :type="node.status === 'UP' ? 'success' : 'warning'">
              {{ node.status }}
            </NTag>
          </div>
        </div>

        <div class="ocr-node-detail__metrics">
          <article class="ocr-node-detail__metric">
            <NIcon :component="Image" />
            <span>当前解析</span>
            <strong>{{ formatNumber(node.inflight_images) }}</strong>
          </article>
          <article class="ocr-node-detail__metric">
            <NIcon :component="Clock3" />
            <span>当前排队</span>
            <strong>{{ formatNumber(node.queued_images) }}</strong>
          </article>
          <article class="ocr-node-detail__metric">
            <NIcon :component="Activity" />
            <span>今日处理</span>
            <strong>{{ formatNumber(node.processed_images_today) }}</strong>
          </article>
          <article class="ocr-node-detail__metric">
            <NIcon :component="Timer" />
            <span>平均耗时</span>
            <strong>{{ formatDuration(node.avg_latency_ms) }}</strong>
          </article>
          <article class="ocr-node-detail__metric">
            <NIcon :component="AlertTriangle" />
            <span>失败率</span>
            <strong>{{ failedRate }}</strong>
          </article>
        </div>

        <section class="ocr-node-detail__section">
          <h2>运行指标</h2>
          <dl class="ocr-node-detail__pairs">
            <div>
              <dt>部署类型</dt>
              <dd>{{ node.deployment_type === 'ONLINE' ? '在线节点' : '离线节点' }}</dd>
            </div>
            <div v-if="node.deployment_type === 'ONLINE'">
              <dt>密钥状态</dt>
              <dd>{{ node.credential_configured ? '已配置' : '未配置' }}</dd>
            </div>
            <div v-if="node.deployment_type === 'ONLINE'">
              <dt>环境变量名</dt>
              <dd>{{ node.credential_env_var || '未填写环境变量名' }}</dd>
            </div>
            <div>
              <dt>排队图片</dt>
              <dd>{{ formatNumber(node.queued_images) }}</dd>
            </div>
            <div>
              <dt>权重 / 并发</dt>
              <dd>{{ formatNumber(node.weight) }} / {{ formatNumber(node.max_concurrency) }}</dd>
            </div>
            <div>
              <dt>成功图片</dt>
              <dd>{{ formatNumber(node.success_images) }}</dd>
            </div>
            <div>
              <dt>失败图片</dt>
              <dd>{{ formatNumber(node.failed_images) }}</dd>
            </div>
            <div>
              <dt>P95 耗时</dt>
              <dd>{{ formatDuration(node.p95_latency_ms) }}</dd>
            </div>
            <div>
              <dt>最近检查</dt>
              <dd>{{ formatDateTime(node.last_health_at) }}</dd>
            </div>
          </dl>
        </section>

        <section class="ocr-node-detail__section">
          <h2>健康治理</h2>
          <dl class="ocr-node-detail__pairs">
            <div>
              <dt>连续失败</dt>
              <dd>{{ formatNumber(node.failure_count) }}</dd>
            </div>
            <div>
              <dt>恢复成功</dt>
              <dd>{{ formatNumber(node.recovery_success_count) }}</dd>
            </div>
            <div>
              <dt>熔断窗口</dt>
              <dd>{{ governanceSummary.circuitLabel }}</dd>
            </div>
            <div>
              <dt>手动恢复</dt>
              <dd>{{ formatDateTime(node.last_manual_recovery_at) }}</dd>
            </div>
          </dl>
        </section>

        <section class="ocr-node-detail__section">
          <h2>最近错误</h2>
          <p class="ocr-node-detail__error">{{ node.last_error || '暂无错误' }}</p>
        </section>

        <section class="ocr-node-detail__section">
          <h2>当前解析图片</h2>
          <p class="ocr-node-detail__empty">
            当前解析中图片数：{{ formatNumber(node.inflight_images) }}
          </p>
        </section>

        <section class="ocr-node-detail__section">
          <h2>最近调用</h2>
          <div v-if="recentCalls.length > 0" class="ocr-node-detail__calls">
            <article v-for="call in recentCalls" :key="call.id" class="ocr-node-detail__call">
              <NIcon :component="Clock3" />
              <div>
                <strong>{{ call.document_id }} · 第 {{ call.image_index }} 张</strong>
                <span>{{ call.status }} · 重试 {{ call.retry_count }} · {{ formatDuration(call.duration_ms) }}</span>
              </div>
            </article>
          </div>
          <p v-else class="ocr-node-detail__empty">暂无最近调用记录</p>
        </section>
      </section>
    </NDrawerContent>
  </NDrawer>
</template>

<style scoped>
.ocr-node-detail,
.ocr-node-detail__section,
.ocr-node-detail__calls {
  display: flex;
  flex-direction: column;
}

.ocr-node-detail {
  gap: 16px;
  min-width: 0;
}

.ocr-node-detail__summary,
.ocr-node-detail__metrics {
  display: grid;
  gap: 10px;
}

.ocr-node-detail__summary {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.ocr-node-detail__summary div,
.ocr-node-detail__metric {
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--rail-border);
  border-radius: 8px;
  background: var(--surface-inset);
}

.ocr-node-detail__summary span,
.ocr-node-detail__metric span,
.ocr-node-detail__pairs dt,
.ocr-node-detail__empty {
  color: var(--ink-muted);
  font-size: 12px;
}

.ocr-node-detail__summary strong,
.ocr-node-detail__metric strong,
.ocr-node-detail__pairs dd {
  overflow-wrap: anywhere;
  color: var(--ink-strong);
  font-weight: 700;
}

.ocr-node-detail__metrics {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.ocr-node-detail__metric {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 4px 8px;
}

.ocr-node-detail__metric .n-icon {
  grid-row: span 2;
  color: var(--active);
  font-size: 20px;
}

.ocr-node-detail__section {
  gap: 10px;
}

.ocr-node-detail__section h2 {
  margin: 0;
  font-size: 14px;
}

.ocr-node-detail__pairs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin: 0;
}

.ocr-node-detail__pairs div {
  padding: 10px;
  border: 1px solid var(--rail-border);
  border-radius: 8px;
}

.ocr-node-detail__pairs dt,
.ocr-node-detail__pairs dd {
  margin: 0;
}

.ocr-node-detail__error,
.ocr-node-detail__empty {
  margin: 0;
  padding: 12px;
  border: 1px solid var(--rail-border);
  border-radius: 8px;
  background: var(--surface-inset);
}

.ocr-node-detail__error {
  color: var(--danger);
  overflow-wrap: anywhere;
}

.ocr-node-detail__calls {
  gap: 8px;
}

.ocr-node-detail__call {
  display: flex;
  min-width: 0;
  gap: 8px;
  padding: 10px;
  border: 1px solid var(--rail-border);
  border-radius: 8px;
}

.ocr-node-detail__call div {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.ocr-node-detail__call span {
  color: var(--ink-muted);
  font-size: 12px;
}

@media (max-width: 640px) {
  :deep(.ocr-node-detail-drawer.n-drawer) {
    width: min(520px, 100vw) !important;
  }

  .ocr-node-detail__summary,
  .ocr-node-detail__metrics,
  .ocr-node-detail__pairs {
    grid-template-columns: 1fr;
  }
}
</style>
