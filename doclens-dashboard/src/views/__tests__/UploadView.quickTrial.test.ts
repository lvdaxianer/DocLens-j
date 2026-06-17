import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const UPLOAD_VIEW_PATH = resolve(__dirname, '../UploadView.vue')

/**
 * 上传页快速试用入口测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
describe('UploadView quick trial entry', () => {
  /**
   * 上传页应保留一个不阻塞上传流程的五分钟试用指南链接。
   *
   * @author lvdaxianerplus
   * @date 2026-06-17
   */
  it('links to the five-minute trial guide', () => {
    const source = readFileSync(UPLOAD_VIEW_PATH, 'utf8')

    expect(source).toContain('5 分钟跑通')
    expect(source).toContain('/docs/quick-trial.md')
  })
})
