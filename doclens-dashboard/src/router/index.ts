import { createRouter, createWebHashHistory } from 'vue-router'

import BatchDetailView from '@/views/BatchDetailView.vue'
import BatchesView from '@/views/BatchesView.vue'
import OcrHealthView from '@/views/OcrHealthView.vue'
import OverviewView from '@/views/OverviewView.vue'
import UploadView from '@/views/UploadView.vue'

export const router = createRouter({
  history: createWebHashHistory('/dashboard/'),
  routes: [
    {
      path: '/',
      name: 'overview',
      component: OverviewView,
      meta: { title: '运行总览' }
    },
    {
      path: '/upload',
      name: 'upload',
      component: UploadView,
      meta: { title: '上传文件' }
    },
    {
      path: '/batches',
      name: 'batches',
      component: BatchesView,
      meta: { title: '批次列表' }
    },
    {
      path: '/batches/:batchId',
      name: 'batch-detail',
      component: BatchDetailView,
      meta: { title: '批次详情' }
    },
    {
      path: '/ocr-health',
      name: 'ocr-health',
      component: OcrHealthView,
      meta: { title: 'OCR 健康' }
    }
  ]
})
