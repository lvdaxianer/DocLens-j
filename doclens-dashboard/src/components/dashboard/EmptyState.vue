<script setup lang="ts">
import { NIcon } from 'naive-ui'
import type { Component } from 'vue'

defineProps<{
  description: string
  icon?: Component
}>()
</script>

<template>
  <div class="empty-state">
    <NIcon v-if="icon" :component="icon" />
    <span>{{ description }}</span>
  </div>
</template>
