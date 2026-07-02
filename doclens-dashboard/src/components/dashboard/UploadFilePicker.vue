<script setup lang="ts">
import { useTemplateRef } from 'vue'
import { UploadCloud } from '@lucide/vue'
import { NButton, NIcon } from 'naive-ui'

// UploadFilePicker 只负责浏览器原生文件入口。
// 维护边界：
// - accept 列表在这里声明，因为它直接绑定 input。
// - 点击按钮只触发原生 input.click。
// - 拖拽 drop 只提取 FileList，不转换数组。
// - FileList 转数组由父组件统一处理。
// - reset 只清空原生 input.value。
// - 不保存 selectedFiles，避免子父状态重复。
// - 不展示已选文件，文件列表由 UploadFileList 负责。
// - 不展示上传按钮，提交由 UploadDropzone 负责。
// - 不校验 metadata，metadata 属于高级参数。
// - 不校验 OCR 路由，路由属于 OcrRoutingSelector。
// - 不做文件大小校验，当前产品没有限制要求。
// - 不做文件类型二次校验，浏览器 accept 已给出选择约束。
// - 不处理上传 API，网络副作用只在父级 store。
// - 不读取 route/store，保证组件可单独测试和复用。
// - 不向用户展示错误消息，错误提示由编排层决定。
// - drop 事件必须 preventDefault，避免浏览器直接打开文件。
// - dragover 使用模板修饰符即可，不需要额外函数。
// - expose reset 是为了上传成功后父级清空原生 input。
// - 文件 input 隐藏但保留在 DOM 中，保证浏览器选择器可用。
// - 图标和文案属于选择区视觉，不放在父组件。
// - 移动端布局只在本组件内调整，不影响上传表单其它区块。
// - 如果未来支持目录上传，应优先扩展这里的 input 属性。
// - 如果未来支持拖拽 hover 态，也应在这里维护局部状态。
// - 如果未来支持粘贴上传，也应作为文件入口扩展在这里。
// - 不要把文件名列表放进这里，避免入口区和结果区耦合。
// - 不要把 accept 拆到父级，父级不应该关心 input 细节。
// - 不要在 reset 中 emit change，reset 是父级主动清理后的 DOM 同步。
// - 不要在 openFilePicker 里创建临时 input，保持模板可访问性稳定。
// - 不要把按钮改成 submit，避免触发表单默认提交。
// - 不要在 drop 中读取文件内容，只传递 FileList 引用。
// - 不要在这里格式化文件大小，文件展示属于列表组件。
// - 文案保持短句，避免拖拽区在窄屏变得过高。
// - 图标组件只在这里引入，父级不感知视觉实现。
// - NButton 只负责打开文件选择器，不绑定其它副作用。
// - reset 不清空父级 selectedFiles，父级会先清自己的状态。
// - change 事件允许 null，父级可以统一处理空列表。
// - disabled 由父级上传状态控制，用于请求中锁定选择入口。
// - disabled 时 click/change/drop 都不会上抛新文件列表。
// - disabled 时原生 input 也禁用，避免键盘或脚本路径绕过。
// - 不在模板里内联 accept 字符串，避免超长属性降低可读性。
// - 不在样式里使用全局选择器，避免影响其它上传组件。
// - 不改变原有支持格式集合，保证拆分不改变用户能力。
// - 不改变原有选择按钮文案，保证用户路径稳定。
const ACCEPTED_FILE_TYPES = '.pdf,.doc,.docx,.md,.markdown,.png,.jpg,.jpeg,.webp,.tif,.tiff,.txt'

const props = withDefaults(defineProps<{
  /** 上传请求进行中时禁用文件入口。 */
  disabled?: boolean
}>(), {
  disabled: false
})

const emit = defineEmits<{
  /** 原生 input 或拖拽区域产生新的文件列表。 */
  change: [files: FileList | null]
}>()

// useTemplateRef 让 reset/open 都能安全访问原生 input。
const fileInput = useTemplateRef<HTMLInputElement>('fileInput')

/**
 * 打开文件选择器。
 *
 * @returns 打开完成信号
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function openFilePicker(): void {
  if (props.disabled) {
    // 上传中锁定文件入口，不打开系统选择器。
  } else if (fileInput.value) {
    // 文件输入框已经挂载时打开系统选择器。
    fileInput.value.click()
  } else {
    // 文件输入框尚未挂载时不执行操作。
  }
}

/**
 * 处理文件选择事件。
 *
 * @param event 文件选择事件
 * @returns 处理完成信号
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function handleFileChange(event: Event): void {
  if (props.disabled) {
    // 上传中忽略 input change，避免请求中的 payload 被替换。
  } else {
    // input change 的 target 在这里明确收窄为 HTMLInputElement。
    const target = event.target as HTMLInputElement
    // 保持 FileList 原样上抛，父级统一转换为数组。
    emit('change', target.files)
  }
}

/**
 * 处理拖拽上传事件。
 *
 * @param event 拖拽事件
 * @returns 处理完成信号
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function handleDrop(event: DragEvent): void {
  event.preventDefault()
  if (props.disabled) {
    // 上传中忽略拖入文件，保持当前提交内容稳定。
  } else {
    // dataTransfer 可能为空，空值交给父级转换为空文件列表。
    emit('change', event.dataTransfer?.files ?? null)
  }
}

/**
 * 清空原生文件输入框。
 *
 * @returns 重置完成信号
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function reset(): void {
  if (fileInput.value) {
    // 文件输入框已经挂载时同步清空原生值。
    fileInput.value.value = ''
  } else {
    // 文件输入框尚未挂载，无需重置 DOM 值。
  }
}

defineExpose({ reset })
</script>

<template>
  <!-- 选择区同时承载点击选择和拖拽投放两个入口。 -->
  <!-- div 不使用 button 语义，因为内部已经包含真正按钮和 input。 -->
  <div
    class="upload-dropzone__target"
    :class="{ 'upload-dropzone__target--disabled': disabled }"
    :aria-disabled="disabled"
    @drop="handleDrop"
    @dragover.prevent
  >
    <!-- 原生 input 隐藏，由按钮点击间接触发。 -->
    <input
      ref="fileInput"
      class="upload-dropzone__input"
      type="file"
      multiple
      :accept="ACCEPTED_FILE_TYPES"
      :disabled="disabled"
      @change="handleFileChange"
    >
    <!-- 图标强化“上传/拖入”的入口感。 -->
    <NIcon class="upload-dropzone__target-icon" :component="UploadCloud" />
    <!-- 文案只描述支持的大类，详细类型由 accept 控制。 -->
    <div class="upload-dropzone__target-copy">
      <!-- strong 用于主行动文案，让用户先看到“选择或拖入”。 -->
      <strong>选择或拖入文件</strong>
      <!-- span 用于补充支持格式，保持弱化层级。 -->
      <span>PDF、Word、Markdown、图片、TXT</span>
    </div>
    <!-- 按钮保留显式选择入口，兼容不习惯拖拽的用户。 -->
    <NButton size="small" :disabled="disabled" @click="openFilePicker">
      选择文件
    </NButton>
  </div>
</template>

<style scoped>
/* 文件入口视觉需要明显，但不能抢过后续提交按钮。 */
/* dashed 边框是拖拽区域的常见 affordance。 */
/* 背景渐变增加可发现性，同时保持浅色低干扰。 */
/* 图标和文案横向排列，桌面端读取路径更短。 */
/* 小屏幕改为单列，避免按钮被挤出可视区域。 */
/* input display none 保留原生能力但隐藏默认控件。 */
/* hover 只强化边框和焦点环，不改变布局尺寸。 */
/* 所有尺寸都局部定义，避免依赖父组件样式。 */
/* 图标白底让它在浅色渐变上仍有足够对比度。 */
/* transition 只覆盖颜色/阴影，避免布局抖动。 */
/* 选择区使用 dashed 边框，明确这是可投放区域。 */
.upload-dropzone__target {
  /* grid 三列分别承载图标、文案和按钮。 */
  display: grid;
  /* min-height 保留足够拖拽投放面积。 */
  min-height: 164px;
  grid-template-columns: auto minmax(0, 1fr) auto;
  /* 垂直居中让三列在不同字号下仍对齐。 */
  align-items: center;
  gap: 14px;
  /* padding 让拖拽区域在视觉上足够可点击。 */
  padding: 22px;
  border: 1px dashed rgba(249, 115, 22, 0.48);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(255, 240, 229, 0.88), rgba(255, 255, 255, 0.82)),
    var(--surface-inset);
  transition: border-color 180ms ease, box-shadow 180ms ease, background-color 180ms ease;
}

.upload-dropzone__target:hover {
  border-color: var(--active);
  box-shadow: var(--focus-ring);
}

.upload-dropzone__target--disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.upload-dropzone__target--disabled:hover {
  border-color: rgba(249, 115, 22, 0.48);
  box-shadow: none;
}

.upload-dropzone__input {
  display: none;
}

.upload-dropzone__target-icon {
  display: grid;
  width: 48px;
  height: 48px;
  place-items: center;
  border-radius: 8px;
  background: #ffffff;
  color: var(--active);
  font-size: 32px;
  box-shadow: var(--shadow-soft);
}

.upload-dropzone__target-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.upload-dropzone__target-copy strong {
  color: var(--ink-strong);
  font-size: 18px;
}

.upload-dropzone__target-copy span {
  color: var(--ink-muted);
  font-size: 13px;
}

@media (max-width: 760px) {
  .upload-dropzone__target {
    grid-template-columns: 1fr;
  }
}
</style>
