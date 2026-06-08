<script setup lang="ts">
import { onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { NAlert, NButton } from 'naive-ui'

import BatchTable from '@/components/dashboard/BatchTable.vue'
import { useDashboardStore } from '@/stores/dashboard'
import { formatDateTime, formatNumber } from '@/utils/formatters'

const store = useDashboardStore()
const { batches, batchesState } = storeToRefs(store)

function refresh(): void {
  void store.loadBatches()
}

onMounted(refresh)
</script>

<template>
  <div class="view-stack">
    <NAlert v-if="batchesState.error" type="error" :title="batchesState.error" />

    <section class="panel">
      <div class="panel__header">
        <div>
          <h2 class="panel__title">批次扫描</h2>
          <span class="panel__hint">
            共 {{ formatNumber(batches.total) }} 个批次 · 最后刷新 {{ formatDateTime(batchesState.lastUpdated) }}
          </span>
        </div>
        <NButton size="small" :loading="batchesState.loading" @click="refresh">
          刷新
        </NButton>
      </div>
      <BatchTable :batches="batches.items" :loading="batchesState.loading" />
    </section>
  </div>
</template>
