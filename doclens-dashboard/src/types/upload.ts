export type OcrRoutingMode = 'DEFAULT' | 'GLOBAL_LOAD_BALANCE' | 'MODEL_LOAD_BALANCE' | 'SPECIFIC_NODE'
export type UploadChunkStrategy = 'GENERAL' | 'NEWS' | 'TECHNICAL' | 'ACADEMIC'

export interface UploadOcrRoutingOptions {
  ocrRoutingMode: OcrRoutingMode
  ocrModelKey: string
  ocrNodeId: string
  ocrLoadBalanceStrategy: string
}

export interface UploadAdvancedOptionsValue {
  metadata: string
  callbackUrl: string
  idempotencyKey: string
}

export interface UploadChunkStrategyOptions {
  chunkStrategy: UploadChunkStrategy
}

export interface UploadBatchOptions {
  files: File[]
  metadata: string
  callbackUrl: string
  idempotencyKey: string
  ocrRoutingMode: OcrRoutingMode
  ocrModelKey: string
  ocrNodeId: string
  ocrLoadBalanceStrategy: string
  chunkStrategy: UploadChunkStrategy
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
