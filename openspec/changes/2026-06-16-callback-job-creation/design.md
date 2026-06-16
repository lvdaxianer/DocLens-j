## Architecture

`BatchProcessingUseCase` 已经在同一个事务里持久化文档状态、OCR 结果和完成事件。修复点放在这个事务内：保存事件后，筛选 `document.completed` 事件，并在批次带有 `callback_url` 时创建对应的 `CallbackJob`。

## Data Flow

1. 文档完成后，`DocumentProcessingEventBuilder` 继续生成包含 `callback_body` 的 `document.completed` 事件。
2. `BatchProcessingUseCase.persistDocumentProcessing` 保存事件和结果。
3. 新增的 callback job 创建逻辑读取完成事件的 `resultSummary.callback_body`，结合批次 `callback_url` 创建 `CallbackJob`。
4. 现有 `CallbackDeliveryWorker` 扫描 PENDING/RETRYING job 并执行 HTTP POST，失败时保留 `failure_reason` 和 `failure_detail`。

## Error Handling

如果批次没有 `callback_url`，不创建 callback job。若完成事件没有稳定的 `callback_body`，抛出明确异常阻止静默丢失回调任务，因为这是内部事件契约破坏。

## Testing

先新增一个 core 层用例，验证带 `callback_url` 的批次在文档完成后保存一条 callback job，并且 payload 来自 `callback_body`。该测试先 RED，再实现最小 GREEN。
