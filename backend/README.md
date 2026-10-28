# LibraryMS · Spring Boot 后端

高校图书管理系统后端，严格按前端 Mock 契约实现：统一 `/api` 前缀、统一 `{code,message,data}` 响应、分页字段一致。
完整功能与业务规则见根目录 [README](../README.md) 和 [核心业务说明](../docs/核心业务说明.md)。

## 技术栈

- Java 17（本机以 JDK 21 验证）
- Spring Boot 3.5.6
- Spring Security + JWT（jjwt 0.12）
- MyBatis-Plus 3.5.14
- MySQL 8.0+（InnoDB / utf8mb4），测试环境使用 H2 MySQL 模式
- Maven Wrapper（`mvnw` / `mvnw.cmd`）

## 目录

```text
src/main/java/com/university/library/
├── common/       # 响应、异常、JWT、Security、配置、常量、工具
├── controller/   # 8 个薄控制器
├── service/      # 认证、图书、读者、借阅、罚款、统计、仪表盘
│   └── support/  # ViewService：Entity → VO 的只读组装
├── mapper/       # MyBatis-Plus Mapper 与少量聚合查询
├── entity/       # 数据库实体
├── dto/          # 入参校验
├── vo/           # 出参视图
└── LibraryManagementApplication.java
```

## 数据库初始化

```bash
mysql -u root -p < ../database/schema.sql
mysql -u root -p < ../database/data.sql
```

默认数据库 `libraryms`，连接信息用环境变量覆盖：

| 变量 | 默认 | 说明 |
| --- | --- | --- |
| `DB_HOST` | `localhost` | |
| `DB_PORT` | `3306` | |
| `DB_NAME` | `libraryms` | |
| `DB_USERNAME` | `root` | |
| `DB_PASSWORD` | 空 | 建议通过环境变量传入 |
| `JWT_SECRET` | 开发默认值 | 生产必须替换，≥32 字节 |
| `PENALTY_RATE` | `0.5` | 每日罚款元 |

种子账号：`admin/admin123`、`2023001/reader123`（其余读者密码相同）。

## 启动

```bash
# Windows
.\mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

已安装 Maven 时也可直接用 `mvn spring-boot:run`。默认端口 `8080`。

## 构建与测试

```bash
# 运行 25 个测试（单元 + H2 接口集成 + 容器冒烟）
.\mvnw.cmd test

# 打包可执行 jar
.\mvnw.cmd -DskipTests package
java -jar target/libraryms-1.0.0.jar
```

> 测试在 `src/test/resources/application-test.yml` 使用独立 H2 内存库和测试种子 SQL，不依赖本机 MySQL。
> Windows 沙箱下 Mockito 使用 `mock-maker-subclass`（见 `src/test/resources/mockito-extensions/`），
> 避免 inline mock maker 自 attach 被拒绝；在普通开发机上两种模式均可运行。

## 接口与权限

- 公开：`POST /api/auth/login`；其余接口需要 `Authorization: Bearer <JWT>`。
- 管理员：图书/分类/读者管理、全量借阅、罚款确认、统计。
- 读者：检索、借书、本人借阅/罚款、自助归还/续借/缴纳。
- 所有资源归属校验在 Service 层二次完成，前端菜单显隐不是安全边界。

完整路径、参数与返回字段见 [接口清单](../docs/接口清单.md)。

## 配置说明

- `app.jwt.secret`、`app.jwt.expire-minutes`、`app.jwt.issuer`
- `app.login.max-fail-attempts`、`app.login.lock-minutes`
- `app.penalty.rate-per-day`
- `app.cors.allowed-origins`
- MyBatis-Plus 自动过滤逻辑删除；`reader/book/book_category` 保留历史记录。

接口约定：HTTP 状态统一 200，业务结果由响应体 `code` 表达（200/400/401/403/404/409/423/500）。
