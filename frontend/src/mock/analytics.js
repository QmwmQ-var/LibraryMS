import dayjs from 'dayjs'
import {
  activeRecords,
  overdueRecords,
  penaltyView,
  readerConfig,
  recordView,
  refreshOverdue,
  today,
  todayString,
  unpaidPenalties,
} from './utils'

const inPeriod = (value, start = null, end = null) => {
  if (!value) return false
  const date = dayjs(value)
  if (start && date.isBefore(dayjs(start), 'day')) return false
  if (end && date.isAfter(dayjs(end), 'day')) return false
  return true
}

export function buildOverview(db, user, { startDate = null, endDate = null } = {}) {
  refreshOverdue(db)

  if (user?.role === 'READER') {
    const reader = db.readers.find((item) => item.id === user.readerId)
    const active = activeRecords(db, user.readerId)
    const overdue = active.filter((record) => record.status === 'OVERDUE')
    const dueSoon = active.filter((record) => {
      if (record.status === 'OVERDUE') return false
      const diff = dayjs(record.dueDate).startOf('day').diff(today(), 'day')
      return diff >= 0 && diff <= 3
    })
    const unpaid = unpaidPenalties(db, user.readerId)
    const config = readerConfig(reader)
    const history = db.records.filter((record) => record.readerId === user.readerId)
    const categoryCounts = {}
    for (const record of history) {
      const book = db.books.find((item) => item.id === record.bookId)
      const category = db.categories.find((item) => item.id === book?.categoryId)
      if (category) categoryCounts[category.name] = (categoryCounts[category.name] || 0) + 1
    }
    const favorite = Object.entries(categoryCounts).sort((a, b) => b[1] - a[1])[0]
    return {
      role: 'READER',
      activeLoans: active.length,
      overdueLoans: overdue.length,
      dueSoon: dueSoon.length,
      availableQuota: Math.max(config.maxBorrow - active.length, 0),
      maxBorrow: config.maxBorrow,
      unpaidCount: unpaid.length,
      unpaidAmount: Number(unpaid.reduce((sum, item) => sum + item.amount, 0).toFixed(2)),
      totalRead: history.length,
      favoriteCategory: favorite?.[0] || '暂无',
      favoriteCategoryCount: favorite?.[1] || 0,
      cardStatus: reader?.cardStatus || 'ACTIVE',
    }
  }

  const totalCopies = db.books.reduce((sum, book) => sum + book.totalCopies, 0)
  const availableCopies = db.books.reduce((sum, book) => sum + book.availableCopies, 0)
  const active = activeRecords(db)
  const overdue = overdueRecords(db)
  const unpaid = unpaidPenalties(db)
  const now = today()
  const rangeRecords = db.records.filter(
    (record) => inPeriod(record.borrowDate, startDate, endDate) || (!startDate && !endDate),
  )
  const currentMonthRecords = db.records.filter((record) =>
    dayjs(record.borrowDate).isSame(now, 'month'),
  )
  const lastMonthRecords = db.records.filter((record) =>
    dayjs(record.borrowDate).isSame(now.subtract(1, 'month'), 'month'),
  )
  const monthDelta =
    lastMonthRecords.length === 0
      ? 100
      : Number(
          (((currentMonthRecords.length - lastMonthRecords.length) / lastMonthRecords.length) * 100).toFixed(1),
        )
  const activeMonthReaders = new Set(
    db.records
      .filter((record) => dayjs(record.borrowDate).isSame(now, 'month'))
      .map((record) => record.readerId),
  ).size

  return {
    role: 'ADMIN',
    totalTitles: db.books.length,
    totalCopies,
    availableCopies,
    readerCount: db.readers.length,
    activeLoans: active.length,
    overdueLoans: overdue.length,
    todayBorrows: db.records.filter((record) => record.borrowDate === todayString()).length,
    monthBorrows: currentMonthRecords.length,
    monthBorrowDelta: monthDelta,
    activeMonthReaders,
    pendingFineCount: unpaid.length,
    pendingFineAmount: Number(unpaid.reduce((sum, item) => sum + item.amount, 0).toFixed(2)),
    lowStockCount: db.books.filter((book) => book.availableCopies <= 1).length,
    rangeBorrowCount: rangeRecords.length,
  }
}

export function buildMonthlyTrend(db, months = 8) {
  const labels = []
  const borrows = []
  const returns = []
  const base = today().startOf('month')
  for (let offset = months - 1; offset >= 0; offset -= 1) {
    const cursor = base.subtract(offset, 'month')
    labels.push(cursor.format('M月'))
    borrows.push(
      db.records.filter((record) => record.borrowDate && dayjs(record.borrowDate).isSame(cursor, 'month'))
        .length,
    )
    returns.push(
      db.records.filter((record) => record.returnDate && dayjs(record.returnDate).isSame(cursor, 'month'))
        .length,
    )
  }
  return { labels, borrows, returns }
}

export function buildCategoryDistribution(db) {
  return db.categories.map((category) => {
    const bookIds = db.books
      .filter((book) => book.categoryId === category.id)
      .map((book) => book.id)
    const value = db.records.filter((record) => bookIds.includes(record.bookId)).length
    return {
      id: category.id,
      name: category.name,
      value,
      color: category.color,
    }
  })
}

export function buildBookRanking(db, limit = 6, startDate = null, endDate = null) {
  return db.books
    .map((book) => {
      const count = db.records.filter(
        (record) =>
          record.bookId === book.id && inPeriod(record.borrowDate, startDate, endDate),
      ).length
      const category = db.categories.find((item) => item.id === book.categoryId)
      return {
        ...book,
        categoryName: category?.name || '未分类',
        categoryColor: category?.color || '#6f7f79',
        borrowCount: count,
      }
    })
    .sort((a, b) => b.borrowCount - a.borrowCount || b.rating - a.rating)
    .slice(0, limit)
}

export function buildReaderRanking(db, limit = 6) {
  return db.readers
    .map((reader) => ({
      ...reader,
      typeLabel: readerConfig(reader).label,
      borrowCount: db.records.filter((record) => record.readerId === reader.id).length,
      activeCount: activeRecords(db, reader.id).length,
    }))
    .sort((a, b) => b.borrowCount - a.borrowCount)
    .slice(0, limit)
}

export function buildOverdueStats(db, months = 6) {
  refreshOverdue(db)
  const overdue = overdueRecords(db)
  const detailed = overdue
    .map((record) => recordView(db, record))
    .sort((a, b) => b.overdueDays - a.overdueDays)
  const base = today().startOf('month')
  const labels = []
  const counts = []
  const amounts = []
  for (let offset = months - 1; offset >= 0; offset -= 1) {
    const cursor = base.subtract(offset, 'month')
    labels.push(cursor.format('M月'))
    const monthly = db.penalties.filter(
      (penalty) => penalty.createdAt && dayjs(penalty.createdAt).isSame(cursor, 'month'),
    )
    counts.push(monthly.length)
    amounts.push(Number(monthly.reduce((sum, item) => sum + item.amount, 0).toFixed(2)))
  }

  const buckets = [
    { label: '1–7 天', min: 1, max: 7, color: '#d5a25c' },
    { label: '8–30 天', min: 8, max: 30, color: '#bd7a4e' },
    { label: '30 天以上', min: 31, max: Infinity, color: '#b45b52' },
  ].map((bucket) => ({
    ...bucket,
    value: detailed.filter(
      (record) => record.overdueDays >= bucket.min && record.overdueDays <= bucket.max,
    ).length,
  }))

  return {
    summary: {
      count: detailed.length,
      maxDays: detailed[0]?.overdueDays || 0,
      pendingAmount: Number(
        db.penalties
          .filter((item) => item.status === 'UNPAID')
          .reduce((sum, item) => sum + item.amount, 0)
          .toFixed(2),
      ),
      totalRecords: db.records.length,
      overdueRate: db.records.length
        ? Number(((detailed.length / activeRecords(db).length || 0) * 100).toFixed(1))
        : 0,
    },
    trend: { labels, counts, amounts },
    buckets,
    records: detailed.slice(0, 8),
    recentPenalties: db.penalties
      .filter((item) => item.status === 'UNPAID')
      .slice(-5)
      .map((penalty) => penaltyView(db, penalty)),
  }
}

export function buildRecentActivities(db, user, limit = 6) {
  let records = db.records
  if (user?.role === 'READER') {
    records = records.filter((record) => record.readerId === user.readerId)
  }
  return records
    .slice()
    .sort((a, b) => String(b.createdAt).localeCompare(String(a.createdAt)))
    .slice(0, limit)
    .map((record) => {
      const view = recordView(db, record)
      return {
        id: `record-${record.id}`,
        type: record.returnDate ? 'RETURN' : 'BORROW',
        title:
          record.returnDate
            ? `${view.readerName} 归还了《${view.bookTitle}》`
            : `${view.readerName} 借阅了《${view.bookTitle}》`,
        description: `${view.readerNo} · ${view.categoryName}`,
        time: record.returnDate
          ? `${record.returnDate}T16:20:00.000Z`
          : record.createdAt,
        tone: record.returnDate ? 'green' : 'copper',
      }
    })
}

export function buildNotifications(db, user) {
  refreshOverdue(db)
  const items = []
  if (user.role === 'ADMIN') {
    const overdue = overdueRecords(db)
    const unpaid = unpaidPenalties(db)
    if (overdue.length) {
      items.push({
        id: 'overdue',
        tone: 'danger',
        title: `${overdue.length} 笔借阅已逾期`,
        description: '建议尽快联系读者办理归还，逾期将按日累计罚款。',
        path: '/circulation?status=OVERDUE',
      })
    }
    if (unpaid.length) {
      items.push({
        id: 'fines',
        tone: 'warning',
        title: `${unpaid.length} 笔罚款待确认`,
        description: `待收金额 ¥${unpaid.reduce((sum, item) => sum + item.amount, 0).toFixed(2)}，可在罚款管理中处理。`,
        path: '/penalties-manage',
      })
    }
    const lowStock = db.books.filter((book) => book.availableCopies <= 1)
    if (lowStock.length) {
      items.push({
        id: 'stock',
        tone: 'info',
        title: `${lowStock.length} 种图书可借量不足`,
        description: '可在图书管理中调整馆藏数量或安排补采。',
        path: '/books-manage?availability=UNAVAILABLE',
      })
    }
    if (!items.length) {
      items.push({
        id: 'calm',
        tone: 'success',
        title: '今日流通一切正常',
        description: '暂无逾期、待收罚款或低库存提醒。',
        path: '/dashboard',
      })
    }
  } else {
    const active = activeRecords(db, user.readerId)
    const overdue = active.filter((record) => record.status === 'OVERDUE')
    const dueSoon = active.filter((record) => {
      const diff = dayjs(record.dueDate).startOf('day').diff(today(), 'day')
      return diff >= 0 && diff <= 3 && record.status !== 'OVERDUE'
    })
    const unpaid = unpaidPenalties(db, user.readerId)
    if (overdue.length) {
      items.push({
        id: 'my-overdue',
        tone: 'danger',
        title: `${overdue.length} 本图书已逾期`,
        description: '请尽快归还，逾期罚款将按每日 ¥0.50 计算。',
        path: '/my-borrows',
      })
    }
    if (dueSoon.length) {
      items.push({
        id: 'my-due',
        tone: 'warning',
        title: `${dueSoon.length} 本图书即将到期`,
        description: '可在「我的借阅」中办理续借或归还。',
        path: '/my-borrows',
      })
    }
    if (unpaid.length) {
      items.push({
        id: 'my-fines',
        tone: 'warning',
        title: `有 ${unpaid.length} 笔罚款待缴纳`,
        description: `合计 ¥${unpaid.reduce((sum, item) => sum + item.amount, 0).toFixed(2)}。`,
        path: '/penalties',
      })
    }
    if (!items.length) {
      items.push({
        id: 'reader-calm',
        tone: 'success',
        title: '借阅状态良好',
        description: '暂无临期、逾期或待缴罚款，继续享受阅读吧。',
        path: '/my-borrows',
      })
    }
  }
  return items
}
