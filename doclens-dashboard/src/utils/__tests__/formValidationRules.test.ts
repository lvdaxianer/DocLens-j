import test from 'node:test'
import assert from 'node:assert/strict'

import { createDefaultLlmMarkdownConfigForm, createLlmMarkdownConfigFormRules } from '../llmMarkdownConfigRules.ts'
import { createDefaultOcrGovernanceConfigForm, createOcrGovernanceConfigFormRules } from '../ocrGovernanceConfigRules.ts'
import { createDefaultOcrNodeForm, createOcrNodeFormRules } from '../ocrNodeFormRules.ts'
import { createDefaultUploadAdvancedOptions, createUploadAdvancedOptionsRules } from '../uploadFormRules.ts'

test('llm markdown form rules require model when url is filled', () => {
  const form = createDefaultLlmMarkdownConfigForm()
  form.url = 'https://llm.example.com/v1'

  const rules = createLlmMarkdownConfigFormRules(form)

  assert.equal(Array.isArray(rules.model), true)
})

test('ocr governance form rules mark every governance number as required', () => {
  const rules = createOcrGovernanceConfigFormRules(createDefaultOcrGovernanceConfigForm())

  assert.deepEqual(Object.keys(rules).sort(), [
    'circuitOpenSeconds',
    'failureThreshold',
    'manualRecoveryAttempts',
    'probeIntervalSeconds',
    'recoverySuccessThreshold'
  ].sort())
})

test('ocr node form rules expose conditional offline and online requirements', () => {
  const form = createDefaultOcrNodeForm('paddle_ocr')

  const offlineRules = createOcrNodeFormRules(form)
  assert.equal(Array.isArray(offlineRules.host), true)

  form.deploymentType = 'ONLINE'
  const onlineRules = createOcrNodeFormRules(form)
  assert.equal(Array.isArray(onlineRules.providerModel), true)
  assert.equal(Array.isArray(onlineRules.apiKey), true)
})

test('upload advanced options rules validate metadata json before submit', () => {
  const form = createDefaultUploadAdvancedOptions()
  form.metadata = '{"source":'

  const rules = createUploadAdvancedOptionsRules()

  assert.equal(Array.isArray(rules.metadata), true)
})
