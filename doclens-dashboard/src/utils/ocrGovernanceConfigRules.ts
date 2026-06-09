import type { OcrGovernanceConfigPayload, OcrGovernanceConfigResponse } from '@/types/ocrGovernanceConfig'

export interface OcrGovernanceConfigFormState {
  failureThreshold: number
  probeIntervalSeconds: number
  circuitOpenSeconds: number
  recoverySuccessThreshold: number
  manualRecoveryAttempts: number
}

const DEFAULT_FAILURE_THRESHOLD = 3
const DEFAULT_PROBE_INTERVAL_SECONDS = 60
const DEFAULT_CIRCUIT_OPEN_SECONDS = 86400
const DEFAULT_RECOVERY_SUCCESS_THRESHOLD = 3
const DEFAULT_MANUAL_RECOVERY_ATTEMPTS = 3

/**
 * 创建 OCR 全局治理配置表单默认值。
 *
 * @returns OCR 全局治理配置表单状态
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
export function createDefaultOcrGovernanceConfigForm(): OcrGovernanceConfigFormState {
  return {
    failureThreshold: DEFAULT_FAILURE_THRESHOLD,
    probeIntervalSeconds: DEFAULT_PROBE_INTERVAL_SECONDS,
    circuitOpenSeconds: DEFAULT_CIRCUIT_OPEN_SECONDS,
    recoverySuccessThreshold: DEFAULT_RECOVERY_SUCCESS_THRESHOLD,
    manualRecoveryAttempts: DEFAULT_MANUAL_RECOVERY_ATTEMPTS
  }
}

/**
 * 将接口响应转换为编辑表单状态。
 *
 * @param response - OCR 全局治理配置响应
 * @returns OCR 全局治理配置表单状态
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
export function fillOcrGovernanceConfigFormFromResponse(
  response: OcrGovernanceConfigResponse
): OcrGovernanceConfigFormState {
  return {
    failureThreshold: response.failure_threshold,
    probeIntervalSeconds: response.probe_interval_seconds,
    circuitOpenSeconds: response.circuit_open_seconds,
    recoverySuccessThreshold: response.recovery_success_threshold,
    manualRecoveryAttempts: response.manual_recovery_attempts
  }
}

/**
 * 判断 OCR 全局治理配置表单是否可提交。
 *
 * @param form - OCR 全局治理配置表单状态
 * @returns 是否可提交
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
export function isOcrGovernanceConfigFormSubmittable(form: OcrGovernanceConfigFormState): boolean {
  return [
    form.failureThreshold,
    form.probeIntervalSeconds,
    form.circuitOpenSeconds,
    form.recoverySuccessThreshold,
    form.manualRecoveryAttempts
  ].every(isPositiveInteger)
}

/**
 * 创建 OCR 全局治理配置提交载荷。
 *
 * @param form - OCR 全局治理配置表单状态
 * @returns OCR 全局治理配置提交载荷
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
export function createOcrGovernanceConfigPayload(form: OcrGovernanceConfigFormState): OcrGovernanceConfigPayload {
  return {
    failure_threshold: normalizePositiveInteger(form.failureThreshold),
    probe_interval_seconds: normalizePositiveInteger(form.probeIntervalSeconds),
    circuit_open_seconds: normalizePositiveInteger(form.circuitOpenSeconds),
    recovery_success_threshold: normalizePositiveInteger(form.recoverySuccessThreshold),
    manual_recovery_attempts: normalizePositiveInteger(form.manualRecoveryAttempts)
  }
}

/**
 * 判断数值是否为正整数。
 *
 * @param value - 待校验数值
 * @returns 是否为正整数
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
function isPositiveInteger(value: number): boolean {
  return Number.isInteger(value) && value > 0
}

/**
 * 归一化表单中的正整数值。
 *
 * @param value - 原始数值
 * @returns 归一化后的正整数
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
function normalizePositiveInteger(value: number): number {
  return Math.max(1, Math.trunc(value))
}
