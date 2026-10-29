-- H2（MySQL 模式）测试建表脚本：仅用于不依赖本机 MySQL 的集成测试。
DROP TABLE IF EXISTS `reservation`;
DROP TABLE IF EXISTS `penalty`;
DROP TABLE IF EXISTS `borrow_record`;
DROP TABLE IF EXISTS `login_log`;
DROP TABLE IF EXISTS `announcement`;
DROP TABLE IF EXISTS `book`;
DROP TABLE IF EXISTS `book_category`;
DROP TABLE IF EXISTS `reader`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username`        VARCHAR(50)  NOT NULL,
    `password`        VARCHAR(100) NOT NULL,
    `name`            VARCHAR(50)  NOT NULL,
    `role`            VARCHAR(20)  NOT NULL,
    `title`           VARCHAR(50),
    `email`           VARCHAR(100),
    `phone`           VARCHAR(20),
    `avatar_color`    VARCHAR(20),
    `status`          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    `failed_attempts` INT          NOT NULL DEFAULT 0,
    `locked_until`    DATETIME,
    `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `uk_test_user_username` UNIQUE (`username`)
);

CREATE TABLE `reader` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`         BIGINT       NOT NULL,
    `student_no`      VARCHAR(20)  NOT NULL,
    `name`            VARCHAR(50)  NOT NULL,
    `gender`          VARCHAR(10)  NOT NULL DEFAULT '未设置',
    `type`            VARCHAR(20)  NOT NULL,
    `card_status`     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    `department`      VARCHAR(100),
    `major`           VARCHAR(100),
    `email`           VARCHAR(100),
    `phone`           VARCHAR(20),
    `card_no`         VARCHAR(30)  NOT NULL,
    `card_issued_at`  DATE,
    `card_expires_at` DATE,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `uk_test_reader_student_no` UNIQUE (`student_no`)
);

CREATE TABLE `book_category` (
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name`        VARCHAR(50)  NOT NULL,
    `code`        VARCHAR(20)  NOT NULL,
    `color`       VARCHAR(20)  NOT NULL DEFAULT '#2e7364',
    `description` VARCHAR(255),
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `book` (
    `id`                BIGINT AUTO_INCREMENT PRIMARY KEY,
    `isbn`              VARCHAR(20)   NOT NULL,
    `title`             VARCHAR(200)  NOT NULL,
    `author`            VARCHAR(100)  NOT NULL,
    `publisher`         VARCHAR(100),
    `category_id`       BIGINT        NOT NULL,
    `total_copies`      INT           NOT NULL DEFAULT 1,
    `available_copies`  INT           NOT NULL DEFAULT 1,
    `publish_year`      INT,
    `price`             DECIMAL(10, 2) DEFAULT 0.00,
    `location`          VARCHAR(50),
    `description`       VARCHAR(500),
    `rating`            DECIMAL(2, 1) NOT NULL DEFAULT 4.5,
    `reservation_count` INT           NOT NULL DEFAULT 0,
    `deleted`           TINYINT       NOT NULL DEFAULT 0,
    `created_at`        DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME      DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `uk_test_book_isbn` UNIQUE (`isbn`)
);

CREATE TABLE `borrow_record` (
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
    `reader_id`   BIGINT      NOT NULL,
    `book_id`     BIGINT      NOT NULL,
    `borrow_date` DATE        NOT NULL,
    `due_date`    DATE        NOT NULL,
    `return_date` DATE,
    `status`      VARCHAR(20) NOT NULL DEFAULT 'BORROWED',
    `renew_count` INT         NOT NULL DEFAULT 0,
    `operator_id` BIGINT,
    `returned_by` BIGINT,
    `reader_name` VARCHAR(50),
    `reader_no`   VARCHAR(20),
    `book_title`  VARCHAR(200),
    `book_isbn`   VARCHAR(20),
    `created_at`  DATETIME    DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `reservation` (
    `id`               BIGINT AUTO_INCREMENT PRIMARY KEY,
    `reader_id`        BIGINT      NOT NULL,
    `book_id`          BIGINT      NOT NULL,
    `status`           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `requested_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `ready_at`         DATETIME,
    `expire_at`        DATETIME,
    `closed_at`        DATETIME,
    `borrow_record_id` BIGINT,
    `cancel_reason`    VARCHAR(100),
    `reader_name`      VARCHAR(50),
    `reader_no`        VARCHAR(20),
    `book_title`       VARCHAR(200),
    `book_isbn`        VARCHAR(20),
    `created_at`       DATETIME    DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `penalty` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
    `record_id`       BIGINT        NOT NULL,
    `reader_id`       BIGINT        NOT NULL,
    `book_id`         BIGINT        NOT NULL,
    `overdue_days`    INT           NOT NULL,
    `amount`          DECIMAL(10, 2) NOT NULL,
    `status`          VARCHAR(20)   NOT NULL DEFAULT 'UNPAID',
    `paid_at`         DATETIME,
    `paid_by`         BIGINT,
    `payment_channel` VARCHAR(20),
    `rule_rate`       DECIMAL(10, 2) NOT NULL DEFAULT 0.50,
    `created_at`      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `uk_test_penalty_record` UNIQUE (`record_id`)
);

CREATE TABLE `login_log` (
    `id`         BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`    BIGINT,
    `username`   VARCHAR(50) NOT NULL,
    `role`       VARCHAR(20),
    `ip`         VARCHAR(45),
    `success`    TINYINT     NOT NULL DEFAULT 0,
    `created_at` DATETIME    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `announcement` (
    `id`         BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title`      VARCHAR(100) NOT NULL,
    `content`    VARCHAR(500) NOT NULL,
    `tag`        VARCHAR(20),
    `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP
);
