<script setup lang="ts">
import { computed } from 'vue'
import { FilePlus2, X } from '@lucide/vue'
import { NButton, NIcon, NTag } from 'naive-ui'

import { formatNumber } from '@/utils/formatters'

// UploadFileList 只负责展示已选文件和移除入口。
// 维护边界：
// - 文件数组由父组件传入，本组件不保存副本。
// - 总大小从 props 派生，避免父组件重复计算。
// - 移除动作只上抛文件名，不直接改 props。
// - 空态在这里展示，父组件不需要判断 hasFiles。
// - 文件大小统一转成 KB，延续原有页面文案。
// - 文件名和大小都做省略，防止长文件名撑破布局。
// - 每个文件行使用 name+size 作为稳定 key。
// - 删除按钮只表达移除待上传列表，不触发后端删除。
// - NTag 展示数量和总大小，作为上传前的快速校验。
// - 不展示文件类型图标差异，当前用统一文件图标降低复杂度。
// - 不读取 input/drop 事件，文件入口由 UploadFilePicker 负责。
// - 不做上传提交，提交按钮属于 UploadDropzone。
// - 不校验 metadata 或 OCR 路由，避免职责扩散。
// - 不调用 message，用户提示由父组件或页面层负责。
// - disabled 由父级上传状态控制，用于请求中锁定移除入口。
// - 如果未来支持文件大小限制，应在这里展示文件级提示。
// - 如果未来支持重复文件提示，应由父级去重后传入标记字段。
// - 样式 scoped 到本组件，避免污染批次详情文件列表。
// - 文件列表高度有限，避免大批量选择时挤掉下面的表单。
// - 不展示文件最后修改时间，当前上传确认只需要名称和大小。
// - 不展示 MIME 类型，浏览器 File.type 对部分文件并不稳定。
// - 不排序文件，保持用户选择或拖入时的自然顺序。
// - 不在移除前弹确认，因为文件尚未上传到后端。
// - 不在这里清空全部文件，整体清空属于父组件操作区。
// - 不用 table，卡片列表在窄屏更容易适配。
// - 不使用索引作为 key，避免删除时 DOM 复用错位。
// - 不把 File 对象深拷贝，File 是浏览器原生只读对象。
// - 不把 totalSize 传给父级，父级提交不需要这个派生值。
// - 不把单位写成 MB，当前原页面就是 KB 展示。
// - 不在这里触发上传，列表只能表达“待上传”状态。
// - 上传中禁止移除，避免正在提交的 File 数组发生变化。
// - 组件名称保留 Upload 前缀，便于和上传功能相关文件聚合。
// - header 中的 NTag 是摘要，不承担筛选或按钮语义。
// - 空态文案保持温和提示，不把无文件状态当错误。
// - 移除按钮使用 aria-label，图标按钮仍可被读屏识别。
// - 列表项不展示预览，避免读取本地文件内容。
// - 组件不依赖 Naive UI 表格，减少小列表渲染开销。
// - 如果未来要支持拖拽排序，应在这里新增排序事件。
// - 当前不支持排序，保持用户选择顺序最可预期。
// - totalSize 使用 computed，files 变化时自动更新摘要。
// - formatFileSize 是展示函数，不参与提交 payload。
// - FILE_SIZE_UNIT 用常量表达 KB 换算，避免魔法数字。
// - files prop 保持必填，父级统一传空数组兜底。
// - remove 事件只传 fileName，延续原先 removeFile 签名。
// - 如果未来允许同名文件，应调整事件 payload 为文件索引或对象。
// - 当前保持同名移除行为不变，避免扩大改动范围。
// - 不把列表拆到 upload 目录，是因为当前父组件在 dashboard 目录。
// - 不在这里引入 upload store，列表组件必须保持纯展示。
// - 不在这里读取父级 loading，避免本地移除和远程上传耦合。
// - 不在这里展示上传成功结果，成功结果由 UploadView 侧栏展示。
// - 不在这里展示解析状态，解析状态属于 batch detail 页面。
// - 不在这里保存 totalSize，computed 足够表达派生关系。
// - 不在这里做文件名清洗，浏览器 File.name 已是展示来源。
// - 不在这里裁剪 files 数组，父级才拥有状态修改权。
// - 不在这里隐藏空态，空态能降低“按钮为何不可用”的困惑。
// - 不在这里使用 index key，删除首项时会造成行复用不直观。
// - 不在这里改变文件顺序，保持用户选择顺序最安心。
// - 不在这里做 i18n 抽象，项目当前文案仍是中文常量。
// - 不在这里添加批量删除，清空按钮已经在父组件动作区。
// - 不在这里监听键盘事件，按钮本身已经具备键盘可达性。
// - 不在这里使用 v-html，文件名必须作为文本安全渲染。
// - 不在这里做远程文件预检，上传接口负责最终校验。
const FILE_SIZE_UNIT = 1024

const props = withDefaults(defineProps<{
  /** 待上传文件数组。 */
  files: File[]
  /** 上传请求进行中时禁用移除入口。 */
  disabled?: boolean
}>(), {
  disabled: false
})

const emit = defineEmits<{
  /** 用户点击单个文件移除按钮。 */
  remove: [fileName: string]
}>()

// hasFiles 只服务模板空态，不作为父级提交判断。
const hasFiles = computed(() => props.files.length > 0)
// totalSize 从文件数组派生，避免额外维护总大小状态。
const totalSize = computed(() => props.files.reduce((sum, file) => sum + file.size, 0))

/**
 * 计算文件大小 KB 文案。
 *
 * @param size 文件字节数
 * @returns KB 展示文案
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function formatFileSize(size: number): string {
  // 使用 formatNumber 保持和 dashboard 其它数字展示一致。
  return `${formatNumber(Math.round(size / FILE_SIZE_UNIT))} KB`
}

/**
 * 通知父组件移除指定文件。
 *
 * @param fileName 文件名称
 * @returns 移除事件发送结果
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function handleRemove(fileName: string): void {
  if (props.disabled) {
    // 上传中保持待提交文件列表稳定，不上抛移除事件。
  } else {
    // 文件移除由父级修改数组，本组件保持 props 只读。
    emit('remove', fileName)
  }
}
</script>

<template>
  <!-- 文件列表区域展示数量、总大小和逐文件移除入口。 -->
  <!-- 根节点保持单一 div，方便父级纵向布局控制间距。 -->
  <div class="upload-dropzone__files">
    <!-- header 让用户上传前确认文件数量和总体积。 -->
    <div class="upload-dropzone__files-header">
      <span>待上传文件</span>
      <!-- NTag 聚合展示数量和总体积，减少额外行占位。 -->
      <NTag round>
        {{ formatNumber(files.length) }} 个 · {{ formatFileSize(totalSize) }}
      </NTag>
    </div>
    <!-- 有文件时展示滚动列表，避免大量文件撑高上传页。 -->
    <div v-if="hasFiles" class="upload-dropzone__file-list">
      <!-- 文件行只展示名称和大小，不展示本地路径以保护隐私。 -->
      <article v-for="file in files" :key="`${file.name}-${file.size}`" class="upload-file">
        <!-- 统一文件图标降低视觉噪音。 -->
        <NIcon class="upload-file__icon" :component="FilePlus2" />
        <!-- body 区域允许收缩，长文件名用省略号。 -->
        <div class="upload-file__body">
          <strong>{{ file.name }}</strong>
          <span>{{ formatFileSize(file.size) }}</span>
        </div>
        <!-- 移除按钮只影响待上传队列，不代表后端删除。 -->
        <NButton quaternary circle size="small" aria-label="移除文件" :disabled="disabled" @click="handleRemove(file.name)">
          <!-- X 图标放在 icon slot，保持 Naive UI 按钮尺寸正确。 -->
          <template #icon>
            <NIcon :component="X" />
          </template>
        </NButton>
      </article>
    </div>
    <!-- 空态由子组件兜底，父级无需条件渲染列表组件。 -->
    <p v-else class="upload-dropzone__empty">还没有选择文件</p>
  </div>
</template>

<style scoped>
/* 文件列表视觉目标是“确认已选文件”，不是展示处理进度。 */
/* header 和列表拆开，便于用户先扫数量和总大小。 */
/* 列表设置最大高度，避免选中文件太多导致提交按钮不可见。 */
/* 每行使用 grid，图标、文件信息和移除按钮稳定对齐。 */
/* 文件名和大小使用省略号，保护右侧移除按钮。 */
/* hover 只改变边框/背景，提示当前行可操作。 */
/* quaternary remove button 视觉较轻，避免误认为主操作。 */
/* 空态使用 muted 色，明确当前没有待上传文件。 */
/* 文件图标使用 active 色，和上传入口形成视觉连续性。 */
/* 行内 gap 小于父级 gap，表示它们属于同一列表组。 */
/* max-height 选择 280px，足够展示多文件又不压垮表单。 */
/* overflow auto 让大量文件仍然可以局部滚动查看。 */
/* border 使用 rail 变量，和 dashboard 其它列表行保持一致。 */
/* 背景使用 raised 变量，和上传选择区形成层级差。 */
/* 字号整体偏小，满足页面能看到更多内容的需求。 */
/* 文件列表使用纵向布局，和父级上传表单节奏一致。 */
.upload-dropzone__files {
  /* 纵向 gap 与父级表单区块节奏保持一致。 */
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.upload-dropzone__files-header {
  /* header 左右分布，让摘要数量靠右便于扫读。 */
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--ink-soft);
  font-size: 13px;
  font-weight: 650;
}

.upload-dropzone__file-list {
  /* 文件列表本身滚动，不挤压 OCR 路由和提交按钮。 */
  display: flex;
  max-height: 280px;
  flex-direction: column;
  gap: 8px;
  overflow: auto;
}

.upload-file {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1px solid var(--rail-border);
  border-radius: 8px;
  background: var(--surface-raised);
  transition: border-color 180ms ease, background-color 180ms ease;
}

.upload-file:hover {
  border-color: var(--rail-border-strong);
  background: var(--surface-hover);
}

.upload-file__icon {
  color: var(--active);
  font-size: 19px;
}

.upload-file__body {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.upload-file__body strong,
.upload-file__body span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.upload-file__body strong {
  color: var(--ink-strong);
  font-size: 13px;
}

.upload-file__body span,
.upload-dropzone__empty {
  color: var(--ink-muted);
  font-size: 12px;
}
</style>
