import { describe, expect, it } from 'vitest'

import { apiErrorMessageFromResponse } from '@/api/apiError'

const DEFAULT_ERROR_MESSAGE = '请求失败'
const JSON_CONTENT_TYPE = 'application/json'

/*
 * API 错误解析测试约定：
 * - F2 只验证前端对后端稳定 code/detail 的解析。
 * - 测试不依赖具体业务 API，避免重复构造 fetch 流程。
 * - code 负责稳定分类，detail 负责展示具体原因。
 * - 旧接口只有 detail 时仍保持兼容。
 * - 网关或代理返回非 JSON 时保留 HTTP 状态兜底。
 * - 默认文案由调用方传入，适配上传和普通请求差异。
 * - 响应体只读一次，模拟真实 fetch Response 行为。
 * - 测试名称保持用户可见行为，不绑定内部实现函数。
 * - 后续新增错误码时优先在这里补映射用例。
 * - 这组测试作为所有 API 调用层复用 parser 的基础契约。
 * - `MISSING_PARTITION` 覆盖最常见的分区缺失恢复提示。
 * - detail-only 覆盖未改造完的历史接口兼容路径。
 * - 非结构化 body 覆盖反向代理或网关直接返回错误页。
 * - 测试不检查日志，日志由调用层测试覆盖。
 * - 测试不暴露 X-Doclens-Key 的真实值。
 * - 所有断言都面向用户最终看到的错误文案。
 */

/**
 * 创建错误响应对象。
 *
 * @param body - 响应体文本
 * @param status - HTTP 状态码
 * @returns 错误响应对象
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
function errorResponse(body: string, status = 400): Response {
  return new Response(body, {
    status,
    statusText: 'Bad Request',
    headers: { 'content-type': JSON_CONTENT_TYPE }
  })
}

describe('api error parser', () => {
  it('maps stable backend code and detail into one user facing message', async () => {
    const response = errorResponse(JSON.stringify({
      code: 'MISSING_PARTITION',
      detail: '缺少 X-Doclens-Key'
    }))

    await expect(apiErrorMessageFromResponse(response, DEFAULT_ERROR_MESSAGE))
      .resolves.toBe('缺少分区键：缺少 X-Doclens-Key')
  })

  it('uses backend detail when older responses do not include a stable code', async () => {
    const response = errorResponse(JSON.stringify({ detail: 'file type is not supported' }))

    await expect(apiErrorMessageFromResponse(response, DEFAULT_ERROR_MESSAGE))
      .resolves.toBe('file type is not supported')
  })

  it('falls back to HTTP status when response body is not structured json', async () => {
    const response = errorResponse('plain failure', 500)

    await expect(apiErrorMessageFromResponse(response, DEFAULT_ERROR_MESSAGE))
      .resolves.toBe('请求失败：500 Bad Request')
  })
})
