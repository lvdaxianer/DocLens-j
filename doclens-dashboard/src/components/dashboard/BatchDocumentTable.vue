<script setup lang="ts">
import { computed } from 'vue'
import { NDataTable } from 'naive-ui'

import { createBatchDocumentColumns } from '@/components/dashboard/batchDocumentTableColumns'
import type { DocumentRow } from '@/types/dashboard'

// 表格组件保持轻量，复杂列渲染统一委托给 TS 工厂文件。
/**
 * 文档表格组件只负责展示文档集合和上抛用户动作。
 *
 * 组件边界：
 * - 列配置放在独立工厂，避免 SFC 再次膨胀。
 * - props 全部来自父级，保持单向数据流。
 * - emit 只描述用户意图，不直接调用远程服务。
 * - loading 态交给 Naive UI 表格统一展示。
 * - selectedResultDocumentId 用于精确控制行内按钮 loading。
 * - retrying/deleting ID 避免整张表误显示加载。
 * - row-key 使用 document_id，保证分页和重渲染稳定。
 * - 样式只覆盖表格内部视觉密度。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
const DOCUMENT_TABLE_PAGE_SIZE = 12
// 每页 12 行与详情页首屏高度匹配，保留结果抽屉入口可见性。
// 横向滚动宽度覆盖文件、进度和三列操作，避免操作列被挤掉。
const DOCUMENT_TABLE_SCROLL_X = 1120

const props = defineProps<{
  /** 当前批次下的文档行集合。 */
  documents: DocumentRow[]
  /** 详情接口加载态，驱动表格统一 loading。 */
  loading: boolean
  /** 结果抽屉加载态，驱动单行查看按钮 loading。 */
  resultLoading: boolean
  /** 当前正在打开结果的文档 ID。 */
  selectedResultDocumentId: string
  /** 当前正在重试的文档 ID。 */
  retryingDocumentId: string
  /** 当前正在删除的文档 ID。 */
  deletingDocumentId: string
}>()

// 所有 emit 都是用户动作意图，不在本组件里解释业务成功或失败。
const emit = defineEmits<{
  /** 请求父级打开文档结果抽屉。 */
  openResult: [document: DocumentRow]
  /** 请求父级重试指定文档。 */
  retryDocument: [documentId: string]
  /** 请求父级删除指定文档。 */
  deleteDocument: [documentId: string]
}>()

// 列配置依赖行级 loading，因此用 computed 跟随 props 更新。
// 工厂函数返回纯列定义，组件重新渲染时不会额外触发远程副作用。
const columns = computed(() => createBatchDocumentColumns({
  resultLoading: props.resultLoading,
  selectedResultDocumentId: props.selectedResultDocumentId,
  retryingDocumentId: props.retryingDocumentId,
  deletingDocumentId: props.deletingDocumentId,
  openResult: handleOpenResult,
  retryDocument: handleRetryDocument,
  deleteDocument: handleDeleteDocument
}))

/**
 * 透传查看结果事件，保持父组件拥有抽屉状态。
 *
 * @param document 文档行
 * @returns 事件发送结果
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function handleOpenResult(document: DocumentRow): void {
  // 父级会根据文档行加载结果并打开抽屉。
  emit('openResult', document)
}

/**
 * 透传重试事件，保持父组件拥有远程请求副作用。
 *
 * @param documentId 文档 ID
 * @returns 事件发送结果
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function handleRetryDocument(documentId: string): void {
  // 父级负责调用 retry API 并刷新当前批次。
  emit('retryDocument', documentId)
}

/**
 * 透传删除事件，保持父组件拥有路由回退逻辑。
 *
 * @param documentId 文档 ID
 * @returns 事件发送结果
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function handleDeleteDocument(documentId: string): void {
  // 父级负责判断删除后刷新还是回退路由。
  emit('deleteDocument', documentId)
}

/**
 * 返回稳定的表格行主键。
 *
 * @param row 文档行
 * @returns 文档 ID
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function getDocumentRowKey(row: DocumentRow): string {
  // document_id 是后端稳定主键，适合作为表格行 key。
  return row.document_id
}
</script>

<template>
  <!-- 表格组件只接收父级状态，避免直接读取 store 或触发远程请求。 -->
  <!-- 数据直接来自 props，保持表格无本地副本。 -->
  <!-- loading 使用表格原生态，避免空白时误判为无数据。 -->
  <!-- 固定页大小和横向滚动，保证操作列在宽表下可达。 -->
  <!-- size=small 和 scoped CSS 一起压缩密度，让用户能看到更多文档。 -->
  <!-- row-key 使用函数而不是内联匿名表达式，减少模板里的逻辑噪音。 -->
  <!-- 分页对象保持内联常量，不引入额外可变状态。 -->
  <NDataTable
    :columns="columns"
    :data="documents"
    :loading="loading"
    :pagination="{ pageSize: DOCUMENT_TABLE_PAGE_SIZE }"
    :row-key="getDocumentRowKey"
    :scroll-x="DOCUMENT_TABLE_SCROLL_X"
    size="small"
  />
</template>

<style scoped>
/* 以下样式只服务于文档表格，不提升到全局样式，避免影响总览列表。 */
/* :deep 只用于 Naive UI 内部节点，其余样式仍保持组件作用域。 */
/* 文件名列限制宽度，避免长文件名挤压右侧操作列。 */
:deep(.document-name) {
  /* inline-block 才能同时控制宽度和省略号。 */
  display: inline-block;
  max-width: 240px;
  overflow: hidden;
  color: var(--ink-strong);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 阶段列保持一行展示，便于用户快速纵向扫读。 */
:deep(.stage-label) {
  /* 阶段文案可能较长，固定宽度能保护进度列。 */
  display: inline-block;
  max-width: 132px;
  overflow: hidden;
  color: var(--ink-soft);
  font-size: 12px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 进度条文本右对齐，降低百分比跳动造成的视觉抖动。 */
:deep(.n-progress-custom-content) {
  /* 最小宽度让 0% 到 100% 的宽度变化更稳定。 */
  min-width: 42px;
  color: var(--ink-soft);
  font-size: 11px;
  font-weight: 700;
  text-align: right;
}

/* 表格整体使用紧凑密度，让详情页能容纳更多文档行。 */
:deep(.n-data-table-th),
:deep(.n-data-table-td) {
  /* 单元格压缩到 12px 字号，满足详情页“多看几行”的要求。 */
  padding: 7px 10px;
  font-size: 12px;
  transition: background-color 120ms ease;
}

:deep(.n-data-table-tr:hover .n-data-table-td) {
  background: var(--surface-hover);
}
</style>
