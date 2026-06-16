## Why

用户上传批次时填写了 `callback_url`，OCR 解析完成后仪表盘仍显示“当前批次没有回调任务”。现有代码只在完成事件的 `result_summary.callback_body` 中构造回调 payload，没有把该事件转换成持久化 `ocr_callback_jobs` 记录，导致投递 worker 没有任务可执行。

## What Changes

在文档完成结果持久化时，如果所属批次存在非空 `callback_url`，为 `document.completed` 事件创建一条 `CallbackJob`，保存回调地址、事件 ID、批次 ID、文档 ID 和回调 payload。继续复用现有 callback worker、失败原因记录、自动重试和页面手动重试逻辑。

## Impact

影响 `doclens-core` 的批处理完成持久化链路和自动装配依赖。不会改变上传接口、Dashboard 回调展示字段、回调失败原因契约或手动重试接口。
