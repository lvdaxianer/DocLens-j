<script setup lang="ts">
defineProps<{
  width?: string
  height?: string
  borderRadius?: string
}>()
</script>

<template>
  <div
    class="skeleton"
    :style="{
      width: width ?? '100%',
      height: height ?? '1em',
      borderRadius: borderRadius ?? 'var(--radius-md)'
    }"
  />
</template>
