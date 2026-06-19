import type { UploadAdvancedOptionsValue, UploadChunkStrategyOptions, UploadOcrRoutingOptions } from '@/types/upload'
import type { FormRules } from 'naive-ui'

export const DEFAULT_UPLOAD_METADATA_JSON = '{}'
export const DEFAULT_UPLOAD_LOAD_BALANCE_STRATEGY = 'weighted-idle'
export const DEFAULT_UPLOAD_CHUNK_STRATEGY = 'GENERAL'
const CALLBACK_TEXT_SOURCE_HINT = '若启用且成功执行 LLM Markdown 后处理则返回 Markdown，否则返回 OCR 合并纯文本'

export interface UploadCallbackContractHints {
  method: 'POST'
  body: {
    meta: Record<string, never>
    text: Record<string, never>
    idempotency_key: string
  }
  textSourceHint: string
}

/**
 * 创建上传高级选项表单校验规则。
 *
 * @returns Naive UI 表单校验规则
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
export function createUploadAdvancedOptionsRules(): FormRules {
  return {
    metadata: [{
      validator: (_rule, value: string) => validateMetadataJson(value) === '',
      message: '元数据 JSON 格式不正确',
      trigger: ['input', 'blur']
    }]
  }
}

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
 * 创建上传页默认分块策略。
 *
 * @returns 上传页默认分块策略
 * @author lvdaxianerplus
 * @date 2026-06-19
 */
export function createDefaultUploadChunkStrategy(): UploadChunkStrategyOptions {
  return {
    chunkStrategy: DEFAULT_UPLOAD_CHUNK_STRATEGY
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

/**
 * 返回上传完成回调的固定契约提示。
 *
 * @returns 回调契约提示
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function callbackContractHints(): UploadCallbackContractHints {
  return {
    method: 'POST',
    body: {
      meta: {},
      text: {},
      idempotency_key: ''
    },
    textSourceHint: CALLBACK_TEXT_SOURCE_HINT
  }
}
