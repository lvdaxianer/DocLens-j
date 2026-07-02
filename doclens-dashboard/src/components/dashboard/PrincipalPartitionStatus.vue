<script setup lang="ts">
import { computed, shallowRef } from 'vue'

/*
 * 顶栏身份状态展示约定：
 * - partition 读取当前会话的 X-Doclens-Key
 * - principal 优先读取网关注入后的可见会话值
 * - 缺少 principal 时显示可信网关注入状态
 * - 缺少 partition 时显示未设置，提示当前请求会被后端拒绝
 * - 组件只展示状态，不允许在这里修改分区或 principal
 * - F2 会继续接入后端稳定错误码做恢复提示
 * - sessionStorage 是当前 dashboard 已采用的分区来源
 * - localStorage 中的旧分区键不会被读取
 * - X-Doclens-Key 不是身份凭证，只作为数据隔离分区
 * - principal 是网关认证后的主体，不由用户在此输入
 * - 当前浏览器无法读取请求网关注入的服务端 header
 * - 所以缺省 principal 文案强调它来自可信网关
 * - 组件不触发网络请求，避免顶栏状态阻塞页面渲染
 * - 组件不展示 secret，也不把分区值写入日志
 * - 组件使用短文本标签，避免挤占页面主操作区
 * - 窄屏时通过 flex-wrap 保持可读，不遮挡标题
 * - warning 样式只用于缺失分区，不代表鉴权失败
 * - 后端仍是分区和 principal 授权的最终信任边界
 * - 前端展示仅提供操作者定位当前上下文
 * - 该组件保持无 props，避免每个页面重复传递状态
 * - 若后续网关暴露 principal API，可替换读取来源
 * - 替换读取来源时应保留相同的文案字段
 * - 替换读取来源时应保留缺省和异常兜底
 * - 状态 badge 不使用颜色作为唯一信息
 * - 文本内容始终包含“分区”和“Principal”
 * - 测试通过 App 顶栏挂载验证用户可见结果
 * - 分区值可能很长，样式使用 ellipsis 保持顶栏稳定
 * - principal 值可能不存在，缺省文案仍然保持正向说明
 * - 顶栏状态不使用按钮，避免用户误以为可以在此切换身份
 * - 顶栏状态不使用表单，避免把 caller 误解为登录凭证
 * - 顶栏状态紧邻实时读模型标签，形成当前上下文区域
 * - 组件没有 emit，因为没有上行交互事件
 * - 组件没有 props，因为状态来源固定为当前浏览器会话
 * - 样式采用 scoped class，避免影响页面内其他标签
 * - badge 最小高度固定，防止加载前后布局跳动
 * - badge 最大宽度固定，防止长分区挤压导航标题
 * - aria-label 描述整组状态，方便辅助技术快速理解
 * - warning 文案和颜色同时出现，避免只依赖颜色传义
 */
const PARTITION_STORAGE_KEY = 'X-Doclens-Key'
const PRINCIPAL_STORAGE_KEY = 'X-Doclens-Principal'
const EMPTY_PARTITION_LABEL = '未设置'
const TRUSTED_GATEWAY_LABEL = '可信网关注入'

/**
 * 安全读取会话存储值。
 *
 * @param key - sessionStorage 键名
 * @returns 去除空白后的会话值
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
function readSessionValue(key: string): string {
  try {
    return globalThis.sessionStorage?.getItem(key)?.trim() ?? ''
  } catch {
    return ''
  }
}

const partition = shallowRef(readSessionValue(PARTITION_STORAGE_KEY))
const principal = shallowRef(readSessionValue(PRINCIPAL_STORAGE_KEY))
const partitionLabel = computed(() => partition.value || EMPTY_PARTITION_LABEL)
const principalLabel = computed(() => principal.value || TRUSTED_GATEWAY_LABEL)
const hasPartition = computed(() => Boolean(partition.value))
</script>

<template>
  <!-- 只读上下文状态，帮助操作者确认当前数据隔离分区。 -->
  <div class="principal-partition-status" aria-label="当前 principal 与分区状态">
    <!-- 分区缺失时用 warning 样式提示后续 API 请求会缺少隔离键。 -->
    <span
      class="principal-partition-status__item"
      :class="{ 'principal-partition-status__item--warning': !hasPartition }"
    >
      分区：{{ partitionLabel }}
    </span>
    <!-- principal 缺省时展示可信网关注入语义，避免误导用户手动输入身份。 -->
    <span class="principal-partition-status__item">
      Principal：{{ principalLabel }}
    </span>
  </div>
</template>

<style scoped>
.principal-partition-status {
  display: inline-flex;
  max-width: 100%;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
}

.principal-partition-status__item {
  display: inline-flex;
  min-height: 24px;
  max-width: 240px;
  align-items: center;
  padding: 2px 8px;
  border: 1px solid rgba(15, 118, 110, 0.24);
  border-radius: 7px;
  overflow: hidden;
  background: rgba(240, 253, 250, 0.84);
  color: #0f766e;
  font-size: 11px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.principal-partition-status__item--warning {
  border-color: rgba(217, 119, 6, 0.28);
  background: rgba(255, 251, 235, 0.92);
  color: #b45309;
}
</style>
