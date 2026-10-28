-- ============================================================================
-- 高校图书管理系统 (LibraryMS) 初始化演示数据
-- 依赖 schema.sql 已执行。
-- 说明：
--   1. 密码均为 BCrypt 哈希（强度 10），可真实登录：
--        admin    / admin123
--        2023001  / reader123（其余读者账号密码相同）
--   2. 借阅/罚款日期使用 CURDATE() 相对偏移，任何时间导入都能得到
--      “近 8 个月借阅趋势、当前在借、逾期”等完整演示数据。
--   3. 可重复执行：先关闭外键检查清空表，再按依赖顺序插入。
-- ============================================================================

USE `libraryms`;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE `penalty`;
TRUNCATE TABLE `borrow_record`;
TRUNCATE TABLE `login_log`;
TRUNCATE TABLE `announcement`;
TRUNCATE TABLE `book`;
TRUNCATE TABLE `book_category`;
TRUNCATE TABLE `reader`;
TRUNCATE TABLE `user`;

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------------------------------------------------------
-- 用户：1 名管理员 + 10 名读者
-- BCrypt("admin123")  /  BCrypt("reader123")
-- ----------------------------------------------------------------------------
INSERT INTO `user` (`id`, `username`, `password`, `name`, `role`, `title`, `email`, `phone`, `avatar_color`, `status`, `failed_attempts`, `locked_until`, `created_at`)
VALUES
(1, 'admin', '$2a$10$3o8LqGThWSOgtw7FJkErcuyzBT0IgnuxoR2s7rs7p4NJDoeTxQa3i', '顾清和', 'ADMIN', '流通服务部 · 馆员', 'admin@library.edu.cn', '13800000001', '#2e7364', 'ACTIVE', 0, NULL, DATE_SUB(NOW(), INTERVAL 500 DAY)),
(2, '2023001', '$2a$10$XYGGhZ3fWdvH13eEApY9k.8PGyOlR7xWnlIYFklosxvYTO2kqPvdS', '林知夏', 'READER', '本科生', 'lin.zhixia@campus.edu.cn', '13800000002', '#8f5c42', 'ACTIVE', 0, NULL, DATE_SUB(NOW(), INTERVAL 410 DAY)),
(3, '2022008', '$2a$10$XYGGhZ3fWdvH13eEApY9k.8PGyOlR7xWnlIYFklosxvYTO2kqPvdS', '陈屿', 'READER', '研究生', 'chen.yu@campus.edu.cn', '13800000003', '#4e6c82', 'ACTIVE', 0, NULL, DATE_SUB(NOW(), INTERVAL 400 DAY)),
(4, '2021012', '$2a$10$XYGGhZ3fWdvH13eEApY9k.8PGyOlR7xWnlIYFklosxvYTO2kqPvdS', '周明远', 'READER', '教师', 'zhou.my@campus.edu.cn', '13800000004', '#6f5d80', 'ACTIVE', 0, NULL, DATE_SUB(NOW(), INTERVAL 390 DAY)),
(5, '2023015', '$2a$10$XYGGhZ3fWdvH13eEApY9k.8PGyOlR7xWnlIYFklosxvYTO2kqPvdS', '苏晚晴', 'READER', '本科生', 'su.wq@campus.edu.cn', '13800000005', '#55745b', 'ACTIVE', 0, NULL, DATE_SUB(NOW(), INTERVAL 380 DAY)),
(6, '2022022', '$2a$10$XYGGhZ3fWdvH13eEApY9k.8PGyOlR7xWnlIYFklosxvYTO2kqPvdS', '顾言', 'READER', '研究生', 'gu.yan@campus.edu.cn', '13800000006', '#a45d51', 'ACTIVE', 0, NULL, DATE_SUB(NOW(), INTERVAL 370 DAY)),
(7, '2023033', '$2a$10$XYGGhZ3fWdvH13eEApY9k.8PGyOlR7xWnlIYFklosxvYTO2kqPvdS', '沈星野', 'READER', '本科生', 'shen.xy@campus.edu.cn', '13800000007', '#8f5c42', 'ACTIVE', 0, NULL, DATE_SUB(NOW(), INTERVAL 360 DAY)),
(8, '2020044', '$2a$10$XYGGhZ3fWdvH13eEApY9k.8PGyOlR7xWnlIYFklosxvYTO2kqPvdS', '叶青', 'READER', '教师', 'ye.qing@campus.edu.cn', '13800000008', '#4e6c82', 'ACTIVE', 0, NULL, DATE_SUB(NOW(), INTERVAL 350 DAY)),
(9, '2023051', '$2a$10$XYGGhZ3fWdvH13eEApY9k.8PGyOlR7xWnlIYFklosxvYTO2kqPvdS', '许一帆', 'READER', '本科生', 'xu.yf@campus.edu.cn', '13800000009', '#6f5d80', 'ACTIVE', 0, NULL, DATE_SUB(NOW(), INTERVAL 340 DAY)),
(10, '2022060', '$2a$10$XYGGhZ3fWdvH13eEApY9k.8PGyOlR7xWnlIYFklosxvYTO2kqPvdS', '陆嘉禾', 'READER', '研究生', 'lu.jh@campus.edu.cn', '13800000010', '#55745b', 'ACTIVE', 0, NULL, DATE_SUB(NOW(), INTERVAL 330 DAY)),
(11, '2023066', '$2a$10$XYGGhZ3fWdvH13eEApY9k.8PGyOlR7xWnlIYFklosxvYTO2kqPvdS', '温以宁', 'READER', '本科生', 'wen.yn@campus.edu.cn', '13800000011', '#a45d51', 'ACTIVE', 0, NULL, DATE_SUB(NOW(), INTERVAL 320 DAY));

-- ----------------------------------------------------------------------------
-- 读者：本科生 5 本/30 天，研究生 10 本/60 天，教师 15 本/90 天
-- ----------------------------------------------------------------------------
INSERT INTO `reader` (`id`, `user_id`, `student_no`, `name`, `gender`, `type`, `card_status`, `department`, `major`, `email`, `phone`, `card_no`, `card_issued_at`, `card_expires_at`, `deleted`, `created_at`)
VALUES
(1, 2, '2023001', '林知夏', '女', 'UNDERGRADUATE', 'ACTIVE', '计算机与电子信息学院', '软件工程 2301', 'lin.zhixia@campus.edu.cn', '13800000002', 'LIB2023001', DATE_SUB(CURDATE(), INTERVAL 410 DAY), DATE_ADD(CURDATE(), INTERVAL 535 DAY), 0, DATE_SUB(NOW(), INTERVAL 410 DAY)),
(2, 3, '2022008', '陈屿', '男', 'GRADUATE', 'ACTIVE', '计算机与电子信息学院', '软件工程 2022 级硕士', 'chen.yu@campus.edu.cn', '13800000003', 'LIB2022008', DATE_SUB(CURDATE(), INTERVAL 400 DAY), DATE_ADD(CURDATE(), INTERVAL 550 DAY), 0, DATE_SUB(NOW(), INTERVAL 400 DAY)),
(3, 4, '2021012', '周明远', '男', 'TEACHER', 'ACTIVE', '文学院', '中国语言文学系', 'zhou.my@campus.edu.cn', '13800000004', 'LIB2021012', DATE_SUB(CURDATE(), INTERVAL 390 DAY), DATE_ADD(CURDATE(), INTERVAL 565 DAY), 0, DATE_SUB(NOW(), INTERVAL 390 DAY)),
(4, 5, '2023015', '苏晚晴', '女', 'UNDERGRADUATE', 'LOST', '外国语学院', '英语 2302', 'su.wq@campus.edu.cn', '13800000005', 'LIB2023015', DATE_SUB(CURDATE(), INTERVAL 380 DAY), DATE_ADD(CURDATE(), INTERVAL 580 DAY), 0, DATE_SUB(NOW(), INTERVAL 380 DAY)),
(5, 6, '2022022', '顾言', '男', 'GRADUATE', 'ACTIVE', '建筑与艺术学院', '设计学 2022 级硕士', 'gu.yan@campus.edu.cn', '13800000006', 'LIB2022022', DATE_SUB(CURDATE(), INTERVAL 370 DAY), DATE_ADD(CURDATE(), INTERVAL 595 DAY), 0, DATE_SUB(NOW(), INTERVAL 370 DAY)),
(6, 7, '2023033', '沈星野', '男', 'UNDERGRADUATE', 'ACTIVE', '数学与统计学院', '统计学 2301', 'shen.xy@campus.edu.cn', '13800000007', 'LIB2023033', DATE_SUB(CURDATE(), INTERVAL 360 DAY), DATE_ADD(CURDATE(), INTERVAL 610 DAY), 0, DATE_SUB(NOW(), INTERVAL 360 DAY)),
(7, 8, '2020044', '叶青', '女', 'TEACHER', 'CANCELLED', '经济管理学院', '经济学系', 'ye.qing@campus.edu.cn', '13800000008', 'LIB2020044', DATE_SUB(CURDATE(), INTERVAL 350 DAY), DATE_ADD(CURDATE(), INTERVAL 625 DAY), 0, DATE_SUB(NOW(), INTERVAL 350 DAY)),
(8, 9, '2023051', '许一帆', '男', 'UNDERGRADUATE', 'ACTIVE', '计算机与电子信息学院', '人工智能 2301', 'xu.yf@campus.edu.cn', '13800000009', 'LIB2023051', DATE_SUB(CURDATE(), INTERVAL 340 DAY), DATE_ADD(CURDATE(), INTERVAL 640 DAY), 0, DATE_SUB(NOW(), INTERVAL 340 DAY)),
(9, 10, '2022060', '陆嘉禾', '女', 'GRADUATE', 'ACTIVE', '文学院', '现当代文学 2022 级硕士', 'lu.jh@campus.edu.cn', '13800000010', 'LIB2022060', DATE_SUB(CURDATE(), INTERVAL 330 DAY), DATE_ADD(CURDATE(), INTERVAL 655 DAY), 0, DATE_SUB(NOW(), INTERVAL 330 DAY)),
(10, 11, '2023066', '温以宁', '女', 'UNDERGRADUATE', 'ACTIVE', '法学院', '法学 2301', 'wen.yn@campus.edu.cn', '13800000011', 'LIB2023066', DATE_SUB(CURDATE(), INTERVAL 320 DAY), DATE_ADD(CURDATE(), INTERVAL 670 DAY), 0, DATE_SUB(NOW(), INTERVAL 320 DAY));

-- ----------------------------------------------------------------------------
-- 图书分类
-- ----------------------------------------------------------------------------
INSERT INTO `book_category` (`id`, `name`, `code`, `color`, `description`, `deleted`, `created_at`)
VALUES
(1, '文学', 'LIT', '#8f5c42', '中外文学经典与当代小说', 0, DATE_SUB(NOW(), INTERVAL 500 DAY)),
(2, '计算机', 'TP', '#2e7364', '计算机科学、软件工程与前沿技术', 0, DATE_SUB(NOW(), INTERVAL 500 DAY)),
(3, '历史', 'K', '#9a6d3c', '中外历史、人物传记与文明史', 0, DATE_SUB(NOW(), INTERVAL 500 DAY)),
(4, '哲学', 'B', '#6f5d80', '哲学思想、伦理与人文思辨', 0, DATE_SUB(NOW(), INTERVAL 500 DAY)),
(5, '艺术', 'J', '#a45d51', '艺术史、设计、美学与视觉文化', 0, DATE_SUB(NOW(), INTERVAL 500 DAY)),
(6, '经济管理', 'F', '#4e6c82', '经济学、管理学与商业实践', 0, DATE_SUB(NOW(), INTERVAL 500 DAY)),
(7, '自然科学', 'N', '#55745b', '数学、物理、生命科学与科普', 0, DATE_SUB(NOW(), INTERVAL 500 DAY)),
(8, '社会科学', 'C', '#876d4b', '社会学、教育学、法律与公共议题', 0, DATE_SUB(NOW(), INTERVAL 500 DAY));

-- ----------------------------------------------------------------------------
-- 图书：available_copies 已扣除 11 笔当前在借
-- ----------------------------------------------------------------------------
INSERT INTO `book` (`id`, `isbn`, `title`, `author`, `publisher`, `category_id`, `total_copies`, `available_copies`, `publish_year`, `price`, `location`, `description`, `rating`, `reservation_count`, `deleted`, `created_at`)
VALUES
(1, '9787544291170', '百年孤独', '加西亚·马尔克斯', '南海出版公司', 1, 6, 5, 2017, 55.00, 'A-01-03', '魔幻现实主义文学代表作。布恩迪亚家族七代人的传奇故事，映照拉丁美洲百年历史。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 359 DAY)),
(2, '9787506365437', '活着', '余华', '作家出版社', 1, 8, 7, 2012, 45.00, 'A-02-01', '以福贵的一生讲述生命的韧性与苦难，语言克制而力量深远。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 358 DAY)),
(3, '9787530216781', '平凡的世界', '路遥', '北京十月文艺出版社', 1, 5, 5, 2017, 98.00, 'A-02-04', '在时代变迁中书写普通人的奋斗、爱情与尊严，茅盾文学奖作品。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 357 DAY)),
(4, '9787020090006', '围城', '钱钟书', '人民文学出版社', 1, 4, 4, 1991, 39.00, 'A-03-02', '“城外的人想进去，城里的人想出来。”一部机智而锋利的现代文学经典。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 356 DAY)),
(5, '9787508647357', '人类简史', '尤瓦尔·赫拉利', '中信出版社', 3, 6, 5, 2014, 68.00, 'C-01-02', '从认知革命到科学革命，重新理解智人如何塑造今日世界。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 355 DAY)),
(6, '9787108009821', '万历十五年', '黄仁宇', '生活·读书·新知三联书店', 3, 3, 3, 1997, 42.00, 'C-02-01', '以 1587 年为切口，观察明代政治、制度与人物命运的结构性困局。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 354 DAY)),
(7, '9787301235928', '中国哲学简史', '冯友兰', '北京大学出版社', 4, 4, 3, 2013, 58.00, 'D-01-03', '在简明的脉络中梳理中国哲学精神，是了解中国传统思想的入门经典。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 353 DAY)),
(8, '9787506394864', '苏菲的世界', '乔斯坦·贾德', '作家出版社', 4, 3, 3, 2017, 68.00, 'D-02-02', '用一封封神秘来信，带领读者完成一场西方哲学史的奇妙漫游。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 352 DAY)),
(9, '9787111407010', '算法导论', '托马斯·科尔曼', '机械工业出版社', 2, 5, 4, 2013, 128.00, 'TP-01-01', '算法领域的权威教材，系统覆盖算法设计与分析方法。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 351 DAY)),
(10, '9787111544937', '深入理解计算机系统', '兰德尔·布莱恩特', '机械工业出版社', 2, 4, 3, 2016, 139.00, 'TP-01-04', '从程序员视角理解计算机系统，连接底层硬件与上层软件。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 350 DAY)),
(11, '9787115545381', 'JavaScript高级程序设计', '马特·弗里斯比', '人民邮电出版社', 2, 5, 5, 2020, 129.00, 'TP-02-02', '前端开发经典“红宝书”，全面讲解 JavaScript 语言与浏览器编程。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 349 DAY)),
(12, '9787115216878', '代码整洁之道', '罗伯特·马丁', '人民邮电出版社', 2, 3, 2, 2010, 59.00, 'TP-02-05', '关于命名、函数、测试与重构的工程实践，让代码保持可读与可维护。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 348 DAY)),
(13, '9787111554158', '软件工程：实践者的研究方法', '罗杰·普莱斯曼', '机械工业出版社', 2, 4, 4, 2017, 89.00, 'TP-03-01', '从需求到维护的软件工程全景，兼顾方法、过程与项目管理。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 347 DAY)),
(14, '9787111211266', '设计模式', '埃里希·伽玛 等', '机械工业出版社', 2, 3, 2, 2007, 69.00, 'TP-03-04', 'GoF 经典著作，归纳面向对象设计中可复用的 23 种模式。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 346 DAY)),
(15, '9787111554011', '数据结构与算法分析', '马克·艾伦·维斯', '机械工业出版社', 2, 4, 4, 2016, 79.00, 'TP-04-02', '以 C 语言描述经典数据结构与算法分析方法，兼顾理论与实践。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 345 DAY)),
(16, '9787549533848', '艺术的故事', '贡布里希', '广西师范大学出版社', 5, 3, 2, 2014, 158.00, 'E-01-01', '从洞窟壁画到现代艺术，一部视野宏阔、叙述优雅的艺术史。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 344 DAY)),
(17, '9787563358236', '设计中的设计', '原研哉', '广西师范大学出版社', 5, 3, 3, 2006, 68.00, 'E-02-03', '原研哉关于设计本质的思考，在留白与日常中发现设计的可能。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 343 DAY)),
(18, '9787301256909', '经济学原理', '格里高利·曼昆', '北京大学出版社', 6, 4, 4, 2015, 118.00, 'F-01-02', '以十大原理为线索的经济学入门，兼具理论清晰度与现实案例。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 342 DAY)),
(19, '9787300291049', '管理学', '斯蒂芬·罗宾斯', '中国人民大学出版社', 6, 3, 3, 2021, 99.00, 'F-02-01', '系统介绍计划、组织、领导与控制等管理职能与当代管理议题。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 341 DAY)),
(20, '9787508633558', '思考，快与慢', '丹尼尔·卡尼曼', '中信出版社', 8, 4, 4, 2012, 69.00, 'G-01-03', '诺贝尔经济学奖得主揭示直觉与理性两套思维系统如何影响决策。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 340 DAY)),
(21, '9787301208274', '乡土中国', '费孝通', '北京大学出版社', 8, 5, 5, 2012, 32.00, 'G-02-01', '理解中国基层社会结构与文化传统的经典社会学文本。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 339 DAY)),
(22, '9787300238418', '社会学的想象力', 'C. 赖特·米尔斯', '中国人民大学出版社', 8, 2, 2, 2017, 49.00, 'G-02-04', '将个人处境与公共议题连接起来，建立社会学观察世界的方式。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 338 DAY)),
(23, '9787535732309', '时间简史', '史蒂芬·霍金', '湖南科学技术出版社', 7, 4, 4, 2010, 45.00, 'N-01-02', '从宇宙起源到黑洞，一部面向大众的现代宇宙学经典。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 337 DAY)),
(24, '9787544852566', '万物简史', '比尔·布莱森', '接力出版社', 7, 3, 3, 2018, 68.00, 'N-02-01', '用幽默而严谨的叙述，串联宇宙、地球与生命演化的科学故事。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 336 DAY)),
(25, '9787115373557', '数学之美', '吴军', '人民邮电出版社', 2, 4, 4, 2014, 49.00, 'TP-01-05', '从信息论、搜索到自然语言处理，看见数学在现代技术中的优雅力量。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 335 DAY)),
(26, '9787550210523', '人类群星闪耀时', '斯蒂芬·茨威格', '北京联合出版公司', 3, 3, 3, 2015, 39.00, 'C-01-04', '十四个改变历史的瞬间，以文学笔法重述决定人类命运的时刻。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 334 DAY)),
(27, '9787544288644', '月亮与六便士', '威廉·萨默塞特·毛姆', '南海出版公司', 1, 4, 4, 2017, 45.00, 'A-03-04', '一个人抛下安稳生活追寻艺术理想，追问天才、自由与代价。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 333 DAY)),
(28, '9787020104345', '小王子', '安托万·德·圣埃克苏佩里', '人民文学出版社', 1, 6, 5, 2016, 36.00, 'A-01-01', '写给大人的童话，在星际旅行中谈论爱、责任与看见事物的本质。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 332 DAY)),
(29, '9787532763658', '局外人', '阿尔贝·加缪', '上海译文出版社', 1, 3, 3, 2015, 35.00, 'A-04-02', '冷峻短小的存在主义经典，叩问荒诞世界中的自由与选择。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 331 DAY)),
(30, '9787536692930', '三体', '刘慈欣', '重庆出版社', 1, 7, 6, 2008, 52.00, 'A-05-01', '地球文明与三体文明的首度接触，中国科幻的里程碑之作。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 330 DAY)),
(31, '9787544258609', '白夜行', '东野圭吾', '南海出版公司', 1, 4, 4, 2013, 49.00, 'A-05-03', '一桩悬案贯穿十九年，在黑暗中书写互为光影的两个人。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 329 DAY)),
(32, '9787213086939', '明朝那些事儿', '当年明月', '浙江人民出版社', 3, 6, 6, 2017, 108.00, 'C-03-01', '用鲜活叙事重述明朝三百年，让历史人物与制度变迁可感可触。', 4.3, 0, 0, DATE_SUB(NOW(), INTERVAL 328 DAY));

-- ----------------------------------------------------------------------------
-- 借阅记录
--   borrow_offset / due_offset / return_offset 都是“距今天数”（正数表示过去，
--   负数表示未来）；id 1-25 为历史已归还记录，id 26-36 为当前在借/逾期记录。
-- ----------------------------------------------------------------------------
INSERT INTO `borrow_record` (`id`, `reader_id`, `book_id`, `borrow_date`, `due_date`, `return_date`, `status`, `renew_count`, `operator_id`, `returned_by`, `created_at`)
SELECT
    x.`id`,
    x.`reader_id`,
    x.`book_id`,
    TIMESTAMPADD(DAY, -x.`borrow_offset`, CURDATE()),
    TIMESTAMPADD(DAY, -x.`due_offset`, CURDATE()),
    CASE WHEN x.`return_offset` IS NULL THEN NULL
         ELSE TIMESTAMPADD(DAY, -x.`return_offset`, CURDATE()) END,
    x.`status`,
    x.`renew_count`,
    1,
    CASE WHEN x.`status` = 'RETURNED' THEN 1 ELSE NULL END,
    TIMESTAMPADD(HOUR, 9, TIMESTAMPADD(DAY, -x.`borrow_offset`, CURDATE()))
FROM (
    SELECT 1 AS `id`, 1 AS `reader_id`, 3 AS `book_id`, 78 AS `borrow_offset`, 48 AS `due_offset`, 43 AS `return_offset`, 'RETURNED' AS `status`, 0 AS `renew_count`
    UNION ALL SELECT 2, 1, 27, 125, 95, 93, 'RETURNED', 1
    UNION ALL SELECT 3, 2, 13, 104, 44, 41, 'RETURNED', 0
    UNION ALL SELECT 4, 5, 17, 155, 125, 116, 'RETURNED', 0
    UNION ALL SELECT 5, 8, 21, 186, 156, 155, 'RETURNED', 0
    UNION ALL SELECT 6, 10, 24, 214, 154, 148, 'RETURNED', 1
    UNION ALL SELECT 7, 3, 15, 240, 150, 148, 'RETURNED', 0
    UNION ALL SELECT 8, 2, 11, 232, 172, 170, 'RETURNED', 0
    UNION ALL SELECT 9, 5, 25, 218, 158, 150, 'RETURNED', 0
    UNION ALL SELECT 10, 9, 6, 205, 145, 140, 'RETURNED', 0
    UNION ALL SELECT 11, 1, 4, 190, 160, 158, 'RETURNED', 0
    UNION ALL SELECT 12, 6, 8, 178, 148, 146, 'RETURNED', 0
    UNION ALL SELECT 13, 4, 18, 165, 135, 130, 'RETURNED', 0
    UNION ALL SELECT 14, 10, 19, 152, 122, 120, 'RETURNED', 0
    UNION ALL SELECT 15, 3, 32, 140, 50, 45, 'RETURNED', 0
    UNION ALL SELECT 16, 8, 22, 128, 98, 95, 'RETURNED', 0
    UNION ALL SELECT 17, 5, 23, 116, 56, 54, 'RETURNED', 0
    UNION ALL SELECT 18, 2, 26, 102, 42, 40, 'RETURNED', 0
    UNION ALL SELECT 19, 9, 31, 88, 28, 25, 'RETURNED', 0
    UNION ALL SELECT 20, 6, 29, 74, 44, 41, 'RETURNED', 0
    UNION ALL SELECT 21, 1, 20, 62, 32, 33, 'RETURNED', 0
    UNION ALL SELECT 22, 10, 21, 50, 20, 18, 'RETURNED', 0
    UNION ALL SELECT 23, 8, 16, 38, 8, 6, 'RETURNED', 0
    UNION ALL SELECT 24, 7, 2, 300, 210, 205, 'RETURNED', 0
    UNION ALL SELECT 25, 5, 3, 32, -28, 2, 'RETURNED', 0
    UNION ALL SELECT 26, 1, 30, 24, -6, NULL, 'BORROWED', 0
    UNION ALL SELECT 27, 1, 5, 38, 8, NULL, 'OVERDUE', 0
    UNION ALL SELECT 28, 2, 9, 44, -16, NULL, 'BORROWED', 1
    UNION ALL SELECT 29, 5, 1, 27, -3, NULL, 'BORROWED', 0
    UNION ALL SELECT 30, 6, 16, 34, 4, NULL, 'OVERDUE', 0
    UNION ALL SELECT 31, 8, 2, 7, -23, NULL, 'BORROWED', 0
    UNION ALL SELECT 32, 3, 7, 18, -72, NULL, 'BORROWED', 0
    UNION ALL SELECT 33, 3, 10, 25, -65, NULL, 'BORROWED', 0
    UNION ALL SELECT 34, 9, 28, 24, -6, NULL, 'BORROWED', 0
    UNION ALL SELECT 35, 10, 14, 48, -12, NULL, 'BORROWED', 0
    UNION ALL SELECT 36, 5, 12, 28, -2, NULL, 'BORROWED', 0
) AS x
ORDER BY x.`id`;

-- 冗余保存读者/图书快照，保证读者或图书被删除后历史记录仍可展示
UPDATE `borrow_record` br
    JOIN `reader` r ON r.`id` = br.`reader_id`
    JOIN `book` b ON b.`id` = br.`book_id`
SET br.`reader_name` = r.`name`,
    br.`reader_no`   = r.`student_no`,
    br.`book_title`  = b.`title`,
    br.`book_isbn`   = b.`isbn`;

-- ----------------------------------------------------------------------------
-- 罚款：仅对已归还且逾期的记录生成，每笔借阅最多一条
-- 金额 = 逾期天数 × 0.50（与 application.yml 中默认费率一致）
-- ----------------------------------------------------------------------------
INSERT INTO `penalty` (`record_id`, `reader_id`, `book_id`, `overdue_days`, `amount`, `status`, `paid_at`, `paid_by`, `payment_channel`, `rule_rate`, `created_at`)
SELECT
    br.`id`,
    br.`reader_id`,
    br.`book_id`,
    p.`days`,
    p.`amount`,
    p.`status`,
    CASE WHEN p.`status` = 'PAID' THEN DATE_ADD(br.`return_date`, INTERVAL 1 DAY) ELSE NULL END,
    CASE WHEN p.`status` = 'PAID' THEN 1 ELSE NULL END,
    CASE WHEN p.`status` = 'PAID' THEN 'CASH' ELSE NULL END,
    0.50,
    DATE_ADD(br.`return_date`, INTERVAL 17 HOUR)
FROM `borrow_record` br
JOIN (
    SELECT 1 AS `record_id`, 5 AS `days`, 2.50 AS `amount`, 'UNPAID' AS `status`
    UNION ALL SELECT 2, 2, 1.00, 'PAID'
    UNION ALL SELECT 3, 3, 1.50, 'UNPAID'
    UNION ALL SELECT 4, 9, 4.50, 'PAID'
    UNION ALL SELECT 5, 1, 0.50, 'UNPAID'
    UNION ALL SELECT 6, 6, 3.00, 'PAID'
    UNION ALL SELECT 7, 2, 1.00, 'PAID'
    UNION ALL SELECT 8, 2, 1.00, 'UNPAID'
    UNION ALL SELECT 9, 8, 4.00, 'PAID'
    UNION ALL SELECT 10, 5, 2.50, 'PAID'
    UNION ALL SELECT 11, 2, 1.00, 'PAID'
    UNION ALL SELECT 12, 2, 1.00, 'UNPAID'
    UNION ALL SELECT 13, 5, 2.50, 'PAID'
    UNION ALL SELECT 14, 2, 1.00, 'PAID'
    UNION ALL SELECT 15, 5, 2.50, 'UNPAID'
    UNION ALL SELECT 16, 3, 1.50, 'PAID'
    UNION ALL SELECT 17, 2, 1.00, 'UNPAID'
    UNION ALL SELECT 18, 2, 1.00, 'PAID'
    UNION ALL SELECT 19, 3, 1.50, 'PAID'
    UNION ALL SELECT 20, 3, 1.50, 'UNPAID'
    UNION ALL SELECT 22, 2, 1.00, 'PAID'
    UNION ALL SELECT 23, 2, 1.00, 'UNPAID'
    UNION ALL SELECT 24, 5, 2.50, 'PAID'
) AS p ON p.`record_id` = br.`id`
ORDER BY br.`id`;

-- ----------------------------------------------------------------------------
-- 登录日志（含少量失败记录）
-- ----------------------------------------------------------------------------
INSERT INTO `login_log` (`user_id`, `username`, `role`, `ip`, `success`, `created_at`)
VALUES
(1, 'admin', 'ADMIN', '127.0.0.1', 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, '2023001', 'READER', '10.24.18.36', 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(1, 'admin', 'ADMIN', '127.0.0.1', 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(3, '2022008', 'READER', '10.24.18.37', 1, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(1, 'admin', 'ADMIN', '127.0.0.1', 1, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(2, '2023001', 'READER', '10.24.18.36', 1, DATE_SUB(NOW(), INTERVAL 11 DAY)),
(1, 'admin', 'ADMIN', '127.0.0.1', 1, DATE_SUB(NOW(), INTERVAL 13 DAY)),
(NULL, 'unknown_user', NULL, '10.24.18.99', 0, DATE_SUB(NOW(), INTERVAL 15 DAY)),
(1, 'admin', 'ADMIN', '127.0.0.1', 1, DATE_SUB(NOW(), INTERVAL 17 DAY));

-- ----------------------------------------------------------------------------
-- 仪表盘公告
-- ----------------------------------------------------------------------------
INSERT INTO `announcement` (`id`, `title`, `content`, `tag`, `created_at`)
VALUES
(1, '图书馆秋季开放时间调整', '自本周起，主馆开放时间调整为 08:00–22:30，周末照常开放。', '通知', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, '新书上架：计算机与人工智能专题', '本月新增 126 种计算机类图书，欢迎前往二楼新书区借阅。', '馆藏', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(3, '关于开展逾期图书集中归还的通知', '请有逾期未还图书的读者尽快办理归还，逾期罚款按每日 0.50 元计。', '提醒', DATE_SUB(NOW(), INTERVAL 5 DAY));
