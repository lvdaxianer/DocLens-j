export type LlmMarkdownApiType = 'openai' | 'anthropic'

export interface LlmMarkdownConfigResponse {
  id?: string
  name?: string
  api_type: LlmMarkdownApiType
  url: string
  model: string
  credential_configured: boolean
  usage_type?: string
  priority?: number
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
  api_key?: string
}

export interface LlmMarkdownConfigTestResponse {
  healthy: boolean
  message: string
}
