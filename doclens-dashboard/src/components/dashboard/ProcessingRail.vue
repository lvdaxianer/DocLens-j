<script setup lang="ts">
import { computed } from 'vue'

import type { ProcessingTrackNode } from '@/types/dashboard'

const props = defineProps<{
  track: ProcessingTrackNode[]
  failed?: boolean
}>()

const nodes = computed(() => props.track ?? [])
</script>

<template>
  <div class="processing-rail" :class="{ 'processing-rail--failed': failed }" aria-label="处理轨道">
    <span
      v-for="node in nodes"
      :key="node.name"
      class="processing-rail__node"
      :class="{ 'processing-rail__node--active': node.active }"
      :title="node.name"
    >
      <span class="processing-rail__dot" />
      <span class="processing-rail__label">{{ node.name }}</span>
    </span>
  </div>
</template>

<style scoped>
.processing-rail {
  display: grid;
  grid-template-columns: repeat(7, minmax(44px, 1fr));
  gap: 0;
  min-width: 360px;
}

.processing-rail__node {
  position: relative;
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: var(--ink-muted);
  font-size: 11px;
}

.processing-rail__node::before {
  position: absolute;
  top: 6px;
  left: calc(-50% + 7px);
  width: calc(100% - 14px);
  height: 2px;
  background: var(--rail-border);
  content: "";
}

.processing-rail__node:first-child::before {
  display: none;
}

.processing-rail__dot {
  z-index: 1;
  width: 14px;
  height: 14px;
  border: 2px solid var(--rail-border);
  border-radius: 50%;
  background: var(--surface-raised);
}

.processing-rail__node--active {
  color: var(--ink-strong);
}

.processing-rail__node--active::before,
.processing-rail__node--active .processing-rail__dot {
  border-color: var(--active);
  background: var(--active);
}

.processing-rail--failed .processing-rail__node--active::before,
.processing-rail--failed .processing-rail__node--active .processing-rail__dot {
  border-color: var(--danger);
  background: var(--danger);
}

.processing-rail__label {
  max-width: 64px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
