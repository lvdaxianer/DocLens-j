export interface LlmMarkdownConfigResponse {
  url: string
  model: string
  credential_configured: boolean
}

export interface LlmMarkdownConfigPayload {
  url: string
  model: string
  api_key?: string
}
