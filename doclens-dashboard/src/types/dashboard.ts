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

export interface StageStatusCount {
  stage: string
  label: string
  document_count: number
  completed_images: number
  total_images: number
}

export interface ImageProgressSummary {
  completed_images: number
  total_images: number
  progress_percent: number
}

export interface DashboardThreadPoolMetrics {
  active_count: number
  queue_size: number
  pool_size?: number
  completed_task_count?: number
}

export interface DashboardOcrResources {
  healthy_node_count: number
  down_node_count: number
  recovering_node_count: number
  global_inflight_images: number
  nodes: DashboardOcrResourceNode[]
  busiest_node: {
    model_key?: string
    model_name?: string
    node_id?: string
    node_name?: string
    inflight_images?: number
  }
  thread_pools: {
    document_processing?: DashboardThreadPoolMetrics
    ocr_request?: DashboardThreadPoolMetrics
    ocr_health?: DashboardThreadPoolMetrics
    callback?: DashboardThreadPoolMetrics
    llm_markdown_chunk?: DashboardThreadPoolMetrics
  }
}

export interface DashboardOcrResourceNode {
  node_id: string
  node_name: string
  status?: 'UP' | 'DOWN' | 'RECOVERING' | 'DISABLED' | string
  last_health_at?: string
  last_error?: string
  processed_images_today: number
  success_images: number
  failed_images: number
  avg_latency_ms: number
  p95_latency_ms: number
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
  callback_url?: string
  idempotency_key?: string
  client_id?: string
  source_app?: string
  tenant_key?: string
  metadata?: Record<string, unknown>
}

export interface ProcessingTrackNode {
  name: string
  active: boolean
  state?: 'done' | 'current' | 'pending' | 'skipped' | 'failed'
  description?: string
}

export interface DocumentRow {
  document_id: string
  batch_id: string
  file_name: string
  file_type: string
  status: string
  stage: string
  progress_percent: number
  current_page: number
  total_pages: number
  duration_ms: number
  llm_chunk_count: number
  track: ProcessingTrackNode[]
  ocr_final_hit_nodes: BatchOcrHitNode[]
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

/**
 * Dashboard 批次详情中的回调投递任务读模型。
 *
 * @author lvdaxianerplus
 * @date 2026-06-16
 */
export interface CallbackJobRow {
  /** 回调任务 ID。 */
  callback_job_id: string
  /** 产生回调任务的完成事件 ID。 */
  event_id: string
  /** 回调任务所属批次 ID。 */
  batch_id: string
  /** 可选文档 ID。 */
  document_id: string
  /** 回调投递目标地址。 */
  callback_url: string
  /** 回调任务状态编码。 */
  status: string
  /** 已记录的投递失败次数。 */
  retry_count: number
  /** 下次自动重试时间。 */
  next_retry_at: string
  /** 最近一次失败原因编码。 */
  failure_reason: string
  /** 最近一次失败详情。 */
  failure_detail: string
  /** 最近更新时间。 */
  updated_at: string
}

export interface DashboardSummary {
  overview: DashboardOverview
  throughput: DashboardThroughput
  stage_status_counts: StageStatusCount[]
  image_progress: ImageProgressSummary
  ocr_resources: DashboardOcrResources
  recent_batches: BatchRow[]
  recent_failures: DocumentRow[]
  recent_events: OcrEventRow[]
}

export interface BatchOcrRoutePolicy {
  routing_mode: string
  model_key: string
  model_name?: string
  node_id: string
  node_name?: string
  load_balance_strategy: string
}

export interface BatchOcrHitNode {
  model_key: string
  model_name?: string
  node_id: string
  node_name?: string
  image_count: number
}

export interface BatchListResponse {
  items: BatchRow[]
  total: number
}

export interface BatchDetailResponse {
  batch: BatchRow
  documents: DocumentRow[]
  events: OcrEventRow[]
  callback_jobs: CallbackJobRow[]
  ocr_route_policy: BatchOcrRoutePolicy
  batch_dispatch_hit_nodes: BatchOcrHitNode[]
  failure_summary: Record<string, number>
}

/**
 * 文档解析结果载荷。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
export interface DocumentResultPayload {
  finalText: string
  llm_markdown_applied: boolean
  llm_error_message?: string
  rawVendorOutput?: Record<string, unknown>
  markdownStorageUri: string
  pages: unknown[]
  confidence: number
  warnings: string[]
  summary: {
    pageCount: number
    blockCount: number
    tableCount: number
    confidence: number
  }
}

/**
 * 文档解析结果接口响应。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
export interface DocumentResultResponse {
  document_id: string
  result_id: string
  result: DocumentResultPayload
}

export interface OcrHealthResponse {
  adapter_key: string
  success_rate: number
  failure_rate: number
  average_duration_ms: number
  ocr_resources: DashboardOcrResources
  recent_failures: DocumentRow[]
}
