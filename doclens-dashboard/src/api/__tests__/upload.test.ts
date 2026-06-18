import { afterEach, describe, expect, it, vi } from 'vitest'

import { uploadBatch } from '@/api/upload'
import type { UploadBatchOptions } from '@/types/upload'

const CALLER_CREDENTIAL_STORAGE_KEY = 'X-DocLens-Credential-Key'
const LEGACY_CALLER_CREDENTIAL_STORAGE_KEY = 'X-DocLens-Credential'
const CALLER_API_KEY_HEADER = 'X-DocLens-Api-Key'

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

afterEach(() => {
  localStorage.clear()
  sessionStorage.clear()
  vi.restoreAllMocks()
})

describe('uploadBatch errors', () => {
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

describe('uploadBatch caller credential headers', () => {
  it('attaches X-DocLens-Api-Key when sessionStorage provides X-DocLens-Credential-Key', async () => {
    sessionStorage.setItem(CALLER_CREDENTIAL_STORAGE_KEY, 'test-api-key')
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('{}', { status: 200 }))

    await uploadBatch(uploadOptions())

    const [, init] = fetchSpy.mock.calls[0] ?? []
    expect(init?.method).toBe('POST')
    expect(init?.headers).toEqual({ [CALLER_API_KEY_HEADER]: 'test-api-key' })
    expect(init?.body).toBeInstanceOf(FormData)
  })

  it('ignores legacy X-DocLens-Credential localStorage key', async () => {
    localStorage.setItem(LEGACY_CALLER_CREDENTIAL_STORAGE_KEY, 'legacy-api-key')
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('{}', { status: 200 }))

    await uploadBatch(uploadOptions())

    const [, init] = fetchSpy.mock.calls[0] ?? []
    expect(init?.headers).toEqual({})
  })

  it('ignores X-DocLens-Credential-Key when it only exists in localStorage', async () => {
    localStorage.setItem(CALLER_CREDENTIAL_STORAGE_KEY, 'persistent-api-key')
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('{}', { status: 200 }))

    await uploadBatch(uploadOptions())

    const [, init] = fetchSpy.mock.calls[0] ?? []
    expect(init?.headers).toEqual({})
  })
})
