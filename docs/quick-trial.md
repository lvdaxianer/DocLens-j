# 5 分钟跑通 DocLens-j

这条路径用于快速验证 DocLens-j 的产品闭环：启动服务、上传文件、查看批次详情，
并下载可进入下游流程的解析结果。

## 适用前提

- 已安装 Java 21、Maven、Node.js 和 npm。
- 已安装 Docker 或 Docker Compose，用于本地 PostgreSQL。
- 本地 OCR 服务或默认 PaddleOCR endpoint 可用。
- 如需处理 Word 文件，已安装 LibreOffice。

## 1. 启动本地服务

在仓库根目录执行：

```bash
./scripts/dev-up.sh
./scripts/dev-status.sh
```

开发脚本会同时启动本地 PostgreSQL、后端和 Dashboard。DocLens-j 的本地、
测试和生产数据库基线均为 PostgreSQL。

服务默认地址：

| 服务 | 地址 |
| --- | --- |
| Dashboard | `http://127.0.0.1:10002/dashboard/` |
| 后端 API | `http://127.0.0.1:10003` |
| 健康检查 | `http://127.0.0.1:10003/api/v1/health` |

如果只想验证后端接口，可以参考 `docs/development.md` 的后端单独启动方式。

## 2. 打开上传页

浏览器打开：

```text
http://127.0.0.1:10002/dashboard/#/upload
```

选择一个 PDF、图片、TIFF、Markdown 或 TXT 文件。为了最快完成试用，建议先用 1 到 3 页的小文件。

## 3. 创建批次

点击上传页的提交按钮后，Dashboard 会创建 OCR 批次并自动跳转到批次详情。

批次详情页重点检查：

- 顶部批次状态和进度。
- 接入信息中的 metadata、callback URL 和 idempotency key。
- OCR 路由命中节点。
- 文档处理轨道中的单个文档状态。

如果文档失败，先查看文档行的失败原因，再根据提示重试或调整 OCR 节点配置。

## 4. 打开解析结果

文档完成后，在文档处理轨道中点击“查看文本”。结果抽屉会展示：

- Markdown 内容或 OCR 纯文本。
- OCR 原内容。
- 结果 ID、保存路径、页数、文本块和置信度。
- LLM Markdown 后处理状态。

## 5. 下载结果

结果抽屉提供三种下载动作：

| 下载动作 | 内容 | 适用场景 |
| --- | --- | --- |
| 下载 Markdown | 最终 `finalText` | 进入知识库、RAG 或人工阅读 |
| 下载 TXT | OCR 原内容，缺失时回退最终文本 | 需要原始 OCR 文本比对 |
| 下载 JSON | 当前加载的结果响应 | 调试、集成验证或自动化处理 |

下载文件名包含源文档名和结果 ID，方便和批次详情回溯。

## 6. 清理本地服务

试用结束后执行：

```bash
./scripts/dev-down.sh
```

## 常见问题

| 现象 | 处理方式 |
| --- | --- |
| 上传后一直处理中 | 检查 OCR 节点健康和 `var/dev/backend.log` |
| Word 文件处理失败 | 检查 LibreOffice 命令路径 |
| 下载按钮不可用 | 确认文档结果已加载，且结果抽屉不是错误或加载中状态 |
| 回调失败 | 在批次详情的回调投递区域查看失败原因并手动重试 |
