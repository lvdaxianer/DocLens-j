<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { ActivitySquare, Gauge, Layers3, Radio, Upload, UploadCloud } from '@lucide/vue'
import {
  NConfigProvider,
  NIcon,
  NMessageProvider
} from 'naive-ui'
import type { GlobalThemeOverrides } from 'naive-ui'

const route = useRoute()

const navigationItems = [
  { name: 'overview', label: '总览', icon: ActivitySquare },
  { name: 'upload', label: '上传', icon: Upload },
  { name: 'batches', label: '批次', icon: Layers3 },
  { name: 'ocr-health', label: 'OCR 健康', icon: Gauge }
]

const activeTitle = computed(() => route.meta.title ?? 'DocLens 控制台')

const themeOverrides: GlobalThemeOverrides = {
  common: {
    primaryColor: '#256d85',
    primaryColorHover: '#2f829c',
    primaryColorPressed: '#1f5b70',
    primaryColorSuppl: '#dceff5',
    borderRadius: '8px',
    fontFamily: 'Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif'
  }
}
</script>

<template>
  <NConfigProvider :theme-overrides="themeOverrides">
    <NMessageProvider>
      <div class="app-shell">
        <aside class="app-shell__sidebar">
          <RouterLink class="app-shell__brand" :to="{ name: 'overview' }">
            <span class="app-shell__brand-mark">
              <NIcon :component="UploadCloud" />
            </span>
            <span class="app-shell__brand-copy">
              <strong>DocLens</strong>
              <small>解析控制台</small>
            </span>
          </RouterLink>

          <nav class="app-shell__nav" aria-label="主导航">
            <RouterLink
              v-for="item in navigationItems"
              :key="item.name"
              class="app-shell__nav-item"
              :to="{ name: item.name }"
            >
              <NIcon :component="item.icon" />
              <span>{{ item.label }}</span>
            </RouterLink>
          </nav>
        </aside>

        <main class="app-shell__main">
          <header class="app-shell__topbar">
            <div>
              <p class="app-shell__eyebrow">Pipeline Console</p>
              <h1 class="app-shell__title">{{ activeTitle }}</h1>
            </div>
            <span class="app-shell__live">
              <NIcon :component="Radio" />
              实时读模型
            </span>
          </header>
          <RouterView />
        </main>
      </div>
    </NMessageProvider>
  </NConfigProvider>
</template>

<style scoped>
.app-shell {
  display: grid;
  min-height: 100vh;
  grid-template-columns: 248px minmax(0, 1fr);
  background: var(--surface-canvas);
  color: var(--ink-strong);
}

.app-shell__sidebar {
  position: sticky;
  top: 0;
  display: flex;
  height: 100vh;
  flex-direction: column;
  gap: 22px;
  padding: 22px 14px;
  border-right: 1px solid var(--rail-border);
  background:
    linear-gradient(180deg, rgba(220, 239, 245, 0.52), rgba(255, 255, 255, 0) 180px),
    var(--surface-sidebar);
}

.app-shell__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  color: inherit;
  text-decoration: none;
  transition: background-color 180ms ease;
}

.app-shell__brand:hover {
  background: var(--surface-hover);
}

.app-shell__brand-mark {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 8px;
  background: linear-gradient(135deg, var(--active), var(--active-strong));
  color: #ffffff;
  font-size: 22px;
  box-shadow: 0 10px 22px rgba(37, 109, 133, 0.22);
}

.app-shell__brand-copy {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.app-shell__brand-copy strong {
  font-size: 16px;
  line-height: 1.1;
}

.app-shell__brand-copy small {
  color: var(--ink-muted);
  font-size: 12px;
}

.app-shell__nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.app-shell__nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 42px;
  padding: 0 12px;
  border-radius: 7px;
  color: var(--ink-soft);
  font-size: 14px;
  text-decoration: none;
  transition: background-color 180ms ease, color 180ms ease, transform 180ms ease;
}

.app-shell__nav-item:hover {
  background: var(--surface-hover);
  color: var(--ink-strong);
}

.app-shell__nav-item.router-link-active {
  background: var(--active-muted);
  color: var(--ink-strong);
  font-weight: 650;
}

.app-shell__main {
  min-width: 0;
  padding: 26px 28px;
}

.app-shell__topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 20px;
}

.app-shell__eyebrow {
  margin: 0 0 4px;
  color: var(--ink-muted);
  font-size: 12px;
  font-weight: 650;
  text-transform: uppercase;
}

.app-shell__title {
  margin: 0;
  font-size: 26px;
  font-weight: 750;
  letter-spacing: 0;
}

.app-shell__live {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 9px;
  border: 1px solid rgba(35, 132, 90, 0.32);
  border-radius: 999px;
  color: var(--success);
  font-size: 12px;
  font-weight: 650;
  background: var(--success-muted);
}

@media (max-width: 860px) {
  .app-shell {
    grid-template-columns: 1fr;
  }

  .app-shell__sidebar {
    position: sticky;
    top: 0;
    z-index: 10;
    height: auto;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    border-right: 0;
    border-bottom: 1px solid var(--rail-border);
  }

  .app-shell__nav {
    flex-direction: row;
  }

  .app-shell__nav-item span {
    display: none;
  }

  .app-shell__main {
    padding: 18px;
  }
}
</style>
