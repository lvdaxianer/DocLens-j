export interface UploadBatchOptions {
  files: File[]
  metadata: string
  callbackUrl: string
  idempotencyKey: string
  adapterOverride: string
  pdfMode: string
  ocrRoutingMode: string
  ocrModelKey: string
  ocrNodeId: string
  ocrLoadBalanceStrategy: string
}

export interface UploadDocumentSummary {
  document_id: string
  file_name: string
  status: string
}

export interface UploadBatchResponse {
  batch_id: string
  status: string
  total_files: number
  documents: UploadDocumentSummary[]
}
