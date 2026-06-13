import test from 'node:test'
import assert from 'node:assert/strict'

import { renderMarkdownPreviewHtml } from '../markdownPreviewRules.ts'

test('markdown preview renders headings paragraphs and strong text', () => {
  const html = renderMarkdownPreviewHtml('## 标题\n\n正文 **重点**')

  assert.match(html, /<h2>标题<\/h2>/)
  assert.match(html, /<p>正文 <strong>重点<\/strong><\/p>/)
})

test('markdown preview escapes script content before rendering html', () => {
  const html = renderMarkdownPreviewHtml('正文 <script>alert(1)</script>')

  assert.equal(html.includes('<script>'), false)
  assert.match(html, /&lt;script&gt;alert\(1\)&lt;\/script&gt;/)
})

test('markdown preview renders safe links and blocks javascript links', () => {
  const html = renderMarkdownPreviewHtml('[官网](https://example.com)\n\n[坏链接](javascript:alert(1))')

  assert.match(html, /<a href="https:\/\/example.com" target="_blank" rel="noopener noreferrer">官网<\/a>/)
  assert.match(html, /坏链接/)
  assert.equal(html.includes('javascript:alert'), false)
})
