-- ============================================================================
-- v1.1 预约功能增量迁移
-- 适用：已经执行过旧版 schema.sql / data.sql，不想重建数据库的环境。
-- 全新安装无需执行本文件，直接使用最新 schema.sql 与 data.sql。
-- ============================================================================

USE `libraryms`;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `reservation` (
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

-- 旧库可以继续使用原有 32 本书；如需演示预约数据，建议重新导入最新 data.sql，
-- 或手工挑选一本“无可借副本”的图书创建预约记录。
