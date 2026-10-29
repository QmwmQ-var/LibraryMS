-- H2 测试种子数据：不显式指定自增主键，保证后续插入不会与种子主键冲突。
INSERT INTO `user` (`username`, `password`, `name`, `role`, `title`, `email`, `phone`, `avatar_color`, `status`, `failed_attempts`)
VALUES
('admin', '$2a$10$3o8LqGThWSOgtw7FJkErcuyzBT0IgnuxoR2s7rs7p4NJDoeTxQa3i', '顾清和', 'ADMIN', '流通服务部 · 馆员', 'admin@library.edu.cn', '13800000001', '#2e7364', 'ACTIVE', 0),
('2023001', '$2a$10$XYGGhZ3fWdvH13eEApY9k.8PGyOlR7xWnlIYFklosxvYTO2kqPvdS', '林知夏', 'READER', '本科生', 'lin.zhixia@campus.edu.cn', '13800000002', '#8f5c42', 'ACTIVE', 0),
('2023002', '$2a$10$XYGGhZ3fWdvH13eEApY9k.8PGyOlR7xWnlIYFklosxvYTO2kqPvdS', '陈屿', 'READER', '研究生', 'chen.yu@campus.edu.cn', '13800000003', '#4e6c82', 'ACTIVE', 0);

INSERT INTO `reader` (`user_id`, `student_no`, `name`, `gender`, `type`, `card_status`, `department`, `major`, `email`, `phone`, `card_no`, `card_issued_at`, `card_expires_at`, `deleted`)
VALUES
(2, '2023001', '林知夏', '女', 'UNDERGRADUATE', 'ACTIVE', '计算机与电子信息学院', '软件工程 2301', 'lin.zhixia@campus.edu.cn', '13800000002', 'LIB2023001', '2025-09-01', '2029-09-01', 0),
(3, '2023002', '陈屿', '男', 'GRADUATE', 'ACTIVE', '计算机与电子信息学院', '软件工程 2024 级硕士', 'chen.yu@campus.edu.cn', '13800000003', 'LIB2023002', '2025-09-01', '2029-09-01', 0);

INSERT INTO `book_category` (`name`, `code`, `color`, `description`, `deleted`)
VALUES
('文学', 'LIT', '#8f5c42', '中外文学经典与当代小说', 0),
('计算机', 'TP', '#2e7364', '计算机科学与软件工程', 0);

INSERT INTO `book` (`isbn`, `title`, `author`, `publisher`, `category_id`, `total_copies`, `available_copies`, `publish_year`, `price`, `location`, `description`, `rating`, `reservation_count`, `deleted`)
VALUES
('9787536692930', '三体', '刘慈欣', '重庆出版社', 1, 3, 3, 2008, 52.00, 'A-05-01', '中国科幻里程碑之作。', 4.3, 0, 0),
('9787506365437', '活着', '余华', '作家出版社', 1, 2, 2, 2012, 45.00, 'A-02-01', '讲述生命的韧性与苦难。', 4.3, 0, 0),
('9787111407010', '算法导论', '托马斯·科尔曼', '机械工业出版社', 2, 1, 0, 2013, 128.00, 'TP-01-01', '算法领域权威教材。', 4.3, 0, 0),
('9787544274665', '雪国', '川端康成', '南海出版公司', 1, 1, 0, 2022, 49.00, 'A-06-02', '日本文学经典。', 4.6, 0, 0);

INSERT INTO `borrow_record` (`reader_id`, `book_id`, `borrow_date`, `due_date`, `return_date`, `status`, `renew_count`, `operator_id`, `returned_by`, `reader_name`, `reader_no`, `book_title`, `book_isbn`, `created_at`)
VALUES
(1, 2, '2026-09-12', '2026-10-12', '2026-10-14', 'RETURNED', 0, 1, 1, '林知夏', '2023001', '活着', '9787506365437', '2026-09-12 09:00:00'),
(1, 3, '2026-09-01', '2026-10-01', NULL, 'OVERDUE', 0, 1, NULL, '林知夏', '2023001', '算法导论', '9787111407010', '2026-09-01 09:00:00'),
(2, 4, '2026-10-01', '2026-10-31', NULL, 'BORROWED', 0, 1, NULL, '陈屿', '2023002', '雪国', '9787544274665', '2026-10-01 09:00:00');

INSERT INTO `penalty` (`record_id`, `reader_id`, `book_id`, `overdue_days`, `amount`, `status`, `paid_at`, `paid_by`, `payment_channel`, `rule_rate`, `created_at`)
VALUES
(1, 1, 2, 2, 1.00, 'UNPAID', NULL, NULL, NULL, 0.50, '2026-10-14 17:00:00');

INSERT INTO `announcement` (`title`, `content`, `tag`, `created_at`)
VALUES
('图书馆秋季开放时间调整', '自本周起，主馆开放时间调整为 08:00–22:30。', '通知', '2026-10-27 09:00:00');
