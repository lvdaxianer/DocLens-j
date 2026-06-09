import type { UploadAdvancedOptionsValue, UploadOcrRoutingOptions } from '@/types/upload'

export const DEFAULT_UPLOAD_METADATA_JSON = '{}'
export const DEFAULT_UPLOAD_LOAD_BALANCE_STRATEGY = 'least-inflight'

/**
 * 创建上传页默认高级选项。
 *
 * @returns 上传页默认高级选项
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function createDefaultUploadAdvancedOptions(): UploadAdvancedOptionsValue {
  return {
    metadata: DEFAULT_UPLOAD_METADATA_JSON,
    callbackUrl: '',
    idempotencyKey: ''
  }
}

/**
 * 创建上传页默认 OCR 路由。
 *
 * @returns 上传页默认 OCR 路由
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function createDefaultUploadOcrRouting(): UploadOcrRoutingOptions {
  return {
    ocrRoutingMode: 'GLOBAL_LOAD_BALANCE',
    ocrModelKey: '',
    ocrNodeId: '',
    ocrLoadBalanceStrategy: DEFAULT_UPLOAD_LOAD_BALANCE_STRATEGY
  }
}

/**
 * 校验上传元数据 JSON。
 *
 * @param metadataJson - 上传元数据 JSON 字符串
 * @returns 校验消息，空字符串表示通过
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function validateMetadataJson(metadataJson: string): string {
  try {
    JSON.parse(metadataJson.trim() || DEFAULT_UPLOAD_METADATA_JSON)
    return ''
  } catch {
    return '元数据 JSON 格式不正确'
  }
}
