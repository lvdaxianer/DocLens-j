<script setup lang="ts">
import { NButton } from 'naive-ui'

import type { BatchRow } from '@/types/dashboard'
import { formatDuration, formatPercent } from '@/utils/formatters'

// 摘要条不维护本地状态，只把父级给出的批次快照渲染出来。
// 这样删除、重试、自动刷新后的数据同步都只需要父级更新 selectedBatch。
// 如果后续增加指标，优先在父级确认后端字段，再在这里追加固定卡片。
// 不建议把指标数组化，因为字段格式化规则和业务顺序都不同。
// 本组件保持无副作用，方便在其它批次详情场景复用。
// 视觉密度以“首屏能看到表格”为优先级，而不是展示大号 KPI。
// 刷新按钮保留在摘要条内，是为了让用户在看指标时能立即重新拉取。
// 这里不展示 created_at/updated_at，避免摘要条变成批次详情表。
// 如果要新增时间信息，应放到详情 header 或独立信息面板。
// 维护提示：
// - 新增指标前先确认后端 BatchRow 是否已经提供稳定字段。
// - 新增指标时同步考虑移动端两列布局是否仍然可读。
// - 不要在这里添加删除、重试等文档级动作。
// - 不要在这里订阅自动刷新，避免和父级轮询重复。
// - 不要把 refreshIntervalSeconds 写死，后续会支持配置化。
// - 不要把 loading 绑定到整个 section，避免遮挡旧数据。
// - 不要在模板里拼接复杂单位，交给 formatter 或父级常量。
// - 不要把批次详情完整对象传入，避免子组件误用其它字段。
// - 不要把失败摘要塞进这里，失败摘要应有独立可展开区域。
// - 不要复用表格密度样式，摘要条有独立视觉层级。
// - 不要在这里处理空批次，父级负责加载和错误状态。
// - 不要把按钮事件改成异步函数，异步生命周期属于父级。
// - 不要在这里读取路由参数，摘要条应可被故事页独立挂载。
// - 不要把接口字段名直接展示给用户，保持产品文案稳定。
// - 不要把进度条放进摘要条，百分比数字已经足够表达进度。
// - 不要把成功率和失败率合并展示，排查时需要分别扫读。
/**
 * 批次摘要条只负责展示批次聚合指标。
 *
 * 组件约束：
 * - 不读取 store，避免展示组件承担数据获取职责。
 * - 不直接调用 API，刷新动作通过事件交给父级。
 * - 百分比和耗时统一使用 formatter，避免格式分叉。
 * - refreshIntervalSeconds 来自父级，和自动刷新策略保持同源。
 * - loading 只控制刷新按钮，避免遮挡已有指标。
 * - 网格样式放在组件内，避免污染其它面板。
 * - 移动端降为两列，保证指标仍可阅读。
 * - 指标顺序保持“进度、成功、失败、耗时、刷新”。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
defineProps<{
  /** 后端返回的批次聚合数据。 */
  batch: BatchRow
  /** 详情接口加载状态，仅用于刷新按钮。 */
  loading: boolean
  /** 自动刷新间隔秒数，用于向用户解释页面刷新节奏。 */
  refreshIntervalSeconds: number
}>()

// 只声明 refresh 一个事件，避免摘要组件未来偷偷承担其它行为。
const emit = defineEmits<{
  /** 用户点击刷新按钮时通知父级。 */
  refresh: []
}>()

// 事件处理函数单独命名，方便测试和代码审查识别刷新入口。
/**
 * 向父级详情页请求刷新批次数据。
 *
 * @returns 刷新事件发送结果
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function handleRefresh(): void {
  // 刷新由父级执行，避免展示组件直接触发 store 或 API。
  // 父级可以复用同一个 refresh 方法处理手动刷新和自动刷新。
  emit('refresh')
}
</script>

<template>
  <!-- 摘要条保持纯展示结构，父级负责决定是否渲染本组件。 -->
  <!-- 这里不用 v-for 渲染指标，是为了保留每个指标的明确业务顺序和类名。 -->
  <!-- 指标卡片不绑定点击事件，避免用户误以为可以进入二级筛选。 -->
  <!-- 刷新按钮放在最后，和“局部刷新”指标形成视觉关联。 -->
  <!-- 五个指标始终同时展示，避免用户在不同批次间看到跳变布局。 -->
  <!-- section 语义表示这是批次详情中的一组摘要信息。 -->
  <!-- class 采用 BEM 风格，避免和全局 panel/card 样式混淆。 -->
  <!-- 每个 article 都是独立指标单元，屏幕阅读器也能自然分组。 -->
  <!-- strong 只包裹数值，帮助视觉和语义都突出结果。 -->
  <!-- span 只包裹标签，保持标签样式统一。 -->
  <!-- 这里没有使用 title 属性，避免悬浮提示重复正文内容。 -->
  <!-- 所有文案都是用户可见文案，不暴露后端字段名。 -->
  <!-- 指标值不在子组件内缓存，父级刷新后立即响应式更新。 -->
  <!-- 组件没有 v-if 分支，父级负责 selectedBatch 的存在性判断。 -->
  <!-- 刷新按钮和刷新间隔相邻，降低用户理解成本。 -->
  <!-- 进度、成功率、失败率放在前面，优先回答“批次怎么样”。 -->
  <!-- 平均耗时放在成功/失败之后，作为性能排查辅助信息。 -->
  <!-- article 顺序不要随意调整，截图沟通时大家默认从左到右读。 -->
  <!-- 如果后续增加筛选入口，应新建组件，不要让摘要条承担导航。 -->
  <section class="batch-summary">
    <!-- 进度直接来自后端聚合值，避免前端重复推导批次进度。 -->
    <article class="batch-summary__item">
      <!-- 标签保持短文案，减少摘要条横向空间占用。 -->
      <span>进度</span>
      <!-- progress_percent 已经是百分制数值，不再二次格式化。 -->
      <strong>{{ batch.progress_percent }}%</strong>
    </article>
    <!-- 成功率统一走百分比格式化，和总览列表展示保持一致。 -->
    <article class="batch-summary__item">
      <!-- 成功率帮助用户判断批次整体健康度。 -->
      <span>成功率</span>
      <!-- formatter 负责兜底 undefined/NaN 等展示细节。 -->
      <strong>{{ formatPercent(batch.success_rate) }}</strong>
    </article>
    <!-- 失败率独立展示，方便用户快速判断是否需要进入详情排查。 -->
    <article class="batch-summary__item">
      <!-- 失败率不由成功率反推，直接使用后端统计结果。 -->
      <span>失败率</span>
      <!-- 失败率单独展示能辅助判断是否需要批量重试。 -->
      <strong>{{ formatPercent(batch.failure_rate) }}</strong>
    </article>
    <!-- 平均耗时使用统一 formatter，避免空值或毫秒直出。 -->
    <article class="batch-summary__item">
      <!-- 平均耗时用于粗略判断 OCR/LLM 链路是否变慢。 -->
      <span>平均耗时</span>
      <!-- duration formatter 统一 ms、秒、分钟的展示。 -->
      <strong>{{ formatDuration(batch.average_duration_ms) }}</strong>
    </article>
    <!-- 刷新节奏展示来自统一常量，提示用户列表不是静态结果。 -->
    <article class="batch-summary__item batch-summary__item--refresh">
      <!-- 局部刷新指的是当前批次详情刷新，不是全站刷新。 -->
      <span>局部刷新</span>
      <!-- 秒数直接来自父级常量，方便后续改配置。 -->
      <strong>{{ refreshIntervalSeconds }} 秒</strong>
    </article>
    <!-- 按钮只表达刷新意图；loading 来自父级 detailState。 -->
    <!-- small 尺寸和指标卡高度匹配，避免按钮破坏摘要条节奏。 -->
    <NButton size="small" :loading="loading" @click="handleRefresh">
      刷新
    </NButton>
  </section>
</template>

<style scoped>
/* 摘要条属于详情页首屏信息架构的一部分。 */
/* 样式目标是可扫读、占位小、和表格密度一致。 */
/* 不使用 CSS grid auto-fit，避免不同批次指标位置发生变化。 */
/* 不使用全局 utility class，减少后续页面调整时的连带影响。 */
  /* 指标卡片不设置 cursor，强调它们不是可点击入口。 */
  /* 按钮样式交给 Naive UI，避免重复实现交互态。 */
  /* 颜色全部来自主题变量，支持后续整体换肤。 */
  /* 移动端只调整列数，不改变 DOM 顺序。 */
  /* 这里不使用 margin，由父级 view-stack 控制组件间距。 */
  /* 组件内部只处理 grid gap，职责更清晰。 */
  /* 所有字号都显式设置，避免被 Naive UI 表格样式影响。 */
  /* 卡片背景使用 raised 层级，和下方 panel 主体区分开。 */
  /* 刷新按钮保持默认背景，避免被误读成主要提交按钮。 */
  /* 指标卡不设置 min-height，随内容自然撑开。 */
  /* 响应式规则只在一个 media 块内，方便后续统一调整断点。 */
  /* 如果主题变量缺失，CSS 会自然回退到浏览器默认继承色。 */
  /* scoped 样式让 BEM 命名不必全局唯一。 */
  /* 注释保留设计意图，防止后续把摘要条扩成复杂 dashboard。 */
/* 本组件样式全部 scoped，避免影响总览页或批次表格。 */
/* 指标条使用紧凑网格，优先把核心批次状态放在首屏。 */
.batch-summary {
  /* 五个指标加一个按钮，桌面端尽量保持一行。 */
  display: grid;
  /* minmax(0, 1fr) 允许长数值在卡片内部换行。 */
  grid-template-columns: repeat(5, minmax(0, 1fr)) auto;
  /* 小间距减少首屏占用，让下方表格露出更多内容。 */
  gap: 8px;
  /* stretch 让按钮和指标卡高度保持一致。 */
  align-items: stretch;
}

/* 每个指标卡片都允许内容换行，避免长耗时或异常百分比撑破布局。 */
.batch-summary__item {
  display: flex;
  /* 允许指标卡片在窄屏中收缩，不和按钮争宽。 */
  min-width: 0;
  /* 纵向排列能让标签和数值在紧凑卡片中保持清楚层级。 */
  flex-direction: column;
  /* 标签与数值距离很近，阅读时会被视为一个指标单元。 */
  gap: 2px;
  /* 紧凑内边距匹配详情页整体较小字号。 */
  padding: 8px 10px;
  /* 指标卡片不使用阴影，避免密集区域显得杂乱。 */
  /* 边框使用系统变量，和其它 rail 面板保持一致。 */
  border: 1px solid var(--rail-border);
  /* 小圆角减少卡片密集排列时的视觉噪音。 */
  border-radius: 8px;
  /* raised 背景让指标条从页面底色里浮出来。 */
  background: var(--surface-raised);
}

/* 标签用弱化颜色，突出真正需要扫读的数值。 */
.batch-summary__item span {
  /* 标签只做辅助信息，避免和数值抢视觉焦点。 */
  color: var(--ink-muted);
  /* 11px 是摘要标签的最小可读尺寸，减少高度占用。 */
  font-size: 11px;
}

/* 数值允许 anywhere 换行，兼容极端后端格式。 */
.batch-summary__item strong {
  /* anywhere 可以兜住异常长数字或后端兜底文案。 */
  overflow-wrap: anywhere;
  /* 数值用强文本色，让用户先看到批次健康度。 */
  color: var(--ink-strong);
  /* 16px 与全局较小字号形成层级，但不压迫表格区域。 */
  font-size: 16px;
  /* 紧凑行高避免数字换行时卡片过高。 */
  line-height: 1.25;
}

/* 刷新节奏是操作提示，不参与成功/失败语义色。 */
/* 刷新节奏使用强调色，提示这是动态页面。 */
.batch-summary__item--refresh strong {
  /* 只有刷新节奏使用强调色，避免五个数值全部抢焦点。 */
  color: var(--active-strong);
}

/* 小屏幕下两列展示，比横向滚动更容易读取。 */
@media (max-width: 900px) {
  /* 两列布局让五个指标自然换行，保留刷新按钮在末尾。 */
  /* 900px 与 dashboard 其它面板断点保持一致。 */
  /* 不单独隐藏指标，保证移动端信息和桌面端一致。 */
  .batch-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
