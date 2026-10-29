-- ============================================================================
-- 高校图书管理系统 (LibraryMS) 数据库建表脚本
-- 数据库：MySQL 8.0+ / InnoDB / utf8mb4
-- 说明：
--   1. reader / book / book_category 使用 deleted 字段实现逻辑删除，
--      以便删除读者、图书、分类后仍能保留历史借阅与罚款数据；
--      业务层查询会自动过滤 deleted = 1 的记录（MyBatis-Plus @TableLogic）。
--   2. borrow_record 冗余保存了读者姓名/学号、图书书名/ISBN 快照，
--      即使读者或图书之后被删除，历史记录仍可完整展示。
--   3. 借阅证状态：ACTIVE / LOST / CANCELLED；
--      借阅状态：BORROWED / RETURNED / OVERDUE；
--      罚款状态：UNPAID / PAID。
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `libraryms`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE `libraryms`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `reservation`;
DROP TABLE IF EXISTS `penalty`;
DROP TABLE IF EXISTS `borrow_record`;
DROP TABLE IF EXISTS `login_log`;
DROP TABLE IF EXISTS `announcement`;
DROP TABLE IF EXISTS `book`;
DROP TABLE IF EXISTS `book_category`;
DROP TABLE IF EXISTS `reader`;
DROP TABLE IF EXISTS `user`;

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------------------------------------------------------
-- 用户表：管理员与读者共用，role 区分角色
-- 登录失败计数与锁定截止时间直接落库，不依赖 Redis
-- ----------------------------------------------------------------------------
CREATE TABLE `user` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`        VARCHAR(50)  NOT NULL COMMENT '登录账号（管理员账号或读者学号）',
    `password`        VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希',
    `name`            VARCHAR(50)  NOT NULL COMMENT '姓名',
    `role`            VARCHAR(20)  NOT NULL DEFAULT 'READER' COMMENT '角色：ADMIN / READER',
    `title`           VARCHAR(50)  DEFAULT NULL COMMENT '职务/读者类型标签',
    `email`           VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone`           VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `avatar_color`    VARCHAR(20)  DEFAULT '#2e7364' COMMENT '头像主题色',
    `status`          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '账号状态：ACTIVE / DISABLED',
    `failed_attempts` INT          NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
    `locked_until`    DATETIME     DEFAULT NULL COMMENT '锁定截止时间（失败 3 次锁定 15 分钟）',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`),
    KEY `idx_user_role` (`role`),
    KEY `idx_user_status` (`status`),
    CONSTRAINT `chk_user_role` CHECK (`role` IN ('ADMIN', 'READER')),
    CONSTRAINT `chk_user_status` CHECK (`status` IN ('ACTIVE', 'DISABLED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表';

-- ----------------------------------------------------------------------------
-- 读者表：与 user 一对一（user_id 唯一），保存学号、读者类型、借阅证状态
-- ----------------------------------------------------------------------------
CREATE TABLE `reader` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         BIGINT       NOT NULL COMMENT '关联登录用户',
    `student_no`      VARCHAR(20)  NOT NULL COMMENT '学号（同时作为登录账号）',
    `name`            VARCHAR(50)  NOT NULL COMMENT '姓名',
    `gender`          VARCHAR(10)  NOT NULL DEFAULT '未设置' COMMENT '性别',
    `type`            VARCHAR(20)  NOT NULL COMMENT '读者类型：UNDERGRADUATE / GRADUATE / TEACHER',
    `card_status`     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '借阅证状态：ACTIVE / LOST / CANCELLED',
    `department`      VARCHAR(100) DEFAULT NULL COMMENT '院系',
    `major`           VARCHAR(100) DEFAULT NULL COMMENT '专业',
    `email`           VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone`           VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `card_no`         VARCHAR(30)  NOT NULL COMMENT '借阅证号',
    `card_issued_at`  DATE         DEFAULT NULL COMMENT '发证日期',
    `card_expires_at` DATE         DEFAULT NULL COMMENT '证件有效期',
    `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reader_user_id` (`user_id`),
    UNIQUE KEY `uk_reader_student_no` (`student_no`),
    UNIQUE KEY `uk_reader_card_no` (`card_no`),
    KEY `idx_reader_type` (`type`),
    KEY `idx_reader_card_status` (`card_status`),
    KEY `idx_reader_deleted` (`deleted`),
    CONSTRAINT `fk_reader_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `chk_reader_type` CHECK (`type` IN ('UNDERGRADUATE', 'GRADUATE', 'TEACHER')),
    CONSTRAINT `chk_reader_card_status` CHECK (`card_status` IN ('ACTIVE', 'LOST', 'CANCELLED')),
    CONSTRAINT `chk_reader_deleted` CHECK (`deleted` IN (0, 1))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '读者表';

-- ----------------------------------------------------------------------------
-- 图书分类表
-- ----------------------------------------------------------------------------
CREATE TABLE `book_category` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(50)  NOT NULL COMMENT '分类名称',
    `code`        VARCHAR(20)  NOT NULL COMMENT '分类代码（中图法简码）',
    `color`       VARCHAR(20)  NOT NULL DEFAULT '#2e7364' COMMENT '展示颜色',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '分类说明',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_name` (`name`),
    UNIQUE KEY `uk_category_code` (`code`),
    KEY `idx_category_deleted` (`deleted`),
    CONSTRAINT `chk_category_deleted` CHECK (`deleted` IN (0, 1))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '图书分类表';

-- ----------------------------------------------------------------------------
-- 图书表：ISBN 唯一，馆藏总量与可借数量
-- ----------------------------------------------------------------------------
CREATE TABLE `book` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `isbn`              VARCHAR(20)   NOT NULL COMMENT 'ISBN（唯一）',
    `title`             VARCHAR(200)  NOT NULL COMMENT '书名',
    `author`            VARCHAR(100)  NOT NULL COMMENT '作者',
    `publisher`         VARCHAR(100)  DEFAULT NULL COMMENT '出版社',
    `category_id`       BIGINT        NOT NULL COMMENT '分类',
    `total_copies`      INT           NOT NULL DEFAULT 1 COMMENT '馆藏总量',
    `available_copies`  INT           NOT NULL DEFAULT 1 COMMENT '可借数量',
    `publish_year`      INT           DEFAULT NULL COMMENT '出版年份',
    `price`             DECIMAL(10, 2) DEFAULT 0.00 COMMENT '定价',
    `location`          VARCHAR(50)   DEFAULT NULL COMMENT '馆藏位置',
    `description`       VARCHAR(500)  DEFAULT NULL COMMENT '简介',
    `rating`            DECIMAL(2, 1) NOT NULL DEFAULT 4.5 COMMENT '推荐评分',
    `reservation_count` INT           NOT NULL DEFAULT 0 COMMENT '预留预约数（>0 时禁止续借）',
    `deleted`           TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    `created_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_book_isbn` (`isbn`),
    KEY `idx_book_category` (`category_id`),
    KEY `idx_book_title` (`title`),
    KEY `idx_book_author` (`author`),
    KEY `idx_book_available` (`available_copies`),
    KEY `idx_book_deleted` (`deleted`),
    CONSTRAINT `fk_book_category` FOREIGN KEY (`category_id`) REFERENCES `book_category` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `chk_book_copies` CHECK (`total_copies` >= 1 AND `available_copies` >= 0 AND `available_copies` <= `total_copies`),
    CONSTRAINT `chk_book_deleted` CHECK (`deleted` IN (0, 1))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '图书表';

-- ----------------------------------------------------------------------------
-- 借阅记录表：显式维护 BORROWED / RETURNED / OVERDUE 状态
-- 冗余保存读者与图书快照，便于历史展示
-- ----------------------------------------------------------------------------
CREATE TABLE `borrow_record` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `reader_id`   BIGINT       NOT NULL COMMENT '读者',
    `book_id`     BIGINT       NOT NULL COMMENT '图书',
    `borrow_date` DATE         NOT NULL COMMENT '借出日期',
    `due_date`    DATE         NOT NULL COMMENT '应还日期',
    `return_date` DATE         DEFAULT NULL COMMENT '实际归还日期',
    `status`      VARCHAR(20)  NOT NULL DEFAULT 'BORROWED' COMMENT '状态：BORROWED / RETURNED / OVERDUE',
    `renew_count` INT          NOT NULL DEFAULT 0 COMMENT '续借次数（最多 1 次）',
    `operator_id` BIGINT       DEFAULT NULL COMMENT '经办管理员',
    `returned_by` BIGINT       DEFAULT NULL COMMENT '归还经办人',
    `reader_name` VARCHAR(50)  DEFAULT NULL COMMENT '读者姓名快照',
    `reader_no`   VARCHAR(20)  DEFAULT NULL COMMENT '读者学号快照',
    `book_title`  VARCHAR(200) DEFAULT NULL COMMENT '书名快照',
    `book_isbn`   VARCHAR(20)  DEFAULT NULL COMMENT 'ISBN 快照',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_borrow_reader` (`reader_id`),
    KEY `idx_borrow_book` (`book_id`),
    KEY `idx_borrow_status` (`status`),
    KEY `idx_borrow_borrow_date` (`borrow_date`),
    KEY `idx_borrow_due_date` (`due_date`),
    KEY `idx_borrow_reader_status` (`reader_id`, `status`),
    KEY `idx_borrow_book_status` (`book_id`, `status`),
    CONSTRAINT `fk_borrow_reader` FOREIGN KEY (`reader_id`) REFERENCES `reader` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_borrow_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_borrow_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `fk_borrow_returned_by` FOREIGN KEY (`returned_by`) REFERENCES `user` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `chk_borrow_status` CHECK (`status` IN ('BORROWED', 'RETURNED', 'OVERDUE')),
    CONSTRAINT `chk_borrow_renew` CHECK (`renew_count` >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '借阅记录表';

-- ----------------------------------------------------------------------------
-- 预约表：读者对无可借副本的图书排队；到书后保留 48 小时
-- 状态：PENDING 排队中 / READY 已到书待领取 / FULFILLED 已借出
--       CANCELLED 已取消 / EXPIRED 已过期
-- 说明：book.reservation_count 缓存 PENDING + READY 的数量，用于续借拦截；
--       READY 的副本从 available_copies 中扣除（被视为“已留书”）。
-- ----------------------------------------------------------------------------
CREATE TABLE `reservation` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `reader_id`        BIGINT       NOT NULL COMMENT '预约读者',
    `book_id`          BIGINT       NOT NULL COMMENT '预约图书',
    `status`           VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / READY / FULFILLED / CANCELLED / EXPIRED',
    `requested_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '预约时间（排队依据）',
    `ready_at`         DATETIME     DEFAULT NULL COMMENT '到书时间',
    `expire_at`        DATETIME     DEFAULT NULL COMMENT '保留截止时间',
    `closed_at`        DATETIME     DEFAULT NULL COMMENT '完成/取消/过期时间',
    `borrow_record_id` BIGINT       DEFAULT NULL COMMENT '履约生成的借阅记录',
    `cancel_reason`    VARCHAR(100) DEFAULT NULL COMMENT '取消原因',
    `reader_name`      VARCHAR(50)  DEFAULT NULL COMMENT '读者姓名快照',
    `reader_no`        VARCHAR(20)  DEFAULT NULL COMMENT '读者学号快照',
    `book_title`       VARCHAR(200) DEFAULT NULL COMMENT '书名快照',
    `book_isbn`        VARCHAR(20)  DEFAULT NULL COMMENT 'ISBN 快照',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_reservation_reader_status` (`reader_id`, `status`, `requested_at`),
    KEY `idx_reservation_book_status` (`book_id`, `status`, `requested_at`),
    KEY `idx_reservation_expire` (`status`, `expire_at`),
    KEY `idx_reservation_borrow_record` (`borrow_record_id`),
    CONSTRAINT `fk_reservation_reader` FOREIGN KEY (`reader_id`) REFERENCES `reader` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_reservation_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_reservation_borrow_record` FOREIGN KEY (`borrow_record_id`) REFERENCES `borrow_record` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `chk_reservation_status` CHECK (`status` IN ('PENDING', 'READY', 'FULFILLED', 'CANCELLED', 'EXPIRED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '图书预约表';

-- ----------------------------------------------------------------------------
-- 罚款表：与借阅记录一对一（record_id 唯一），保证还书幂等不重复生成
-- ----------------------------------------------------------------------------
CREATE TABLE `penalty` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `record_id`        BIGINT        NOT NULL COMMENT '借阅记录',
    `reader_id`        BIGINT        NOT NULL COMMENT '读者',
    `book_id`          BIGINT        NOT NULL COMMENT '图书',
    `overdue_days`     INT           NOT NULL COMMENT '逾期天数',
    `amount`           DECIMAL(10, 2) NOT NULL COMMENT '罚款金额',
    `status`           VARCHAR(20)   NOT NULL DEFAULT 'UNPAID' COMMENT '状态：UNPAID / PAID',
    `paid_at`          DATETIME      DEFAULT NULL COMMENT '缴纳时间',
    `paid_by`          BIGINT        DEFAULT NULL COMMENT '经办用户',
    `payment_channel`  VARCHAR(20)   DEFAULT NULL COMMENT '缴纳渠道：CASH / ONLINE',
    `rule_rate`        DECIMAL(10, 2) NOT NULL DEFAULT 0.50 COMMENT '生成时的每日费率快照',
    `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_penalty_record` (`record_id`),
    KEY `idx_penalty_reader` (`reader_id`),
    KEY `idx_penalty_status` (`status`),
    KEY `idx_penalty_created_at` (`created_at`),
    CONSTRAINT `fk_penalty_record` FOREIGN KEY (`record_id`) REFERENCES `borrow_record` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_penalty_reader` FOREIGN KEY (`reader_id`) REFERENCES `reader` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_penalty_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_penalty_paid_by` FOREIGN KEY (`paid_by`) REFERENCES `user` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `chk_penalty_status` CHECK (`status` IN ('UNPAID', 'PAID')),
    CONSTRAINT `chk_penalty_days` CHECK (`overdue_days` > 0),
    CONSTRAINT `chk_penalty_amount` CHECK (`amount` >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '罚款表';

-- ----------------------------------------------------------------------------
-- 登录日志表
-- ----------------------------------------------------------------------------
CREATE TABLE `login_log` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`    BIGINT       DEFAULT NULL COMMENT '用户（账号不存在时为空）',
    `username`   VARCHAR(50)  NOT NULL COMMENT '登录账号',
    `role`       VARCHAR(20)  DEFAULT NULL COMMENT '登录时角色',
    `ip`         VARCHAR(45)  DEFAULT NULL COMMENT '客户端 IP',
    `success`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否成功：1 成功 / 0 失败',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    PRIMARY KEY (`id`),
    KEY `idx_login_log_username` (`username`),
    KEY `idx_login_log_created_at` (`created_at`),
    KEY `idx_login_log_success` (`success`),
    CONSTRAINT `fk_login_log_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '登录日志表';

-- ----------------------------------------------------------------------------
-- 公告表：仪表盘展示（非核心业务表，便于管理员后续扩展）
-- ----------------------------------------------------------------------------
CREATE TABLE `announcement` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`      VARCHAR(100) NOT NULL COMMENT '标题',
    `content`    VARCHAR(500) NOT NULL COMMENT '内容',
    `tag`        VARCHAR(20)  DEFAULT NULL COMMENT '标签',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    PRIMARY KEY (`id`),
    KEY `idx_announcement_created_at` (`created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '公告表';
