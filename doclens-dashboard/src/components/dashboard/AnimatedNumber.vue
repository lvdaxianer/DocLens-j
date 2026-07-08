<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  value: number | string | undefined | null
  duration?: number
}>()

const DURATION_DEFAULT = 600

const displayValue = ref<string>(format(props.value))

function format(raw: number | string | undefined | null): string {
  if (raw === undefined || raw === null) {
    return '-'
  }
  return String(raw)
}

interface NumericMatch {
  raw: string
  numeric: number
  decimals: number
  start: number
  end: number
}

function extractNumeric(text: string): NumericMatch | null {
  // 匹配带千分位、小数的数字，例如 1,234.56
  const match = text.match(/(\d{1,3}(?:,\d{3})*(?:\.\d+)?|\.\d+|\d+\.\d*|\d+)/)
  if (!match) {
    return null
  }
  const raw = match[0]
  const numeric = Number(raw.replace(/,/g, ''))
  if (!Number.isFinite(numeric)) {
    return null
  }
  const decimalPart = raw.split('.')[1]
  const decimals = decimalPart ? decimalPart.length : 0
  return {
    raw,
    numeric,
    decimals,
    start: match.index ?? 0,
    end: (match.index ?? 0) + raw.length
  }
}

function formatNumeric(value: number, decimals: number): string {
  if (decimals > 0) {
    return value.toFixed(decimals)
  }
  return String(Math.round(value))
}

function replaceNumeric(text: string, replacement: string, match: NumericMatch): string {
  return text.slice(0, match.start) + replacement + text.slice(match.end)
}

function easeOutCubic(t: number): number {
  return 1 - Math.pow(1 - t, 3)
}

watch(
  () => props.value,
  (newValue, oldValue) => {
    const newText = format(newValue)
    const oldText = format(oldValue)
    const newMatch = extractNumeric(newText)
    const oldMatch = extractNumeric(oldText)

    if (!newMatch || !oldMatch || oldValue === undefined) {
      displayValue.value = newText
      return
    }

    const targetMatch = newMatch
    const sourceMatch = oldMatch
    const duration = props.duration ?? DURATION_DEFAULT
    const startTime = performance.now()
    const delta = targetMatch.numeric - sourceMatch.numeric

    function step(now: number): void {
      const elapsed = now - startTime
      const progress = Math.min(elapsed / duration, 1)
      const eased = easeOutCubic(progress)
      const current = sourceMatch.numeric + delta * eased
      const formatted = formatNumeric(current, targetMatch.decimals)
      displayValue.value = replaceNumeric(newText, formatted, targetMatch)

      if (progress < 1) {
        requestAnimationFrame(step)
      } else {
        displayValue.value = newText
      }
    }

    requestAnimationFrame(step)
  },
  { immediate: true }
)
</script>

<template>
  <span class="animated-number">{{ displayValue }}</span>
</template>

<style scoped>
.animated-number {
  display: inline-block;
}
</style>
