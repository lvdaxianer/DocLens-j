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
