# DocLens Dashboard

DocLens 控制台前端模块，使用 Vue 3、TypeScript、Vite、Naive UI、Pinia 和 ECharts。

## 开发

```bash
npm install
npm run dev
```

开发服务默认代理 `/api` 到 `http://127.0.0.1:8080`。

## 构建

```bash
npm run build
```

构建产物会输出到 `../doclens-server/src/main/resources/static/dashboard`，随 Spring Boot 服务通过 `/dashboard/` 访问。
