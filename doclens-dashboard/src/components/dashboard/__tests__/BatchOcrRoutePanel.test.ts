import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'

import BatchOcrRoutePanel from '../BatchOcrRoutePanel.vue'

describe('BatchOcrRoutePanel', () => {
  it('shows current file final allocation and batch dispatch hits at the same time', () => {
    const wrapper = mount(BatchOcrRoutePanel, {
      props: {
        routePolicy: {
          routing_mode: 'MODEL_LOAD_BALANCE',
          model_key: 'paddle_ocr',
          node_id: '',
          load_balance_strategy: 'least-inflight'
        },
        currentDocumentName: 'invoice-a.pdf',
        currentDocumentRunningHitNodes: [],
        currentDocumentFinalHitNodes: [
          { model_key: 'paddle_ocr', node_id: 'node-1', node_name: '节点一', image_count: 2 }
        ],
        hitNodes: [
          { model_key: 'paddle_ocr', node_id: 'node-1', node_name: '节点一', image_count: 2 },
          { model_key: 'paddle_ocr', node_id: 'node-2', node_name: '节点二', image_count: 1 }
        ]
      }
    })

    expect(wrapper.text()).toContain('当前文件实际分配')
    expect(wrapper.text()).toContain('invoice-a.pdf')
    expect(wrapper.text()).toContain('批次 OCR 调度命中')
    expect(wrapper.text()).toContain('包含重试、故障转移和运行中请求')
  })

  it('shows explicit empty state when current document has not completed ocr yet', () => {
    const wrapper = mount(BatchOcrRoutePanel, {
      props: {
        routePolicy: {
          routing_mode: 'GLOBAL_LOAD_BALANCE',
          model_key: 'paddle_ocr',
          node_id: '',
          load_balance_strategy: 'least-inflight'
        },
        currentDocumentName: 'invoice-b.pdf',
        currentDocumentRunningHitNodes: [
          { model_key: 'paddle_ocr', node_id: 'node-3', node_name: '节点三', image_count: 2 }
        ],
        currentDocumentFinalHitNodes: [],
        hitNodes: [
          { model_key: 'paddle_ocr', node_id: 'node-3', node_name: '节点三', image_count: 3 }
        ]
      }
    })

    expect(wrapper.text()).toContain('当前文件实际分配')
    expect(wrapper.text()).toContain('运行中分配')
    expect(wrapper.text()).toContain('节点三')
    expect(wrapper.text()).toContain('当前文件尚未完成 OCR，暂无最终分配结果')
    expect(wrapper.text()).toContain('批次 OCR 调度命中')
  })

  it('shows running and final allocation as separate sections', () => {
    const wrapper = mount(BatchOcrRoutePanel, {
      props: {
        routePolicy: {
          routing_mode: 'MODEL_LOAD_BALANCE',
          model_key: 'paddle_ocr',
          node_id: '',
          load_balance_strategy: 'least-inflight'
        },
        currentDocumentName: 'running.pdf',
        currentDocumentRunningHitNodes: [
          { model_key: 'paddle_ocr', node_id: 'node-running', node_name: '运行节点', image_count: 1 }
        ],
        currentDocumentFinalHitNodes: [
          { model_key: 'paddle_ocr', node_id: 'node-final', node_name: '最终节点', image_count: 2 }
        ],
        hitNodes: []
      }
    })

    expect(wrapper.text()).toContain('运行中分配')
    expect(wrapper.text()).toContain('最终分配结果')
    expect(wrapper.text()).toContain('运行节点')
    expect(wrapper.text()).toContain('最终节点')
  })

  it('shows OCR model names separately from provider model details', () => {
    const wrapper = mount(BatchOcrRoutePanel, {
      props: {
        routePolicy: {
          routing_mode: 'GLOBAL_LOAD_BALANCE',
          model_key: '',
          node_id: '',
          load_balance_strategy: 'least-inflight'
        },
        currentDocumentName: 'manual.pdf',
        currentDocumentRunningHitNodes: [],
        currentDocumentFinalHitNodes: [
          { model_key: 'ollama', model_name: 'Ollama', node_id: 'node-ollama', node_name: 'Ollama 节点', image_count: 1 }
        ],
        hitNodes: [
          { model_key: 'ollama', model_name: 'Ollama', node_id: 'node-ollama', node_name: 'Ollama 节点', image_count: 2 }
        ]
      }
    })

    expect(wrapper.text()).toContain('OCR 模型：Ollama')
    expect(wrapper.text()).not.toContain('OCR 模型：deepseek-ocr')
  })

  it('shows live OCR page tasks with worker threads', () => {
    const wrapper = mount(BatchOcrRoutePanel, {
      props: {
        routePolicy: {
          routing_mode: 'GLOBAL_LOAD_BALANCE',
          model_key: 'paddle_ocr',
          node_id: '',
          load_balance_strategy: 'least-inflight'
        },
        currentDocumentName: 'parallel.pdf',
        currentDocumentRunningHitNodes: [],
        currentDocumentFinalHitNodes: [],
        runningPageTasks: [
          {
            task_id: 'task-1',
            document_id: 'doc-running',
            page_no: 1,
            worker_id: 'worker-a',
            thread_name: 'doclens-page-task-ocr-1',
            started_at: '2026-06-21T10:15:30+08:00',
            running_ms: 1200,
            model_key: 'paddle_ocr',
            node_id: 'node-1',
            node_name: '节点一'
          },
          {
            task_id: 'task-2',
            document_id: 'doc-running',
            page_no: 2,
            worker_id: 'worker-a',
            thread_name: 'doclens-page-task-ocr-2',
            started_at: '2026-06-21T10:15:31+08:00',
            running_ms: 900,
            model_key: 'paddle_ocr',
            node_id: 'node-1',
            node_name: '节点一'
          }
        ],
        hitNodes: []
      }
    })

    expect(wrapper.text()).toContain('实时 OCR 图片任务')
    expect(wrapper.text()).toContain('第 1 页')
    expect(wrapper.text()).toContain('第 2 页')
    expect(wrapper.text()).toContain('doclens-page-task-ocr-1')
    expect(wrapper.text()).toContain('doclens-page-task-ocr-2')
    expect(wrapper.text()).toContain('worker-a')
  })
})
