import type { UploadBatchOptions, UploadBatchResponse } from '@/types/upload'

const CREATE_BATCH_ENDPOINT = '/api/v1/batches'
const FILES_FIELD = 'files'
const METADATA_FIELD = 'metadata'
const CALLBACK_URL_FIELD = 'callback_url'
const IDEMPOTENCY_KEY_FIELD = 'idempotency_key'
const ADAPTER_OVERRIDE_FIELD = 'adapter_override'
const PDF_MODE_FIELD = 'pdf_mode'
const OCR_ROUTING_MODE_FIELD = 'ocrRoutingMode'
const OCR_MODEL_KEY_FIELD = 'ocrModelKey'
const OCR_NODE_ID_FIELD = 'ocrNodeId'
const OCR_LOAD_BALANCE_STRATEGY_FIELD = 'ocrLoadBalanceStrategy'

function appendOptional(formData: FormData, fieldName: string, value: string): void {
  const trimmedValue = value.trim()
  if (trimmedValue) {
    formData.append(fieldName, trimmedValue)
  } else {
    // 空表单字段不提交给后端，保持服务端默认行为。
  }
}

function createUploadFormData(options: UploadBatchOptions): FormData {
  const formData = new FormData()
  options.files.forEach((file) => {
    formData.append(FILES_FIELD, file)
  })
  appendOptional(formData, METADATA_FIELD, options.metadata)
  appendOptional(formData, CALLBACK_URL_FIELD, options.callbackUrl)
  appendOptional(formData, IDEMPOTENCY_KEY_FIELD, options.idempotencyKey)
  appendOptional(formData, ADAPTER_OVERRIDE_FIELD, options.adapterOverride)
  appendOptional(formData, PDF_MODE_FIELD, options.pdfMode)
  appendOptional(formData, OCR_ROUTING_MODE_FIELD, options.ocrRoutingMode)
  appendOptional(formData, OCR_MODEL_KEY_FIELD, options.ocrModelKey)
  appendOptional(formData, OCR_NODE_ID_FIELD, options.ocrNodeId)
  appendOptional(formData, OCR_LOAD_BALANCE_STRATEGY_FIELD, options.ocrLoadBalanceStrategy)
  return formData
}

export async function uploadBatch(options: UploadBatchOptions): Promise<UploadBatchResponse> {
  const response = await fetch(CREATE_BATCH_ENDPOINT, {
    method: 'POST',
    body: createUploadFormData(options)
  })
  if (response.ok) {
    return response.json() as Promise<UploadBatchResponse>
  } else {
    throw new Error(`上传失败：${response.status} ${response.statusText}`)
  }
}
