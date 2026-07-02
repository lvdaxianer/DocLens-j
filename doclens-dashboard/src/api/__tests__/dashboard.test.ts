import { afterEach, describe, expect, it, vi } from 'vitest'

import { fetchDashboardSummary } from '@/api/dashboard'

const CALLER_CREDENTIAL_STORAGE_KEY = 'X-Doclens-Key'
const OLD_CALLER_CREDENTIAL_STORAGE_KEY = 'X-Recall-Key'
const DOC_LENS_CALLER_CREDENTIAL_STORAGE_KEY = 'X-DocLens-Credential-Key'
const LEGACY_CALLER_CREDENTIAL_STORAGE_KEY = 'X-DocLens-Credential'
const CALLER_PARTITION_HEADER = 'X-Doclens-Key'
const OLD_CALLER_PARTITION_HEADER = 'X-Recall-Key'
const DOC_LENS_CALLER_PARTITION_HEADER = 'X-DocLens-Credential-Key'
const BEARER_PREFIX = 'Bearer '
const JSON_CONTENT_TYPE = 'application/json'

/**
 * 为 Dashboard 请求准备一个空响应。
 *
 * @returns fetch 响应模拟对象
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
function mockDashboardResponse(): Response {
  return new Response('{}', {
    status: 200,
    headers: { 'content-type': JSON_CONTENT_TYPE }
  })
}

describe('dashboard caller credential headers', () => {
  afterEach(() => {
    localStorage.clear()
    sessionStorage.clear()
    vi.restoreAllMocks()
  })

  it('attaches raw X-Doclens-Key when sessionStorage provides caller key', async () => {
    sessionStorage.setItem(CALLER_CREDENTIAL_STORAGE_KEY, 'test-api-key')
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(mockDashboardResponse())

    await fetchDashboardSummary()

    expect(fetchSpy).toHaveBeenCalledWith('/api/v1/dashboard/summary', {
      method: 'GET',
      headers: {
        Accept: JSON_CONTENT_TYPE,
        [CALLER_PARTITION_HEADER]: 'test-api-key'
      }
    })
  })

  it('omits caller credential headers when sessionStorage does not provide X-Doclens-Key', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(mockDashboardResponse())

    await fetchDashboardSummary()

    expect(fetchSpy).toHaveBeenCalledWith('/api/v1/dashboard/summary', {
      method: 'GET',
      headers: {
        Accept: JSON_CONTENT_TYPE
      }
    })
  })

  it('does not translate Bearer-like caller key to Authorization', async () => {
    sessionStorage.setItem(CALLER_CREDENTIAL_STORAGE_KEY, `${BEARER_PREFIX}test-bearer-token`)
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(mockDashboardResponse())

    await fetchDashboardSummary()

    expect(fetchSpy).toHaveBeenCalledWith('/api/v1/dashboard/summary', {
      method: 'GET',
      headers: {
        Accept: JSON_CONTENT_TYPE,
        [CALLER_PARTITION_HEADER]: `${BEARER_PREFIX}test-bearer-token`
      }
    })
  })

  it('ignores legacy X-DocLens-Credential localStorage key', async () => {
    localStorage.setItem(LEGACY_CALLER_CREDENTIAL_STORAGE_KEY, 'legacy-api-key')
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(mockDashboardResponse())

    await fetchDashboardSummary()

    expect(fetchSpy).toHaveBeenCalledWith('/api/v1/dashboard/summary', {
      method: 'GET',
      headers: {
        Accept: JSON_CONTENT_TYPE
      }
    })
  })

  it('ignores X-Doclens-Key when it only exists in localStorage', async () => {
    localStorage.setItem(CALLER_CREDENTIAL_STORAGE_KEY, 'persistent-api-key')
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(mockDashboardResponse())

    await fetchDashboardSummary()

    expect(fetchSpy).toHaveBeenCalledWith('/api/v1/dashboard/summary', {
      method: 'GET',
      headers: {
        Accept: JSON_CONTENT_TYPE
      }
    })
  })

  it('ignores legacy X-DocLens-Credential-Key in sessionStorage', async () => {
    sessionStorage.setItem(DOC_LENS_CALLER_CREDENTIAL_STORAGE_KEY, 'legacy-partition-key')
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(mockDashboardResponse())

    await fetchDashboardSummary()

    expect(fetchSpy).toHaveBeenCalledWith('/api/v1/dashboard/summary', {
      method: 'GET',
      headers: {
        Accept: JSON_CONTENT_TYPE
      }
    })
  })

  it('ignores old X-Recall-Key when it exists in sessionStorage', async () => {
    sessionStorage.setItem(OLD_CALLER_CREDENTIAL_STORAGE_KEY, 'recall-partition-key')
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(mockDashboardResponse())

    await fetchDashboardSummary()

    expect(fetchSpy).toHaveBeenCalledWith('/api/v1/dashboard/summary', {
      method: 'GET',
      headers: {
        Accept: JSON_CONTENT_TYPE
      }
    })
  })

  it('does not emit legacy headers when X-Doclens-Key exists', async () => {
    sessionStorage.setItem(CALLER_CREDENTIAL_STORAGE_KEY, 'doclens-partition-key')
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(mockDashboardResponse())

    await fetchDashboardSummary()

    const [, init] = fetchSpy.mock.calls[0] ?? []
    expect(init?.headers).not.toHaveProperty(DOC_LENS_CALLER_PARTITION_HEADER)
    expect(init?.headers).not.toHaveProperty(OLD_CALLER_PARTITION_HEADER)
  })
})

describe('dashboard api error parser', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('shows stable missing partition message from backend code and detail', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(
      JSON.stringify({ code: 'MISSING_PARTITION', detail: '缺少 X-Doclens-Key' }),
      { status: 401, statusText: 'Unauthorized', headers: { 'content-type': JSON_CONTENT_TYPE } }
    ))

    await expect(fetchDashboardSummary()).rejects.toThrow('缺少分区键：缺少 X-Doclens-Key')
  })
})
