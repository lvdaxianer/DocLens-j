/**
 * OCR 节点表格布局常量，约束操作列在 1440×960 视口下保持可见。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */

export const OCR_NODE_TABLE_ACTION_BUTTON_COUNT = 6
export const OCR_NODE_TABLE_ACTION_WIDTH = 28
export const OCR_NODE_TABLE_ACTION_GAP = 2
export const OCR_NODE_TABLE_ACTION_COLUMN_WIDTH = 192

/**
 * 判断 OCR 节点表格操作列是否需要固定在右侧。
 *
 * @returns 是否固定操作列
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function shouldStickOcrNodeTableActions(): boolean {
  return true
}
