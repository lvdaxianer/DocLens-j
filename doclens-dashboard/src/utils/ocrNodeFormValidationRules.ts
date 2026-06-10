import type { FormRules } from 'naive-ui'

import type { OcrNodeFormState } from './ocrNodeFormRules.ts'

interface PositiveIntegerRangeRuleOptions {
  valueGetter: () => number
  min: number
  max: number
  message: string
}

/**
 * 创建 OCR 节点表单校验规则。
 *
 * @param form - OCR 节点表单状态
 * @returns Naive UI 表单校验规则
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
export function createOcrNodeFormRules(form: OcrNodeFormState): FormRules {
  return {
    ...createOfflineNodeFormRules(form),
    name: [requiredRule(() => form.name, '请输入节点名称')],
    ...createOnlineNodeFormRules(form),
    ...createNodeSchedulingFormRules(form)
  }
}

/**
 * 创建离线节点相关校验规则。
 *
 * @param form - OCR 节点表单状态
 * @returns 离线节点校验规则
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
function createOfflineNodeFormRules(form: OcrNodeFormState): FormRules {
  return {
    modelKey: [requiredWhenRule(() => form.deploymentType === 'OFFLINE', () => form.modelKey, '请选择 OCR 模型')],
    host: [requiredWhenRule(() => form.deploymentType === 'OFFLINE', () => form.host, '请输入 Host')],
    port: [positiveIntegerRangeRule({
      valueGetter: () => form.port,
      min: 1,
      max: 65535,
      message: '请输入 1-65535 的端口'
    })]
  }
}

/**
 * 创建在线节点相关校验规则。
 *
 * @param form - OCR 节点表单状态
 * @returns 在线节点校验规则
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
function createOnlineNodeFormRules(form: OcrNodeFormState): FormRules {
  return {
    channelKey: [requiredWhenRule(() => form.deploymentType === 'ONLINE', () => form.channelKey, '请选择在线渠道')],
    providerModel: [requiredWhenRule(() => form.deploymentType === 'ONLINE', () => form.providerModel, '请输入模型名称')],
    apiKey: [requiredWhenRule(() => form.deploymentType === 'ONLINE' && !form.credentialConfigured, () => form.apiKey, '请输入 API Key')]
  }
}

/**
 * 创建节点调度参数校验规则。
 *
 * @param form - OCR 节点表单状态
 * @returns 调度参数校验规则
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
function createNodeSchedulingFormRules(form: OcrNodeFormState): FormRules {
  return {
    weight: [positiveIntegerRangeRule({
      valueGetter: () => form.weight,
      min: 1,
      max: 10000,
      message: '请输入 1-10000 的权重'
    })],
    maxConcurrency: [positiveIntegerRangeRule({
      valueGetter: () => form.maxConcurrency,
      min: 1,
      max: 1000,
      message: '请输入 1-1000 的最大并发'
    })]
  }
}

/**
 * 创建必填校验规则。
 *
 * @param valueGetter - 字段取值函数
 * @param message - 校验失败提示
 * @returns Naive UI 单字段校验规则
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
function requiredRule(valueGetter: () => string, message: string) {
  return {
    validator: () => valueGetter().trim() !== '',
    message,
    trigger: ['input', 'blur', 'change']
  }
}

/**
 * 创建条件必填校验规则。
 *
 * @param conditionGetter - 条件取值函数
 * @param valueGetter - 字段取值函数
 * @param message - 校验失败提示
 * @returns Naive UI 单字段校验规则
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
function requiredWhenRule(conditionGetter: () => boolean, valueGetter: () => string, message: string) {
  return {
    validator: () => !conditionGetter() || valueGetter().trim() !== '',
    message,
    trigger: ['input', 'blur', 'change']
  }
}

/**
 * 创建正整数范围校验规则。
 *
 * @param options - 正整数范围校验配置
 * @returns Naive UI 单字段校验规则
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
function positiveIntegerRangeRule(options: PositiveIntegerRangeRuleOptions) {
  return {
    validator: () => {
      const value = options.valueGetter()
      return Number.isInteger(value) && value >= options.min && value <= options.max
    },
    message: options.message,
    trigger: ['input', 'blur', 'change']
  }
}
