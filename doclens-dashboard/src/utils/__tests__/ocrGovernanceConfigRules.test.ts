import test from 'node:test'
import assert from 'node:assert/strict'

import {
  createDefaultOcrGovernanceConfigForm,
  createOcrGovernanceConfigPayload,
  fillOcrGovernanceConfigFormFromResponse,
  isOcrGovernanceConfigFormSubmittable
} from '../ocrGovernanceConfigRules.ts'

test('ocr governance config form exposes current values and payload fields', () => {
  const form = fillOcrGovernanceConfigFormFromResponse({
    failure_threshold: 3,
    probe_interval_seconds: 60,
    circuit_open_seconds: 86400,
    recovery_success_threshold: 3,
    manual_recovery_attempts: 3
  })

  assert.deepEqual(form, {
    failureThreshold: 3,
    probeIntervalSeconds: 60,
    circuitOpenSeconds: 86400,
    recoverySuccessThreshold: 3,
    manualRecoveryAttempts: 3
  })
  assert.equal(isOcrGovernanceConfigFormSubmittable(form), true)
  assert.deepEqual(createOcrGovernanceConfigPayload(form), {
    failure_threshold: 3,
    probe_interval_seconds: 60,
    circuit_open_seconds: 86400,
    recovery_success_threshold: 3,
    manual_recovery_attempts: 3
  })
})

test('ocr governance config form rejects non-positive values', () => {
  const form = createDefaultOcrGovernanceConfigForm()

  form.failureThreshold = 0
  assert.equal(isOcrGovernanceConfigFormSubmittable(form), false)

  form.failureThreshold = 3
  form.probeIntervalSeconds = -1
  assert.equal(isOcrGovernanceConfigFormSubmittable(form), false)
})
