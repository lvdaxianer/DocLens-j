export interface DashboardOverview {
  batch_count: number
  document_count: number
  completed_documents: number
  failed_documents: number
  processing_documents: number
  success_rate: number
  failure_rate: number
  average_duration_ms: number
}

export interface DashboardThroughput {
  completed_last_window: number
  window_size: number
}

export interface BatchRow {
  batch_id: string
  status: string
  total_files: number
  completed_files: number
  failed_files: number
  progress_percent: number
  success_rate: number
  failure_rate: number
  average_duration_ms: number
  created_at: string
  updated_at: string
}

export interface ProcessingTrackNode {
  name: string
  active: boolean
}

export interface DocumentRow {
  document_id: string
  batch_id: string
  file_name: string
  file_type: string
  status: string
  stage: string
  progress_percent: number
  duration_ms: number
  track: ProcessingTrackNode[]
  error_code: string
  error_message: string
  updated_at: string
}

export interface OcrEventRow {
  event_id: string
  event_type: string
  batch_id: string
  document_id: string
  status: string
  stage: string
  occurred_at: string
}

export interface DashboardSummary {
  overview: DashboardOverview
  throughput: DashboardThroughput
  recent_batches: BatchRow[]
  recent_failures: DocumentRow[]
  recent_events: OcrEventRow[]
}

export interface BatchListResponse {
  items: BatchRow[]
  total: number
}

export interface BatchDetailResponse {
  batch: BatchRow
  documents: DocumentRow[]
  events: OcrEventRow[]
  failure_summary: Record<string, number>
}

export interface OcrHealthResponse {
  adapter_key: string
  success_rate: number
  failure_rate: number
  average_duration_ms: number
  recent_failures: DocumentRow[]
}
