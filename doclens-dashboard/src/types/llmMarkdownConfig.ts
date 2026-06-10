export type LlmMarkdownApiType = 'openai' | 'anthropic'

export interface LlmMarkdownConfigResponse {
  api_type: LlmMarkdownApiType
  url: string
  model: string
  credential_configured: boolean
  healthy: boolean
  health_message: string
  last_health_at?: string
}

export interface LlmMarkdownConfigPayload {
  api_type: LlmMarkdownApiType
  url: string
  model: string
  api_key?: string
}

export interface LlmMarkdownConfigTestResponse {
  healthy: boolean
  message: string
}
