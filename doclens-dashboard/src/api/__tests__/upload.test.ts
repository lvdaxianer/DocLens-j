import { afterEach, describe, expect, it, vi } from 'vitest'

import { uploadBatch } from '@/api/upload'
import type { UploadBatchOptions } from '@/types/upload'

/**
 * 创建上传测试参数。
 *
 * @returns 上传测试参数
 * @author lvdaxianerplus
 * @date 2026-06-16
 */
function uploadOptions(): UploadBatchOptions {
  return {
    files: [new File(['demo'], 'demo.txt', { type: 'text/plain' })],
    metadata: '{}',
    callbackUrl: '',
    idempotencyKey: '1001',
    ocrRoutingMode: 'GLOBAL_LOAD_BALANCE',
    ocrModelKey: '',
    ocrNodeId: '',
    ocrLoadBalanceStrategy: 'weighted-idle'
  }
}

describe('uploadBatch errors', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('shows backend detail for structured upload failures', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(
      JSON.stringify({ detail: 'file type is not supported' }),
      { status: 400, statusText: 'Bad Request', headers: { 'content-type': 'application/json' } }
    ))

    await expect(uploadBatch(uploadOptions())).rejects.toThrow('file type is not supported')
  })

  it('falls back to status text when detail is missing', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('', {
      status: 500,
      statusText: 'Internal Server Error'
    }))

    await expect(uploadBatch(uploadOptions())).rejects.toThrow('上传失败：500 Internal Server Error')
  })
})
