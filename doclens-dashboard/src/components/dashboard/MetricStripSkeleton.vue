<script setup lang="ts">
import SkeletonBox from '@/components/dashboard/SkeletonBox.vue'
</script>

<template>
  <section class="metric-strip" aria-label="Dashboard metrics loading">
    <article v-for="index in 6" :key="index" class="metric-card metric-card--skeleton">
      <SkeletonBox width="34px" height="34px" borderRadius="7px" />
      <div class="metric-card__body">
        <SkeletonBox width="50%" height="12px" />
        <SkeletonBox width="70%" height="22px" />
        <SkeletonBox width="60%" height="12px" />
      </div>
    </article>
  </section>
</template>

<style scoped>
.metric-card--skeleton {
  display: flex;
  gap: 10px;
  padding: 16px;
  border: 1px solid var(--rail-border);
  border-radius: 8px;
  background: var(--surface-raised);
}

.metric-card--skeleton .metric-card__body {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 6px;
}
</style>
