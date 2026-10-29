# 参与开发 · CONTRIBUTING

本文是给项目成员的最短上手路径。**模块边界、分支归属、合并检查的完整说明在 [`docs/团队分工与协作.md`](docs/团队分工与协作.md)，遇到冲突以那份文档为准。**

## 1. 环境准备

| 依赖 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17 或以上 | 后端编译目标为 Java 17 |
| Node.js | 18 或以上（推荐 20） | 前端 Vite 6 要求 |
| MySQL | 8.0 或以上 | 只有联调真实后端时需要，日常开发用 Mock 即可 |
| Git | 2.30+ | 需要支持 `git switch` |

首次开发**不需要装数据库**：前端默认走内置 Mock API，`npm run dev` 就能跑通全部页面。

## 2. 获取代码

```bash
git clone git@github.com:QmwmQ-var/LibraryMS.git
cd LibraryMS
git switch feature/<你的模块>     # 见第 3 节的分支归属
```

若 SSH 连接 GitHub 不稳定（`Connection reset`），按 [`docs/远程协作操作手册.md`](docs/远程协作操作手册.md) 第 6 节的排查表处理；只用 HTTPS 克隆同样可行。

## 3. 我该在哪个分支、改哪些文件

| 成员 | 分支 | 后端 | 前端 |
| --- | --- | --- | --- |
| A 平台与认证 | `feature/platform-auth` | `common/**`、`AuthController`、`AuthService`、`User/LoginLog/Announcement` | `layouts/`、`router/`、`stores/`、`assets/styles/`、`views/login`、`views/error`、`api/http.js`、`mock/index.js` |
| B 馆藏 | `feature/catalog` | `BookController`、`CategoryController`、`BookService`、`CategoryService`、`Book/BookCategory` | `views/book`、`views/category`、`components/book`、`api/book.js` |
| C 读者 | `feature/readers` | `ReaderController`、`ReaderService`、`Reader` | `views/reader`、`views/profile`、`api/reader.js` |
| D 流通与罚款 | `feature/circulation` | `BorrowRecordController`、`PenaltyController`、`ReservationController`、`BorrowService`、`PenaltyService`、`ReservationService`、`OverdueService` | `views/borrow`、`views/penalty`、`views/reservation`、`api/borrow.js`、`api/penalty.js`、`api/reservation.js` |
| E 数据与交付 | `feature/analytics` | `StatsController`、`DashboardController`、`StatsService`、`DashboardService` | `views/dashboard`、`views/stats`、`api/stats.js`、`mock/analytics.js`；另负责 `database/`、`docs/` |

精确到文件的归属在 [`.github/CODEOWNERS`](.github/CODEOWNERS)；开 PR 后系统会按它自动请求对应负责人评审。

**越界规则**：需要改别人目录时，先在群里发契约 diff，由该目录负责人提提交，或明确约定后在同一 PR 里注明"已获 XX 同意"。

## 4. 本地运行

```bash
# 前端（Mock 模式，默认）
cd frontend
npm install
npm run dev            # http://localhost:5173

# 前端（连接真实后端）
npm run build:backend  # 读取 .env.backend，VITE_USE_MOCK=false

# 后端
cd backend
./mvnw spring-boot:run           # Windows: mvnw.cmd spring-boot:run
# 需先建库并执行 database/schema.sql、database/data.sql，见 README「快速开始」
```

演示账号见 [`README.md`](README.md#快速开始)。

## 5. 提交规范

采用 Conventional Commits，一个提交只做一件事：

```
feat(book): 增加 ISBN 唯一性校验
fix(borrow): 修复并发还书导致库存多增
docs(api): 补充罚款接口说明
test(reader): 覆盖借阅证挂失场景
```

提交信息里的 scope 用模块名（`book`、`reader`、`borrow`、`penalty`、`stats`、`auth`、`repo`、`docs`）。

## 6. PR 五步流程

1. `git switch feature/<你的模块> && git pull origin feature/<你的模块>`，确保基于最新代码；
2. 开发并自测：`cd backend && ./mvnw test`、`cd frontend && npm run build`；
3. `git push origin feature/<你的模块>`；
4. 在 GitHub 上开 PR，**base 选 `develop`**，按模板填写自测结果、契约影响、截图；
5. 等 CI 全绿 + 至少一位其他成员 Approve，由当周集成人合并。

`develop` → `main` 只在每周五验收主链路（登录 → 借书 → 还书 → 罚款 → 统计）通过后进行，并打 tag。

## 7. 禁止项

- 不提交 `node_modules/`、`target/`、`dist/`、`.env.local`、日志、IDE 个人配置；
- 不把真实数据库口令、JWT 密钥写进代码或配置文件——用环境变量 `DB_PASSWORD`、`JWT_SECRET` 覆盖；
- 不直接在 `main`、`develop` 上提交（分支保护会拒绝）；
- 不在 Controller 里写业务逻辑，不跨模块直接改对方的表。

## 8. 遇到问题

- 环境/启动问题：先看 [`docs/远程协作操作手册.md`](docs/远程协作操作手册.md) 第 6 节排查表；
- 业务规则疑问：[`docs/核心业务说明.md`](docs/核心业务说明.md)；
- 接口契约：[`docs/接口清单.md`](docs/接口清单.md)；
- 仍然卡住：在群里发"分支 + 报错原文 + 已尝试的操作"，不要只发"跑不起来"。
