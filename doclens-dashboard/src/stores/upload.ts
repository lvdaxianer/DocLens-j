import { reactive, shallowRef } from 'vue'
import { defineStore } from 'pinia'

import { uploadBatch } from '@/api/upload'
import type { UploadBatchOptions, UploadBatchResponse } from '@/types/upload'

interface UploadState {
  loading: boolean
  error: string
}

function emptyUploadState(): UploadState {
  return {
    loading: false,
    error: ''
  }
}

export const useUploadStore = defineStore('upload', () => {
  const latestBatch = shallowRef<UploadBatchResponse | null>(null)
  const uploadState = reactive(emptyUploadState())

  async function submitBatch(options: UploadBatchOptions): Promise<UploadBatchResponse> {
    uploadState.loading = true
    uploadState.error = ''
    try {
      const result = await uploadBatch(options)
      latestBatch.value = result
      return result
    } catch (error) {
      uploadState.error = error instanceof Error ? error.message : '上传失败'
      throw error
    } finally {
      uploadState.loading = false
    }
  }

  function clearLatestBatch(): void {
    latestBatch.value = null
  }

  return {
    latestBatch,
    uploadState,
    submitBatch,
    clearLatestBatch
  }
})
