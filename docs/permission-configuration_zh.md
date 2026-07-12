# 权限配置

DocLens-j 的 `/api/v1/**` 权限不是 Spring Security、登录会话或 JWT，
而是通过 Spring MVC 拦截器链完成。本文说明生产环境和开发环境应该如何
配置。

## 权限模型

所有 `/api/v1/**` 请求在进入控制器之前都会经过以下检查：

1. `GlobalProtectionInterceptor` 限制全局并发在途请求数。
2. `TrustedGatewayInterceptor` 在开启时校验请求是否来自可信网关。
3. `CallerPartitionInterceptor` 要求 `X-Doclens-Key`，并把它转换为调用方分区。
4. `CallerTrafficInterceptor` 按分区执行流量限流。

最关键的一点是：`X-Doclens-Key` 只是数据分区键，不是认证令牌。生产
环境的身份认证来自可信网关头，并通过 `doclens.gateway-auth.enabled=true`
开启。

## 请求头

| 请求头 | 何时必需 | 说明 |
| --- | --- | --- |
| `X-Doclens-Gateway-Secret` | 开启生产网关认证时 | 可信网关共享证明，值必须和 `doclens.gateway-auth.trusted-gateway.header-value` 一致。 |
| `X-Doclens-Principal` | 开启生产网关认证时 | 网关注入的 principal 名称，必须命中 `doclens.gateway-auth.principals[].principal`。 |
| `X-Doclens-Roles` | 可选 | 网关注入的角色上下文。当前 admin 校验基于配置中的 principal 角色，而不是只信任这个请求头。 |
| `X-Doclens-Key` | 所有 `/api/v1/**` 请求 | 只用于数据隔离和限流的调用方分区键。 |

HTTP 请求头大小写不敏感，但示例统一使用上面的名字，便于和实现对齐。

## 生产环境配置

生产环境应该在边缘网关完成外部用户或系统认证后，再把身份转发到
DocLens-j。不要直接把 Spring Boot 服务暴露给公网而不经过可信网关。

`doclens-server/src/main/resources/application-prod.yml` 中已经开启了网关认证：

```yaml
doclens:
  gateway-auth:
    enabled: true
    trusted-gateway:
      header-value: ${DOCLENS_GATEWAY_SECRET}
    principals:
      - principal: ${DOCLENS_GATEWAY_PRINCIPAL}
        roles:
          - admin
        allowed-partitions:
          - ${DOCLENS_GATEWAY_PARTITION}
```

单个生产 dashboard principal 的环境变量示例：

```bash
export DOCLENS_GATEWAY_SECRET='replace-with-long-random-secret'
export DOCLENS_GATEWAY_PRINCIPAL='dashboard-admin'
export DOCLENS_GATEWAY_PARTITION='tenant-a'
```

网关转发请求示例：

```bash
curl -sS http://localhost:10003/api/v1/dashboard/summary \
  -H 'X-Doclens-Gateway-Secret: replace-with-long-random-secret' \
  -H 'X-Doclens-Principal: dashboard-admin' \
  -H 'X-Doclens-Roles: admin' \
  -H 'X-Doclens-Key: tenant-a'
```

只有同时满足以下条件，请求才会被接受：

- `X-Doclens-Gateway-Secret` 和 `DOCLENS_GATEWAY_SECRET` 一致。
- `X-Doclens-Principal` 是 `dashboard-admin`。
- `X-Doclens-Key` 属于 `dashboard-admin` 允许访问的分区。
- 目标路由不需要 `admin`，或者该 principal 的配置角色包含 `admin`。

### 多分区生产配置

如果不同网关 principal 需要访问不同分区，可以配置多个 principal：

```yaml
doclens:
  gateway-auth:
    enabled: true
    trusted-gateway:
      header-value: ${DOCLENS_GATEWAY_SECRET}
    principals:
      - principal: dashboard-admin
        roles:
          - admin
        allowed-partitions:
          - tenant-a
          - tenant-b
      - principal: tenant-a-viewer
        roles:
          - user
        allowed-partitions:
          - tenant-a
```

在这个例子里，`dashboard-admin` 可以使用 `tenant-a` 或 `tenant-b`，
而 `tenant-a-viewer` 只能访问 `tenant-a`。admin 路由仍然要求 principal 具备
`admin` 角色。

### roles 和 allowed-partitions 到底是什么意思

可以把网关授权配置拆成三个问题来看：

| 字段 | 含义 | 控制什么 |
| --- | --- | --- |
| `principal` | 可信网关告诉服务端“谁在访问”。 | 使用哪一个已配置身份做授权判断。 |
| `roles` | 这个身份有什么操作级别。 | 能不能访问 admin 路由。 |
| `allowed-partitions` | 这个身份允许请求哪些数据分区。 | 哪些 `X-Doclens-Key` 会被接受。 |

例如：

```yaml
principals:
  - principal: company-a-user
    roles:
      - user
    allowed-partitions:
      - company-a

  - principal: platform-admin
    roles:
      - admin
    allowed-partitions:
      - company-a
      - company-b
```

这段配置表示：

- `company-a-user` 只能使用 `X-Doclens-Key: company-a`。
- `platform-admin` 可以使用 `X-Doclens-Key: company-a` 或
  `X-Doclens-Key: company-b`。
- `platform-admin` 可以访问 admin 路由，因为它配置了 `admin` 角色。
- `company-a-user` 不能访问 admin 路由，因为 `user` 不满足 admin 路由策略。

下面这个请求对 `platform-admin` 是允许的：

```http
X-Doclens-Principal: platform-admin
X-Doclens-Key: company-a
```

这个请求也允许：

```http
X-Doclens-Principal: platform-admin
X-Doclens-Key: company-b
```

但这个请求会返回 `403 Forbidden`，因为 `company-c` 不在
`platform-admin` 的 `allowed-partitions` 中：

```http
X-Doclens-Principal: platform-admin
X-Doclens-Key: company-c
```

一句话理解：

```text
principal = 谁在访问
roles = 这个身份能不能做管理操作
allowed-partitions = 这个身份能访问哪些隔离数据空间
```

`roles` 不决定能读哪个租户或分区的数据；数据访问范围由
`allowed-partitions` 和请求里的 `X-Doclens-Key` 决定。反过来，
`allowed-partitions` 也不会自动授予 admin 操作权限。

### admin 路由

默认的 admin 路由包括：

- `/api/v1/ocr-governance-config/**`
- `/api/v1/llm-markdown-config/**`
- `/api/v1/ocr-models/**`
- `/api/v1/ocr-nodes/**`
- `/api/v1/dashboard/callback-jobs/*/retry`

访问这些路径的 principal 配置里必须包含 `admin`。

## 开发环境配置

本地默认配置里 `doclens.gateway-auth.enabled=false`。开发者不需要提供网关
secret 或 principal 请求头，但访问 `/api/v1/**` 仍然必须带 `X-Doclens-Key`。

最小本地请求示例：

```bash
curl -sS http://localhost:10003/api/v1/dashboard/summary \
  -H 'X-Doclens-Key: local-dev'
```

本地上传示例：

```bash
curl -sS http://localhost:10003/api/v1/batches \
  -H 'X-Doclens-Key: local-dev' \
  -F 'files=@./sample.pdf'
```

使用不同的本地分区键可以测试隔离效果：

```bash
curl -sS http://localhost:10003/api/v1/dashboard/summary \
  -H 'X-Doclens-Key: tenant-a'

curl -sS http://localhost:10003/api/v1/dashboard/summary \
  -H 'X-Doclens-Key: tenant-b'
```

`tenant-a` 创建的数据不会出现在 `tenant-b` 下。

## 本地启用网关认证

如果想在本地验证生产模式，可以临时开启网关认证：

```yaml
doclens:
  gateway-auth:
    enabled: true
    trusted-gateway:
      header-value: gateway-secret-local
    principals:
      - principal: local-admin
        roles:
          - admin
        allowed-partitions:
          - local-dev
```

然后使用全部网关头访问接口：

```bash
curl -sS http://localhost:10003/api/v1/ocr-nodes \
  -H 'X-Doclens-Gateway-Secret: gateway-secret-local' \
  -H 'X-Doclens-Principal: local-admin' \
  -H 'X-Doclens-Roles: admin' \
  -H 'X-Doclens-Key: local-dev'
```

这个模式适合提前验证网关联动、admin 路由和分区 allowlist。

## 流量限制

调用方流量限制配置在 `doclens.traffic` 下。默认 `application.yml` 会开启
流量限制和全局保护：

```yaml
doclens:
  traffic:
    enabled: true
    anonymous-enabled: false
    default-limits:
      dashboard-read:
        qps: 10
        burst: 20
      detail-read:
        qps: 5
        burst: 10
      upload-write:
        qps: 0.5
        burst: 2
      ocr-mutation:
        qps: 0.2
        burst: 1
      config-mutation:
        qps: 0.1
        burst: 1
      admin-health:
        qps: 2
        burst: 5
    global-protection:
      enabled: true
      max-in-flight: 100
```

每个限流桶都按 `X-Doclens-Key` 加接口组计算。一个分区耗尽了
`upload-write`，不会影响另一个分区的同类额度。

测试环境会在 `doclens-server/src/test/resources/application.yml` 中关闭流量
限制，避免应用测试依赖令牌桶时序。

## 失败响应

| 状态码 | 常见原因 |
| --- | --- |
| `401 Unauthorized` | `X-Doclens-Key` 缺失或无效，可信网关证明缺失，或者开启网关认证后 principal 缺失。 |
| `403 Forbidden` | principal 未配置到请求分区，或者路由需要 `admin` 但该 principal 没有这个角色。 |
| `429 Too Many Requests` | 调用方分区超过了流量限制。响应头会带 `Retry-After` 和 `X-DocLens-Traffic-Group`。 |
| `503 Service Unavailable` | 全局并发保护拒绝了请求。 |

## 注意事项

- 当前服务没有做用户登录、JWT 校验或 Spring Security 授权。
- `X-Doclens-Key` 负责数据隔离，但它不是密钥，不能单独作为生产认证证明。
- 旧的 API key / bearer token 兼容解析现在不参与当前的认证或 allowlist 判断。
- 配置里默认的 `/api/v1/** -> user` 路由策略存在，但当前拦截器只强制
  `admin` 这类策略。

## 相关文档

- [配置说明](configuration.md)
- [HTTP API Reference](api.md)
- [本地开发](development.md)
