import type { UploadBatchOptions, UploadBatchResponse } from '@/types/upload'

const CREATE_BATCH_ENDPOINT = '/api/v1/batches'
const FILES_FIELD = 'files'
const METADATA_FIELD = 'metadata'
const CALLBACK_URL_FIELD = 'callback_url'
const IDEMPOTENCY_KEY_FIELD = 'idempotency_key'
const ADAPTER_OVERRIDE_FIELD = 'adapter_override'
const PDF_MODE_FIELD = 'pdf_mode'

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
