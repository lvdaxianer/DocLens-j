export type LlmMarkdownApiType = 'openai' | 'anthropic'

export interface LlmMarkdownConfigResponse {
  id?: string
  name?: string
  api_type: LlmMarkdownApiType
  url: string
  model: string
  credential_env_var?: string
  credential_configured: boolean
  usage_type?: string
  priority?: number
  max_context_tokens?: number
  max_concurrency?: number
  request_interval_millis?: number
  is_default?: boolean
  enabled: boolean
  healthy: boolean
  health_message: string
  last_health_at?: string
}

export interface LlmMarkdownConfigPayload {
  name?: string
  api_type: LlmMarkdownApiType
  url: string
  model: string
  enabled: boolean
  usage_type: string
  priority: number
  is_default: boolean
  credential_env_var: string
  max_context_tokens: number
  max_concurrency: number
  request_interval_millis: number
}

export interface LlmMarkdownConfigTestResponse {
  healthy: boolean
  message: string
}
