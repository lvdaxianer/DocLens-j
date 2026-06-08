import type {
  BatchDetailResponse,
  BatchListResponse,
  DashboardSummary,
  OcrHealthResponse
} from '@/types/dashboard'

async function requestJson<T>(path: string): Promise<T> {
  const response = await fetch(path, {
    headers: {
      Accept: 'application/json'
    }
  })
  if (!response.ok) {
    throw new Error(`请求失败：${response.status} ${response.statusText}`)
  }
  return response.json() as Promise<T>
}

export function fetchDashboardSummary(): Promise<DashboardSummary> {
  return requestJson<DashboardSummary>('/api/v1/dashboard/summary')
}

export function fetchBatchList(): Promise<BatchListResponse> {
  return requestJson<BatchListResponse>('/api/v1/dashboard/batches')
}

export function fetchBatchDetail(batchId: string): Promise<BatchDetailResponse> {
  return requestJson<BatchDetailResponse>(`/api/v1/dashboard/batches/${encodeURIComponent(batchId)}`)
}

export function fetchOcrHealth(): Promise<OcrHealthResponse> {
  return requestJson<OcrHealthResponse>('/api/v1/dashboard/ocr-health')
}
