export type OcrNodeStatus = 'UP' | 'DOWN' | 'RECOVERING' | 'DISABLED'
export type OcrNodeDeploymentType = 'OFFLINE' | 'ONLINE'
export type OcrOnlineChannelKey = 'aliyun_bailian_dashscope'
export type OcrNodeChannelKey = OcrOnlineChannelKey | 'ollama'

export interface OcrModel {
  model_key: string
  name?: string
  description?: string
  supported_inputs: string[]
  ocr_path: string
  health_path: string
  node_count: number
  healthy_node_count: number
  enabled_node_count: number
}

export interface OcrModelListResponse {
  items: OcrModel[]
}

export interface OcrNodeMetrics {
  inflight_images: number
  queued_images: number
  processed_images_today: number
  success_images: number
  failed_images: number
  avg_latency_ms: number
  p95_latency_ms: number
}

export interface OcrNode {
  id: string
  model_key: string
  deployment_type: OcrNodeDeploymentType
  name: string
  host: string
  port: number
  channel_key: string
  provider_model: string
  credential_env_var?: string
  credential_configured: boolean
  enabled: boolean
  participate_global: boolean
  weight: number
  max_concurrency: number
  status: OcrNodeStatus
  inflight_images: number
  queued_images: number
  processed_images_today: number
  success_images: number
  failed_images: number
  avg_latency_ms: number
  p95_latency_ms: number
  failure_count: number
  recovery_success_count: number
  last_health_at: string
  circuit_open_until: string
  last_manual_recovery_at: string
  last_error: string
}

export interface OcrNodeListResponse {
  items: OcrNode[]
}

export interface OcrNodeCall {
  id: string
  document_id: string
  image_index: number
  status: string
  retry_count: number
  duration_ms: number
  error_message: string
  started_at: string
  finished_at: string
}

export interface OcrNodeCallListResponse {
  items: OcrNodeCall[]
}

export interface OcrNodePayload {
  deployment_type: OcrNodeDeploymentType
  name: string
  host?: string
  port?: number
  channel_key?: OcrNodeChannelKey
  provider_model?: string
  credential_env_var?: string
  enabled: boolean
  participate_global: boolean
  weight: number
  max_concurrency: number
}

export interface OcrNodeSubmitPayload {
  modelKey: string
  node: OcrNodePayload
}

export interface OcrNodeTestResponse {
  healthy: boolean
  message: string
}

export interface OcrNodeReconnectResponse {
  healthy: boolean
  attempts: number
  status: OcrNodeStatus
  circuit_open_until: string
}
