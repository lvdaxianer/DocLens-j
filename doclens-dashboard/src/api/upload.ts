import type { UploadBatchOptions, UploadBatchResponse } from '@/types/upload'

const CREATE_BATCH_ENDPOINT = '/api/v1/batches'
const FILES_FIELD = 'files'
const METADATA_FIELD = 'metadata'
const CALLBACK_URL_FIELD = 'callback_url'
const IDEMPOTENCY_KEY_FIELD = 'idempotency_key'
const OCR_ROUTING_MODE_FIELD = 'ocrRoutingMode'
const OCR_MODEL_KEY_FIELD = 'ocrModelKey'
const OCR_NODE_ID_FIELD = 'ocrNodeId'
const OCR_LOAD_BALANCE_STRATEGY_FIELD = 'ocrLoadBalanceStrategy'

/**
 * 追加非空 multipart 字段。
 *
 * @param formData - multipart 表单数据
 * @param fieldName - 字段名称
 * @param value - 字段值
 * @returns 追加完成信号
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function appendOptional(formData: FormData, fieldName: string, value: string): void {
  const trimmedValue = value.trim()
  if (trimmedValue) {
    // 非空表单字段需要传给后端。
    formData.append(fieldName, trimmedValue)
  } else {
    // 空表单字段不提交给后端，保持服务端默认行为。
  }
}

/**
 * 创建上传批次 multipart 表单数据。
 *
 * @param options - 上传批次参数
 * @returns multipart 表单数据
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function createUploadFormData(options: UploadBatchOptions): FormData {
  const formData = new FormData()
  options.files.forEach((file) => {
    formData.append(FILES_FIELD, file)
  })
  appendOptional(formData, METADATA_FIELD, options.metadata)
  appendOptional(formData, CALLBACK_URL_FIELD, options.callbackUrl)
  appendOptional(formData, IDEMPOTENCY_KEY_FIELD, options.idempotencyKey)
  appendOptional(formData, OCR_ROUTING_MODE_FIELD, options.ocrRoutingMode)
  appendOptional(formData, OCR_MODEL_KEY_FIELD, options.ocrModelKey)
  appendOptional(formData, OCR_NODE_ID_FIELD, options.ocrNodeId)
  appendOptional(formData, OCR_LOAD_BALANCE_STRATEGY_FIELD, options.ocrLoadBalanceStrategy)
  return formData
}

/**
 * 提交上传批次。
 *
 * @param options - 上传批次参数
 * @returns 上传批次响应
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export async function uploadBatch(options: UploadBatchOptions): Promise<UploadBatchResponse> {
  const response = await fetch(CREATE_BATCH_ENDPOINT, {
    method: 'POST',
    body: createUploadFormData(options)
  })
  if (response.ok) {
    // 上传成功时返回后端批次信息。
    return response.json() as Promise<UploadBatchResponse>
  } else {
    // 上传失败时抛出明确错误给页面展示。
    throw new Error(`上传失败：${response.status} ${response.statusText}`)
  }
}
