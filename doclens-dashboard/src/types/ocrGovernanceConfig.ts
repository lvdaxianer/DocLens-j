export interface OcrGovernanceConfigResponse {
  failure_threshold: number
  probe_interval_seconds: number
  circuit_open_seconds: number
  recovery_success_threshold: number
  manual_recovery_attempts: number
}

export interface OcrGovernanceConfigPayload extends OcrGovernanceConfigResponse {
}
