import dayjs from 'dayjs'
import { FINE_RATE, READER_TYPES } from '@/utils/constants'

/**
 * 演示数据种子。
 *
 * 仅 Mock 层使用，接入真实 Spring Boot 服务后不会进入生产包的业务逻辑。
 * 数据覆盖：馆藏、读者、在借、历史借阅、逾期罚款与登录日志，
 * 便于直接展示所有前端页面和图表。
 */

const startOfToday = () => dayjs().startOf('day')
const dateOffset = (days) => startOfToday().add(days, 'day').format('YYYY-MM-DD')
const isoOffset = (days) => startOfToday().add(days, 'day').hour(10).minute(20).second(0).toISOString()

const categories = [
  { id: 1, name: '文学', code: 'LIT', color: '#8f5c42', description: '中外文学经典与当代小说' },
  { id: 2, name: '计算机', code: 'TP', color: '#2e7364', description: '计算机科学、软件工程与前沿技术' },
  { id: 3, name: '历史', code: 'K', color: '#9a6d3c', description: '中外历史、人物传记与文明史' },
  { id: 4, name: '哲学', code: 'B', color: '#6f5d80', description: '哲学思想、伦理与人文思辨' },
  { id: 5, name: '艺术', code: 'J', color: '#a45d51', description: '艺术史、设计、美学与视觉文化' },
  { id: 6, name: '经济管理', code: 'F', color: '#4e6c82', description: '经济学、管理学与商业实践' },
  { id: 7, name: '自然科学', code: 'N', color: '#55745b', description: '数学、物理、生命科学与科普' },
  { id: 8, name: '社会科学', code: 'C', color: '#876d4b', description: '社会学、教育学、法律与公共议题' },
]

const rawBooks = [
  [1, '9787544291170', '百年孤独', '加西亚·马尔克斯', '南海出版公司', 1, 6, 2017, 55, 'A-01-03', '魔幻现实主义文学代表作。布恩迪亚家族七代人的传奇故事，映照拉丁美洲百年历史。'],
  [2, '9787506365437', '活着', '余华', '作家出版社', 1, 8, 2012, 45, 'A-02-01', '以福贵的一生讲述生命的韧性与苦难，语言克制而力量深远。'],
  [3, '9787530216781', '平凡的世界', '路遥', '北京十月文艺出版社', 1, 5, 2017, 98, 'A-02-04', '在时代变迁中书写普通人的奋斗、爱情与尊严，茅盾文学奖作品。'],
  [4, '9787020090006', '围城', '钱钟书', '人民文学出版社', 1, 4, 1991, 39, 'A-03-02', '“城外的人想进去，城里的人想出来。”一部机智而锋利的现代文学经典。'],
  [5, '9787508647357', '人类简史', '尤瓦尔·赫拉利', '中信出版社', 3, 6, 2014, 68, 'C-01-02', '从认知革命到科学革命，重新理解智人如何塑造今日世界。'],
  [6, '9787108009821', '万历十五年', '黄仁宇', '生活·读书·新知三联书店', 3, 3, 1997, 42, 'C-02-01', '以 1587 年为切口，观察明代政治、制度与人物命运的结构性困局。'],
  [7, '9787301235928', '中国哲学简史', '冯友兰', '北京大学出版社', 4, 4, 2013, 58, 'D-01-03', '在简明的脉络中梳理中国哲学精神，是了解中国传统思想的入门经典。'],
  [8, '9787506394864', '苏菲的世界', '乔斯坦·贾德', '作家出版社', 4, 3, 2017, 68, 'D-02-02', '用一封封神秘来信，带领读者完成一场西方哲学史的奇妙漫游。'],
  [9, '9787111407010', '算法导论', '托马斯·科尔曼', '机械工业出版社', 2, 5, 2013, 128, 'TP-01-01', '算法领域的权威教材，系统覆盖算法设计与分析方法。'],
  [10, '9787111544937', '深入理解计算机系统', '兰德尔·布莱恩特', '机械工业出版社', 2, 4, 2016, 139, 'TP-01-04', '从程序员视角理解计算机系统，连接底层硬件与上层软件。'],
  [11, '9787115545381', 'JavaScript高级程序设计', '马特·弗里斯比', '人民邮电出版社', 2, 5, 2020, 129, 'TP-02-02', '前端开发经典“红宝书”，全面讲解 JavaScript 语言与浏览器编程。'],
  [12, '9787115216878', '代码整洁之道', '罗伯特·马丁', '人民邮电出版社', 2, 3, 2010, 59, 'TP-02-05', '关于命名、函数、测试与重构的工程实践，让代码保持可读与可维护。'],
  [13, '9787111554158', '软件工程：实践者的研究方法', '罗杰·普莱斯曼', '机械工业出版社', 2, 4, 2017, 89, 'TP-03-01', '从需求到维护的软件工程全景，兼顾方法、过程与项目管理。'],
  [14, '9787111211266', '设计模式', '埃里希·伽玛 等', '机械工业出版社', 2, 3, 2007, 69, 'TP-03-04', 'GoF 经典著作，归纳面向对象设计中可复用的 23 种模式。'],
  [15, '9787111554011', '数据结构与算法分析', '马克·艾伦·维斯', '机械工业出版社', 2, 4, 2016, 79, 'TP-04-02', '以 C 语言描述经典数据结构与算法分析方法，兼顾理论与实践。'],
  [16, '9787549533848', '艺术的故事', '贡布里希', '广西师范大学出版社', 5, 3, 2014, 158, 'E-01-01', '从洞窟壁画到现代艺术，一部视野宏阔、叙述优雅的艺术史。'],
  [17, '9787563358236', '设计中的设计', '原研哉', '广西师范大学出版社', 5, 3, 2006, 68, 'E-02-03', '原研哉关于设计本质的思考，在留白与日常中发现设计的可能。'],
  [18, '9787301256909', '经济学原理', '格里高利·曼昆', '北京大学出版社', 6, 4, 2015, 118, 'F-01-02', '以十大原理为线索的经济学入门，兼具理论清晰度与现实案例。'],
  [19, '9787300291049', '管理学', '斯蒂芬·罗宾斯', '中国人民大学出版社', 6, 3, 2021, 99, 'F-02-01', '系统介绍计划、组织、领导与控制等管理职能与当代管理议题。'],
  [20, '9787508633558', '思考，快与慢', '丹尼尔·卡尼曼', '中信出版社', 8, 4, 2012, 69, 'G-01-03', '诺贝尔经济学奖得主揭示直觉与理性两套思维系统如何影响决策。'],
  [21, '9787301208274', '乡土中国', '费孝通', '北京大学出版社', 8, 5, 2012, 32, 'G-02-01', '理解中国基层社会结构与文化传统的经典社会学文本。'],
  [22, '9787300238418', '社会学的想象力', 'C. 赖特·米尔斯', '中国人民大学出版社', 8, 2, 2017, 49, 'G-02-04', '将个人处境与公共议题连接起来，建立社会学观察世界的方式。'],
  [23, '9787535732309', '时间简史', '史蒂芬·霍金', '湖南科学技术出版社', 7, 4, 2010, 45, 'N-01-02', '从宇宙起源到黑洞，一部面向大众的现代宇宙学经典。'],
  [24, '9787544852566', '万物简史', '比尔·布莱森', '接力出版社', 7, 3, 2018, 68, 'N-02-01', '用幽默而严谨的叙述，串联宇宙、地球与生命演化的科学故事。'],
  [25, '9787115373557', '数学之美', '吴军', '人民邮电出版社', 2, 4, 2014, 49, 'TP-01-05', '从信息论、搜索到自然语言处理，看见数学在现代技术中的优雅力量。'],
  [26, '9787550210523', '人类群星闪耀时', '斯蒂芬·茨威格', '北京联合出版公司', 3, 3, 2015, 39, 'C-01-04', '十四个改变历史的瞬间，以文学笔法重述决定人类命运的时刻。'],
  [27, '9787544288644', '月亮与六便士', '威廉·萨默塞特·毛姆', '南海出版公司', 1, 4, 2017, 45, 'A-03-04', '一个人抛下安稳生活追寻艺术理想，追问天才、自由与代价。'],
  [28, '9787020104345', '小王子', '安托万·德·圣埃克苏佩里', '人民文学出版社', 1, 6, 2016, 36, 'A-01-01', '写给大人的童话，在星际旅行中谈论爱、责任与看见事物的本质。'],
  [29, '9787532763658', '局外人', '阿尔贝·加缪', '上海译文出版社', 1, 3, 2015, 35, 'A-04-02', '冷峻短小的存在主义经典，叩问荒诞世界中的自由与选择。'],
  [30, '9787536692930', '三体', '刘慈欣', '重庆出版社', 1, 7, 2008, 52, 'A-05-01', '地球文明与三体文明的首度接触，中国科幻的里程碑之作。'],
  [31, '9787544258609', '白夜行', '东野圭吾', '南海出版公司', 1, 4, 2013, 49, 'A-05-03', '一桩悬案贯穿十九年，在黑暗中书写互为光影的两个人。'],
  [32, '9787213086939', '明朝那些事儿', '当年明月', '浙江人民出版社', 3, 6, 2017, 108, 'C-03-01', '用鲜活叙事重述明朝三百年，让历史人物与制度变迁可感可触。'],
  [33, '9787544274665', '雪国', '川端康成', '南海出版公司', 1, 3, 2022, 49, 'A-06-02', '穿过县界长长的隧道便是雪国。以洁净克制的文字书写虚无与美，日本文学经典。'],
]

const rawReaders = [
  [1, '2023001', '林知夏', '女', 'UNDERGRADUATE', 'ACTIVE', '计算机与电子信息学院', '软件工程 2301', 'lin.zhixia@campus.edu.cn', '13800000002'],
  [2, '2022008', '陈屿', '男', 'GRADUATE', 'ACTIVE', '计算机与电子信息学院', '软件工程 2022 级硕士', 'chen.yu@campus.edu.cn', '13800000003'],
  [3, '2021012', '周明远', '男', 'TEACHER', 'ACTIVE', '文学院', '中国语言文学系', 'zhou.my@campus.edu.cn', '13800000004'],
  [4, '2023015', '苏晚晴', '女', 'UNDERGRADUATE', 'LOST', '外国语学院', '英语 2302', 'su.wq@campus.edu.cn', '13800000005'],
  [5, '2022022', '顾言', '男', 'GRADUATE', 'ACTIVE', '建筑与艺术学院', '设计学 2022 级硕士', 'gu.yan@campus.edu.cn', '13800000006'],
  [6, '2023033', '沈星野', '男', 'UNDERGRADUATE', 'ACTIVE', '数学与统计学院', '统计学 2301', 'shen.xy@campus.edu.cn', '13800000007'],
  [7, '2020044', '叶青', '女', 'TEACHER', 'CANCELLED', '经济管理学院', '经济学系', 'ye.qing@campus.edu.cn', '13800000008'],
  [8, '2023051', '许一帆', '男', 'UNDERGRADUATE', 'ACTIVE', '计算机与电子信息学院', '人工智能 2301', 'xu.yf@campus.edu.cn', '13800000009'],
  [9, '2022060', '陆嘉禾', '女', 'GRADUATE', 'ACTIVE', '文学院', '现当代文学 2022 级硕士', 'lu.jh@campus.edu.cn', '13800000010'],
  [10, '2023066', '温以宁', '女', 'UNDERGRADUATE', 'ACTIVE', '法学院', '法学 2301', 'wen.yn@campus.edu.cn', '13800000011'],
]

function buildBooks() {
  return rawBooks.map(
    ([id, isbn, title, author, publisher, categoryId, totalCopies, publishYear, price, location, description]) => ({
      id,
      isbn,
      title,
      author,
      publisher,
      categoryId,
      totalCopies,
      availableCopies: totalCopies,
      publishYear,
      price,
      location,
      description,
      rating: Number((4.3 + ((id * 7) % 7) / 10).toFixed(1)),
      reservationCount: 0,
      createdAt: isoOffset(-360 + id),
    }),
  )
}

function buildReaders(users) {
  return rawReaders.map(([id, studentNo, name, gender, type, cardStatus, department, major, email, phone]) => ({
    id,
    userId: users.find((item) => item.readerId === id).id,
    studentNo,
    name,
    gender,
    type,
    cardStatus,
    department,
    major,
    email,
    phone,
    cardNo: `LIB${studentNo}`,
    cardIssuedAt: dateOffset(-420 + id * 10),
    cardExpiresAt: dateOffset(520 + id * 15),
    createdAt: isoOffset(-420 + id * 10),
  }))
}

function createPenalty(records, { id, record, days, paid, paidDaysAgo }) {
  return {
    id,
    recordId: record.id,
    readerId: record.readerId,
    bookId: record.bookId,
    overdueDays: days,
    amount: Number((days * FINE_RATE).toFixed(2)),
    status: paid ? 'PAID' : 'UNPAID',
    paidAt: paid ? isoOffset(-paidDaysAgo) : null,
    paidBy: paid ? 1 : null,
    createdAt: record.returnDate ? `${record.returnDate}T17:00:00.000Z` : isoOffset(-1),
    ruleRate: FINE_RATE,
  }
}

function buildRecordsAndPenalties(books, readers) {
  const records = []
  const penalties = []
  let recordId = 1
  let penaltyId = 1

  const addRecord = (payload) => {
    const record = {
      id: recordId++,
      renewCount: 0,
      operatorId: 1,
      ...payload,
    }
    records.push(record)
    return record
  }

  // 精选历史借阅：确保读者端与罚款页有可展示的完整数据。
  const landmarkHistory = [
    { readerId: 1, bookId: 3, borrow: -78, due: -48, returned: -43, renew: 0, late: 5, paid: false },
    { readerId: 1, bookId: 27, borrow: -125, due: -95, returned: -93, renew: 1, late: 2, paid: true },
    { readerId: 2, bookId: 13, borrow: -104, due: -44, returned: -41, renew: 0, late: 3, paid: false },
    { readerId: 5, bookId: 17, borrow: -155, due: -125, returned: -116, renew: 0, late: 9, paid: true },
    { readerId: 8, bookId: 21, borrow: -186, due: -156, returned: -155, renew: 0, late: 1, paid: false },
    { readerId: 10, bookId: 24, borrow: -214, due: -154, returned: -148, renew: 1, late: 6, paid: true },
  ]

  for (const item of landmarkHistory) {
    const record = addRecord({
      readerId: item.readerId,
      bookId: item.bookId,
      borrowDate: dateOffset(item.borrow),
      dueDate: dateOffset(item.due),
      returnDate: dateOffset(item.returned),
      status: 'RETURNED',
      renewCount: item.renew,
      readerName: readers.find((r) => r.id === item.readerId)?.name,
      readerNo: readers.find((r) => r.id === item.readerId)?.studentNo,
      bookTitle: books.find((b) => b.id === item.bookId)?.title,
      bookIsbn: books.find((b) => b.id === item.bookId)?.isbn,
      createdAt: isoOffset(item.borrow),
    })
    if (item.late > 0) {
      penalties.push(
        createPenalty(records, {
          id: penaltyId++,
          record,
          days: item.late,
          paid: item.paid,
          paidDaysAgo: Math.abs(item.returned) - 2,
        }),
      )
    }
  }

  // 用确定性伪随机数生成过去八个月的借阅历史，保证图表稳定且可复现。
  let seed = 20240517
  const random = () => {
    seed = (seed * 9301 + 49297) % 233280
    return seed / 233280
  }
  const pick = (list) => list[Math.floor(random() * list.length)]
  const today = startOfToday()

  for (let monthOffset = 7; monthOffset >= 0; monthOffset -= 1) {
    const cursor = today.subtract(monthOffset, 'month')
    const monthStart = cursor.startOf('month')
    const daysInMonth = cursor.daysInMonth()
    const availableDays =
      monthOffset === 0 ? Math.max(cursor.date() - 1, 4) : Math.max(daysInMonth - 2, 8)
    const count = 5 + Math.floor(random() * 5)

    for (let index = 0; index < count; index += 1) {
      const reader = pick(readers)
      const book = pick(books)
      if (reader.cardStatus !== 'ACTIVE') continue

      const borrowDay = monthStart.add(Math.floor(random() * availableDays), 'day')
      const config = READER_TYPES[reader.type]
      const dueDay = borrowDay.add(config.borrowDays, 'day')
      const isLate = random() < 0.2
      const lateDays = isLate ? 1 + Math.floor(random() * 10) : 0
      const returnedDay = dueDay.add(lateDays, 'day').subtract(isLate ? 0 : Math.floor(random() * 8), 'day')

      if (returnedDay.isAfter(today, 'day')) continue

      const record = addRecord({
        readerId: reader.id,
        bookId: book.id,
        borrowDate: borrowDay.format('YYYY-MM-DD'),
        dueDate: dueDay.format('YYYY-MM-DD'),
        returnDate: returnedDay.format('YYYY-MM-DD'),
        status: 'RETURNED',
        renewCount: random() < 0.2 ? 1 : 0,
        readerName: reader.name,
        readerNo: reader.studentNo,
        bookTitle: book.title,
        bookIsbn: book.isbn,
        createdAt: borrowDay.hour(9).minute(40).toISOString(),
      })

      if (isLate && lateDays > 0) {
        penalties.push(
          createPenalty(records, {
            id: penaltyId++,
            record,
            days: lateDays,
            paid: random() < 0.58,
            paidDaysAgo: 1 + Math.floor(random() * 60),
          }),
        )
      }
    }
  }

  // 当前在借：包含临期、逾期与不同读者类型，便于演示续借、还书和提醒。
  const activeLoans = [
    [1, 30, -24, 6, 0, 'BORROWED'],
    [1, 5, -38, -8, 0, 'OVERDUE'],
    [2, 9, -44, 16, 1, 'BORROWED'],
    [5, 1, -27, 3, 0, 'BORROWED'],
    [6, 16, -34, -4, 0, 'OVERDUE'],
    [8, 2, -7, 23, 0, 'BORROWED'],
    [3, 7, -18, 72, 0, 'BORROWED'],
    [3, 10, -25, 65, 0, 'BORROWED'],
    [9, 28, -24, 6, 0, 'BORROWED'],
    [10, 14, -48, 12, 0, 'BORROWED'],
    [5, 12, -28, 2, 0, 'BORROWED'],
    [8, 33, -12, 18, 0, 'BORROWED'],
    [10, 33, -22, 8, 0, 'BORROWED'],
  ]

  for (const [readerId, bookId, borrowOffset, dueOffset, renewCount, status] of activeLoans) {
    const reader = readers.find((item) => item.id === readerId)
    const book = books.find((item) => item.id === bookId)
    addRecord({
      readerId,
      bookId,
      borrowDate: dateOffset(borrowOffset),
      dueDate: dateOffset(dueOffset),
      returnDate: null,
      status,
      renewCount,
      readerName: reader.name,
      readerNo: reader.studentNo,
      bookTitle: book.title,
      bookIsbn: book.isbn,
      createdAt: isoOffset(borrowOffset),
    })
    book.availableCopies -= 1
  }

  return { records, penalties }
}

export function seedDatabase() {
  const users = [
    {
      id: 1,
      username: 'admin',
      password: 'admin123',
      name: '顾清和',
      role: 'ADMIN',
      title: '流通服务部 · 馆员',
      email: 'admin@library.edu.cn',
      phone: '13800000001',
      avatarColor: '#2e7364',
      status: 'ACTIVE',
      failedAttempts: 0,
      lockedUntil: null,
      createdAt: isoOffset(-500),
    },
    ...rawReaders.map(([readerId, studentNo, name], index) => ({
      id: index + 2,
      username: studentNo,
      password: 'reader123',
      name,
      role: 'READER',
      title: READER_TYPES[rawReaders[index][4]].label,
      email: rawReaders[index][8],
      phone: rawReaders[index][9],
      readerId,
      avatarColor: ['#8f5c42', '#4e6c82', '#6f5d80', '#55745b', '#a45d51'][index % 5],
      status: 'ACTIVE',
      failedAttempts: 0,
      lockedUntil: null,
      createdAt: isoOffset(-420 + readerId * 10),
    })),
  ]

  const books = buildBooks()
  const readers = buildReaders(users)
  const { records, penalties } = buildRecordsAndPenalties(books, readers)

  // 预约演示：《雪国》3 册中 2 册在借，1 册已到书留给林知夏，陈屿排队等候。
  const demoBook = books.find((book) => book.id === 33)
  demoBook.reservationCount = 2
  demoBook.availableCopies -= 1
  const reservations = [
    {
      id: 1,
      readerId: 1,
      bookId: 33,
      status: 'READY',
      requestedAt: dayjs().subtract(3, 'day').hour(10).minute(15).toISOString(),
      readyAt: dayjs().subtract(1, 'day').hour(9).minute(30).toISOString(),
      expireAt: dayjs().add(1, 'day').hour(9).minute(30).toISOString(),
      closedAt: null,
      borrowRecordId: null,
      cancelReason: null,
      readerName: readers.find((reader) => reader.id === 1)?.name,
      readerNo: readers.find((reader) => reader.id === 1)?.studentNo,
      bookTitle: demoBook.title,
      bookIsbn: demoBook.isbn,
      createdAt: dayjs().subtract(3, 'day').hour(10).minute(15).toISOString(),
    },
    {
      id: 2,
      readerId: 2,
      bookId: 33,
      status: 'PENDING',
      requestedAt: dayjs().subtract(2, 'day').hour(14).minute(20).toISOString(),
      readyAt: null,
      expireAt: null,
      closedAt: null,
      borrowRecordId: null,
      cancelReason: null,
      readerName: readers.find((reader) => reader.id === 2)?.name,
      readerNo: readers.find((reader) => reader.id === 2)?.studentNo,
      bookTitle: demoBook.title,
      bookIsbn: demoBook.isbn,
      createdAt: dayjs().subtract(2, 'day').hour(14).minute(20).toISOString(),
    },
  ]

  const loginLogs = Array.from({ length: 9 }).map((_, index) => ({
    id: index + 1,
    username: index % 4 === 0 ? '2023001' : 'admin',
    role: index % 4 === 0 ? 'READER' : 'ADMIN',
    ip: index % 3 === 0 ? '10.24.18.36' : '127.0.0.1',
    success: true,
    createdAt: isoOffset(-index * 2 - 1),
  }))

  return {
    version: 2,
    seededAt: new Date().toISOString(),
    categories,
    books,
    readers,
    users,
    records,
    penalties,
    reservations,
    loginLogs,
    announcements: [
      {
        id: 1,
        title: '图书馆秋季开放时间调整',
        content: '自本周起，主馆开放时间调整为 08:00–22:30，周末照常开放。',
        tag: '通知',
        createdAt: isoOffset(-1),
      },
      {
        id: 2,
        title: '新书上架：计算机与人工智能专题',
        content: '本月新增 126 种计算机类图书，欢迎前往二楼新书区借阅。',
        tag: '馆藏',
        createdAt: isoOffset(-3),
      },
      {
        id: 3,
        title: '关于开展逾期图书集中归还的通知',
        content: '请有逾期未还图书的读者尽快办理归还，逾期罚款按每日 0.50 元计。',
        tag: '提醒',
        createdAt: isoOffset(-5),
      },
    ],
  }
}
