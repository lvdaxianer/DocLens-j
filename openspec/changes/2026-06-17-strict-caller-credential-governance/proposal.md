## Why

DocLens-j 现在仍然允许无 caller 凭证访问部分接口，导致同一个部署里
不同接入方看到的是同一份批次、文档和运营数据。

这和产品预期不一致。Stage 2 不是个人登录系统，而是请求级 caller
凭证治理：每次请求都要携带可验证的 caller 凭证，并且只能看到
属于该凭证的数据。

## What Changes

收紧 caller 凭证边界：

- 取消匿名 caller 回退，所有需要访问 DocLens-j 的接口都要求已配置的
  caller 凭证。
- 上传、Dashboard 查询、文档操作和 LLM Markdown 配置接口都使用同一
  套请求凭证解析逻辑。
- Dashboard 读模型、批次详情、文档结果、回调重试和删除类接口都按
  caller 过滤，只返回所属凭证的数据。
- Dashboard 前端从本地环境读取 caller 凭证并随每个请求发送。

## Impact

这会改变上传、查询、重试、删除和 Dashboard 请求的默认行为。
未配置 caller 凭证或发送无效凭证时，请求会被拒绝，不再有匿名访问。
