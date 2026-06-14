## Why

DocLens 仪表盘当前的主强调色仍然偏蓝，和用户期望的橙色品牌视觉不一致。

## What Changes

将前端仪表盘的全局主题色统一切换为橙色系，覆盖 Naive UI 的 primary 颜色、全局 CSS 的 active/焦点变量、侧边栏选中态、图表首色以及少量写死的蓝色强调样式。

## Impact

仅影响 `doclens-dashboard` 前端展示层，不改后端接口和数据结构。
