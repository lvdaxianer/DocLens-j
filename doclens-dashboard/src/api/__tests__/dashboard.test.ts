import { afterEach, describe, expect, it, vi } from 'vitest'

import { fetchDashboardSummary } from '@/api/dashboard'

const CALLER_CREDENTIAL_STORAGE_KEY = 'X-DocLens-Credential-Key'
const LEGACY_CALLER_CREDENTIAL_STORAGE_KEY = 'X-DocLens-Credential'
const CALLER_API_KEY_HEADER = 'X-DocLens-Api-Key'
const AUTHORIZATION_HEADER = 'Authorization'
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
    vi.restoreAllMocks()
  })

  it('attaches X-DocLens-Api-Key when localStorage provides X-DocLens-Credential-Key', async () => {
    localStorage.setItem(CALLER_CREDENTIAL_STORAGE_KEY, 'test-api-key')
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(mockDashboardResponse())

    await fetchDashboardSummary()

    expect(fetchSpy).toHaveBeenCalledWith('/api/v1/dashboard/summary', {
      method: 'GET',
      headers: {
        Accept: JSON_CONTENT_TYPE,
        [CALLER_API_KEY_HEADER]: 'test-api-key'
      }
    })
  })

  it('omits caller credential headers when localStorage does not provide X-DocLens-Credential-Key', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(mockDashboardResponse())

    await fetchDashboardSummary()

    expect(fetchSpy).toHaveBeenCalledWith('/api/v1/dashboard/summary', {
      method: 'GET',
      headers: {
        Accept: JSON_CONTENT_TYPE
      }
    })
  })

  it('attaches Authorization when localStorage provides a Bearer token', async () => {
    localStorage.setItem(CALLER_CREDENTIAL_STORAGE_KEY, `${BEARER_PREFIX}test-bearer-token`)
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(mockDashboardResponse())

    await fetchDashboardSummary()

    expect(fetchSpy).toHaveBeenCalledWith('/api/v1/dashboard/summary', {
      method: 'GET',
      headers: {
        Accept: JSON_CONTENT_TYPE,
        [AUTHORIZATION_HEADER]: `${BEARER_PREFIX}test-bearer-token`
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
})
