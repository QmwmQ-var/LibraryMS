# 参与开发 · CONTRIBUTING

本文是给项目成员的最短上手路径。**模块边界、分支归属、合并检查的完整说明在 [`docs/团队分工与协作.md`](docs/团队分工与协作.md)，遇到冲突以那份文档为准。**

**先知道自己能做哪些事**：

- 🔒 **只有仓库管理员（A）能做**：邀请成员、分支保护、Actions 设置、替换 `CODEOWNERS`、推送 `main`、打 tag、删除远程分支；
- 🔁 **当周集成人**：合并 PR 到 `develop`、周五发起 `develop → main`；
- ✍️ **你（模块负责人）**：改自己目录、推自己的 `feature/*`、开 PR、评审别人的 PR；
- 权限矩阵与文档分工见 [`docs/团队分工与协作.md` 第 8 节](docs/团队分工与协作.md#8-角色与操作权限)，网页端设置步骤见 [`docs/远程协作操作手册.md`](docs/远程协作操作手册.md) 第 1 节。

## 1. 环境准备

| 依赖 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17（推荐 Temurin 17） | 后端编译目标为 Java 17；不要用 24/25 直接编译 |
| Node.js | ≥ 20.19（建议 22 LTS） | 前端 Vite 6 要求 |
| MySQL | 8.0 或以上 | 只有联调真实后端时需要，日常开发用 Mock 即可 |
| Git | 2.30+ | 需要支持 `git switch` |

首次开发**不需要装数据库**：前端默认走内置 Mock API，`npm run dev` 就能跑通全部页面。

用 IDEA 打开仓库根目录后，还要做两步（`.idea`/`*.iml` 不入库，每人本地各配一次）：

1. 右键 `backend/pom.xml` → **Add as Maven Project**（或 Maven 面板 Reload）；
2. `File → Project Structure → Project`，SDK 选 JDK 17、Language level 选 17。

> 运行配置里出现“模块 'libraryms' 没有 JDK”或主类飘红，基本都是这两步没做，见 [`docs/远程协作操作手册.md`](docs/远程协作操作手册.md) 第 6 节。

## 2. 获取代码

```bash
git clone git@github.com:QmwmQ-var/LibraryMS.git
cd LibraryMS
git switch feature/<你的模块>     # 见第 3 节的分支归属
git pull origin feature/<你的模块>
```

仓库现有 7 个分支：`main`（稳定版）、`develop`（每日集成）、5 个 `feature/*`。**日常只在自己的 `feature/*` 上提交**，不要直接推 `main`、`develop`。

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

**旧库升级**：如果数据库是早期版本（没有 `reservation` 表），启动后接口会报
`Table 'libraryms.reservation' doesn't exist`，执行一次增量迁移即可，不必重建库：

```bash
mysql -u root -p < database/migrations/20261029_add_reservation.sql
```

新增表结构时，提交 `database/migrations/<日期>_<说明>.sql`，并在 PR 里勾选“表结构已同步”。

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
4. 在 GitHub 上开 PR，**常规功能一律 base 选 `develop`**，按模板填写自测结果、契约影响、截图；
5. 等 CI 全绿 + 至少一位其他成员 Approve，由当周集成人合并。

例外只有两种：**版本发布**和**已上线版本的紧急修复**，可以直接对 `main` 开 PR，但必须由 🔒 仓库管理员合并，并在合并后把 `main` 反向合并回 `develop`，避免集成分支落后。

`develop` → `main` 只在每周五验收主链路（登录 → 借书 → 还书 → 罚款 → 统计）通过后进行，并打 tag。

## 7. 禁止项

- 不提交 `node_modules/`、`target/`、`dist/`、`.env.local`、日志、IDE 个人配置；
- 不把真实数据库口令、JWT 密钥写进代码或配置文件——用环境变量 `DB_PASSWORD`、`JWT_SECRET` 覆盖；
- 不直接在 `main`、`develop` 上提交（分支保护会拒绝）；
- 不在 Controller 里写业务逻辑，不跨模块直接改对方的表。

## 8. 遇到问题

| 现象 | 先看这里 |
| --- | --- |
| 启动报 `Table 'libraryms.reservation' doesn't exist` | 数据库是旧版本，跑第 4 节的增量迁移 |
| IDEA 报“模块 'libraryms' 没有 JDK”、主类飘红 | 按第 1 节导入 Maven + 设 SDK 17 |
| 页面 500 或数据异常 | 服务端日志 + `docs/接口清单.md` 对照字段 |
| 业务规则不确定 | [`docs/核心业务说明.md`](docs/核心业务说明.md) |
| CI 红 | 本地复现：`cd backend && ./mvnw test`、`cd frontend && npm ci && npm run build` |
| 仍然卡住 | 群里发“分支 + 报错原文 + 已尝试的操作”，不要只发“跑不起来” |

更完整的排查表见 [`docs/远程协作操作手册.md`](docs/远程协作操作手册.md) 第 6 节。

---

> **维护人**：A。
> **更新时机**：环境要求、上手步骤、分支或 PR 流程变化时；与 `docs/团队分工与协作.md` 冲突时以后者为准。
> **权限与文档分工**：[团队分工与协作.md 第 8 节](docs/团队分工与协作.md#8-角色与操作权限)。
