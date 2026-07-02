<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { ActivitySquare, Gauge, Layers3, Radio, ServerCog, Upload, UploadCloud } from '@lucide/vue'
import {
  NConfigProvider,
  NIcon,
  NMessageProvider
} from 'naive-ui'
import { createDashboardThemeOverrides } from '@/theme/dashboardTheme'
import PrincipalPartitionStatus from '@/components/dashboard/PrincipalPartitionStatus.vue'

const route = useRoute()

const navigationItems = [
  { name: 'overview', label: '总览', icon: ActivitySquare },
  { name: 'upload', label: '上传', icon: Upload },
  { name: 'batches', label: '批次', icon: Layers3 },
  { name: 'ocr-health', label: 'OCR 健康', icon: Gauge },
  { name: 'ocr-resources', label: 'OCR 资源', icon: ServerCog }
]

const activeTitle = computed(() => route.meta.title ?? 'DocLens 控制台')

const themeOverrides = createDashboardThemeOverrides()

/*
 * 顶栏上下文区只承载只读运行状态：
 * - App 继续保持应用壳职责，不读取或修改分区值。
 * - 分区和 principal 由独立组件处理，避免根组件膨胀。
 * - 状态展示放在实时读模型旁边，形成统一运行上下文。
 * - 窄屏布局允许换行，优先保证标题和状态都可读。
 * - 这里不提供切换入口，避免把分区键误当作登录凭证。
 */
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
            <div class="app-shell__topbar-actions">
              <!-- 当前请求上下文状态保持只读，真实授权仍以后端校验为准。 -->
              <PrincipalPartitionStatus />
              <span class="app-shell__live">
                <NIcon :component="Radio" />
                实时读模型
              </span>
            </div>
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
  grid-template-columns: 220px minmax(0, 1fr);
  background: var(--surface-canvas);
  color: var(--ink-strong);
}

.app-shell__sidebar {
  position: sticky;
  top: 0;
  display: flex;
  height: 100vh;
  flex-direction: column;
  gap: 16px;
  padding: 18px 12px;
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
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 8px;
  background: linear-gradient(135deg, var(--active), var(--active-strong));
  color: #ffffff;
  font-size: 19px;
  box-shadow: 0 10px 22px rgba(249, 115, 22, 0.22);
}

.app-shell__brand-copy {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.app-shell__brand-copy strong {
  font-size: 14px;
  line-height: 1.1;
}

.app-shell__brand-copy small {
  color: var(--ink-muted);
  font-size: 11px;
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
  min-height: 34px;
  padding: 0 10px;
  border-radius: 7px;
  color: var(--ink-soft);
  font-size: 12px;
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
  padding: 20px 22px;
}

.app-shell__topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

.app-shell__topbar-actions {
  /*
   * 顶栏右侧同时容纳 principal/分区状态和实时读模型状态。
   * max-width 防止长分区值挤压标题，组件自身再做省略。
   * flex-wrap 让窄屏和长文本都能自然换行。
   */
  display: inline-flex;
  max-width: 58%;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.app-shell__eyebrow {
  margin: 0 0 4px;
  color: var(--ink-muted);
  font-size: 10px;
  font-weight: 650;
  text-transform: uppercase;
}

.app-shell__title {
  margin: 0;
  font-size: 21px;
  font-weight: 750;
  letter-spacing: 0;
}

.app-shell__live {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 8px;
  border: 1px solid rgba(35, 132, 90, 0.32);
  border-radius: 999px;
  color: var(--success);
  font-size: 11px;
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

  .app-shell__topbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .app-shell__topbar-actions {
    /* 移动端状态区回到左对齐，减少横向空间争抢。 */
    max-width: 100%;
    justify-content: flex-start;
  }
}
</style>
