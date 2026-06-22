import { afterEach, describe, expect, it, vi } from 'vitest'

import { fetchDashboardSummary } from '@/api/dashboard'

const CALLER_CREDENTIAL_STORAGE_KEY = 'X-DocLens-Credential-Key'
const LEGACY_CALLER_CREDENTIAL_STORAGE_KEY = 'X-DocLens-Credential'
const CALLER_PARTITION_HEADER = 'X-DocLens-Credential-Key'
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

  it('attaches raw X-DocLens-Credential-Key when sessionStorage provides caller key', async () => {
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

  it('omits caller credential headers when sessionStorage does not provide X-DocLens-Credential-Key', async () => {
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

  it('ignores X-DocLens-Credential-Key when it only exists in localStorage', async () => {
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
})
