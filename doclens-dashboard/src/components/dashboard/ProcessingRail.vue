<script setup lang="ts">
import { computed } from 'vue'

import type { ProcessingTrackNode } from '@/types/dashboard'

type TrackState = NonNullable<ProcessingTrackNode['state']>

const props = defineProps<{
  track: ProcessingTrackNode[]
  failed?: boolean
}>()

const nodes = computed(() => props.track ?? [])
const STEP_NUMBER_OFFSET = 1

const fallbackDescriptions: Record<TrackState, string> = {
  done: '已完成',
  current: '当前步骤',
  pending: '未开始',
  skipped: '无需执行',
  failed: '在此失败'
}

const stateLabels: Record<TrackState, string> = {
  done: '完成',
  current: '进行中',
  pending: '待执行',
  skipped: '跳过',
  failed: '失败'
}

/**
 * 获取流程节点状态，并兼容旧版只返回 active 的接口。
 *
 * @param node - 流程节点
 * @returns 节点状态
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
function nodeState(node: ProcessingTrackNode): TrackState {
  return node.state ?? (node.active ? 'done' : 'pending')
}

/**
 * 获取流程节点说明文案。
 *
 * @param node - 流程节点
 * @returns 节点说明
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
function nodeDescription(node: ProcessingTrackNode): string {
  const state = nodeState(node)
  return node.description ?? fallbackDescriptions[state]
}

/**
 * 获取流程节点悬浮提示。
 *
 * @param node - 流程节点
 * @returns 悬浮提示文本
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
function nodeTitle(node: ProcessingTrackNode): string {
  return `${node.name}：${nodeDescription(node)}`
}

/**
 * 获取从一开始展示的步骤序号。
 *
 * @param index - 零基数组下标
 * @returns 步骤序号
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
function nodeNumber(index: number): number {
  return index + STEP_NUMBER_OFFSET
}
</script>

<template>
  <div class="processing-rail" :class="{ 'processing-rail--failed': failed }" aria-label="处理轨道">
    <span
      v-for="(node, index) in nodes"
      :key="node.name"
      class="processing-rail__node"
      :class="`processing-rail__node--${nodeState(node)}`"
      :title="nodeTitle(node)"
    >
      <span class="processing-rail__dot">{{ nodeNumber(index) }}</span>
      <span class="processing-rail__body">
        <span class="processing-rail__label">{{ node.name }}</span>
        <span class="processing-rail__state">{{ stateLabels[nodeState(node)] }}</span>
        <span class="processing-rail__description">{{ nodeDescription(node) }}</span>
      </span>
    </span>
  </div>
</template>

<style scoped>
.processing-rail {
  display: grid;
  grid-template-columns: repeat(7, minmax(86px, 1fr));
  gap: 0;
  min-width: 720px;
}

.processing-rail__node {
  position: relative;
  display: grid;
  min-width: 0;
  grid-template-rows: auto minmax(48px, auto);
  justify-items: center;
  gap: 7px;
  color: var(--ink-muted);
  font-size: 12px;
  text-align: center;
}

.processing-rail__node::before {
  position: absolute;
  top: 13px;
  left: calc(-50% + 14px);
  width: calc(100% - 28px);
  height: 3px;
  background: var(--rail-border);
  content: "";
}

.processing-rail__node:first-child::before {
  display: none;
}

.processing-rail__dot {
  z-index: 1;
  display: grid;
  width: 28px;
  height: 28px;
  border: 2px solid var(--rail-border);
  border-radius: 50%;
  background: var(--surface-raised);
  color: var(--ink-muted);
  font-size: 12px;
  font-weight: 800;
  line-height: 1;
  place-items: center;
}

.processing-rail__body {
  display: grid;
  min-width: 0;
  justify-items: center;
  gap: 3px;
}

.processing-rail__node--done,
.processing-rail__node--current,
.processing-rail__node--failed {
  color: var(--ink-strong);
}

.processing-rail__node--done::before,
.processing-rail__node--done .processing-rail__dot {
  border-color: var(--active);
  background: var(--active);
  color: white;
}

.processing-rail__node--current::before {
  background: linear-gradient(90deg, var(--active), var(--rail-border));
}

.processing-rail__node--current .processing-rail__dot {
  border-color: var(--active);
  background: var(--surface-raised);
  color: var(--active-strong);
  box-shadow: 0 0 0 4px rgba(37, 109, 133, 0.14);
}

.processing-rail__node--skipped {
  color: #9aa6ac;
}

.processing-rail__node--skipped .processing-rail__dot {
  border-style: dashed;
  background: var(--surface-inset);
  color: #9aa6ac;
}

.processing-rail__node--failed::before,
.processing-rail__node--failed .processing-rail__dot {
  border-color: var(--danger);
  background: var(--danger);
  color: white;
}

.processing-rail__label {
  max-width: 88px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--ink-strong);
  font-weight: 750;
}

.processing-rail__state {
  max-width: 76px;
  padding: 2px 7px;
  border: 1px solid currentColor;
  border-radius: 999px;
  overflow: hidden;
  color: currentColor;
  font-size: 10px;
  font-weight: 800;
  line-height: 1.1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.processing-rail__description {
  max-width: 96px;
  overflow: hidden;
  color: var(--ink-muted);
  font-size: 10px;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.processing-rail__node--current .processing-rail__state,
.processing-rail__node--done .processing-rail__state {
  background: rgba(37, 109, 133, 0.08);
}

.processing-rail__node--failed .processing-rail__state {
  background: rgba(185, 45, 45, 0.08);
}

.processing-rail__node--skipped .processing-rail__label,
.processing-rail__node--pending .processing-rail__label {
  color: currentColor;
}
</style>
