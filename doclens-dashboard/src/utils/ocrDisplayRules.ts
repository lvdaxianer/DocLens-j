export interface OcrNodeDisplaySource {
  id?: string
  name?: string
  nodeId?: string
  nodeName?: string
}

export interface OcrModelDisplaySource {
  modelKey?: string
  name?: string
}

export interface OcrNodeSubtitleSource {
  id?: string
  nodeId?: string
  name?: string
  nodeName?: string
  modelKey?: string
  imageCount?: number
}

const ROUTING_MODE_LABELS: Record<string, string> = {
  DEFAULT: '全局负载均衡',
  GLOBAL_LOAD_BALANCE: '全局负载均衡',
  MODEL_LOAD_BALANCE: '指定 OCR',
  SPECIFIC_NODE: '指定节点'
}

const LOAD_BALANCE_STRATEGY_LABELS: Record<string, string> = {
  'least-inflight': '最少解析中图片'
}

/**
 * 优先返回 OCR 节点别名。
 *
 * @param node - 节点显示源
 * @returns 节点显示名
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function displayNodeName(node: OcrNodeDisplaySource): string {
  const alias = (node.nodeName ?? node.name ?? '').trim()
  if (alias) {
    // 节点别名存在时作为主显示文本。
    return alias
  } else {
    // 别名缺失时才回退内部节点 ID。
    return (node.nodeId ?? node.id ?? '-').trim() || '-'
  }
}

/**
 * 生成节点辅助说明。
 *
 * @param node - 节点辅助信息源
 * @returns 节点辅助说明
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function displayNodeSubtitle(node: OcrNodeSubtitleSource): string {
  const parts = [
    node.modelKey?.trim(),
    displayNodeName(node),
    typeof node.imageCount === 'number' ? `${node.imageCount} 张图片` : ''
  ].filter((part): part is string => Boolean(part))
  return parts.join(' · ')
}

/**
 * 优先返回 OCR 模型名称。
 *
 * @param model - 模型显示源
 * @returns 模型显示名
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function displayModelName(model: OcrModelDisplaySource): string {
  const name = (model.name ?? '').trim()
  if (name) {
    // 模型名称存在时作为主显示文本。
    return name
  } else {
    // 名称缺失时才回退模型 key。
    return model.modelKey?.trim() || '-'
  }
}

/**
 * 返回用户可理解的 OCR 路由模式展示文案。
 *
 * @param routingMode - 路由模式枚举值
 * @returns 路由模式文案
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function displayRoutingMode(routingMode?: string): string {
  const normalizedMode = routingMode?.trim() || 'DEFAULT'
  return ROUTING_MODE_LABELS[normalizedMode] ?? normalizedMode
}

/**
 * 返回用户可理解的负载均衡策略展示文案。
 *
 * @param strategy - 负载均衡策略
 * @returns 策略文案
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function displayLoadBalanceStrategy(strategy?: string): string {
  const normalizedStrategy = strategy?.trim()
  if (normalizedStrategy) {
    return LOAD_BALANCE_STRATEGY_LABELS[normalizedStrategy] ?? normalizedStrategy
  } else {
    return '-'
  }
}
