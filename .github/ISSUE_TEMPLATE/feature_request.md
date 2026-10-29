---
name: 功能需求 / 任务
about: 拆分一个可独立验收的功能点，指明归属模块与验收标准
title: "[Feat] "
labels: enhancement
assignees: ''
---

## 所属模块

- [ ] A · 平台与认证
- [ ] B · 馆藏
- [ ] C · 读者
- [ ] D · 流通与罚款
- [ ] E · 数据与交付

## 需求描述

<!-- 谁（角色）在什么场景下要做什么，为什么需要 -->

## 验收标准

1.
2.

## 涉及改动

- 后端：Controller / Service / Mapper / 表结构
- 前端：页面 / `api/*.js` / Mock handler
- 文档：是否需要同步 `docs/接口清单.md`、`docs/数据库设计.md`

## 契约影响

- [ ] 不新增/不改动接口
- [ ] 新增或修改接口，契约草案如下：

```json
// 请求 / 响应示例
```

## 拆分与依赖

- 依赖的模块或 Issue：
- 预计分支：`feature/xxx`
