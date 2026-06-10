# DocLens Dashboard

DocLens 控制台前端模块，使用 Vue 3、TypeScript、Vite、Naive UI、Pinia 和 ECharts。

## 开发

```bash
npm install
npm run dev
```

如果希望同时启动前后端并让后端代码变更后自动重启，优先在仓库根目录执行：

```bash
./scripts/dev-up.sh
./scripts/dev-status.sh
./scripts/dev-down.sh
```

开发服务默认运行在 `http://127.0.0.1:10002/dashboard/`，并代理 `/api` 到 `http://127.0.0.1:10003`。

## 构建

```bash
npm run build
```

构建产物会输出到 `../doclens-server/src/main/resources/static/dashboard`，随 Spring Boot 服务通过 `/dashboard/` 访问。
