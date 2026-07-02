/**
 * 构建批次删除确认文案。
 *
 * @param batchId - 批次 ID
 * @returns 批次删除确认文案
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
export function batchDeleteConfirmMessage(batchId: string): string {
  return `将删除批次 ${batchId}、批次内文档、结果与关联存储。此操作不可恢复。`
}

/**
 * 构建文档删除确认文案。
 *
 * @param fileName - 文档文件名
 * @param documentId - 文档 ID
 * @returns 文档删除确认文案
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
export function documentDeleteConfirmMessage(fileName: string, documentId: string): string {
  return `将删除文档 ${entityLabel(fileName, documentId)} 的结果与关联存储。此操作不可恢复。`
}

/**
 * 构建 LLM 配置删除确认文案。
 *
 * @param name - 配置名称
 * @param id - 配置 ID
 * @returns LLM 配置删除确认文案
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
export function llmConfigDeleteConfirmMessage(name: string, id: string): string {
  return `将删除 LLM 配置 ${entityLabel(name, id)}。此操作不可恢复。`
}

/**
 * 构建 OCR 节点删除确认文案。
 *
 * @param name - 节点名称
 * @param id - 节点 ID
 * @returns OCR 节点删除确认文案
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
export function ocrNodeDeleteConfirmMessage(name: string, id: string): string {
  return `将删除 OCR 节点 ${entityLabel(name, id)}。此操作不可恢复。`
}

/**
 * 生成带 ID 的实体标签。
 *
 * @param name - 实体名称
 * @param id - 实体 ID
 * @returns 实体标签
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
function entityLabel(name: string, id: string): string {
  const trimmedName = name.trim()
  if (trimmedName) {
    // 名称存在时同时展示名称和 ID，方便用户确认具体对象。
    return `${trimmedName}（${id}）`
  } else {
    // 名称缺失时直接使用 ID，避免确认文案出现空目标。
    return id
  }
}
