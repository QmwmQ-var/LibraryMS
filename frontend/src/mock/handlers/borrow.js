import dayjs from 'dayjs'
import { nextId, persistDatabase } from '../db'
import { READER_TYPES } from '@/utils/constants'
import {
  MockError,
  activeRecords,
  bookView,
  calculateOverdueDays,
  includesKeyword,
  isWithinRange,
  makePenalty,
  nowIso,
  ok,
  paginate,
  parseDateRange,
  readerConfig,
  recordView,
  refreshOverdue,
  requireAdmin,
  today,
  todayString,
} from '../utils'

const loadRecord = (db, id) => {
  const record = db.records.find((item) => item.id === Number(id))
  if (!record) throw new MockError(404, '借阅记录不存在')
  return record
}

const canOperate = (user, record) => {
  if (user.role === 'ADMIN') return true
  if (Number(user.readerId) !== Number(record.readerId)) {
    throw new MockError(403, '无权操作其他读者的借阅记录')
  }
  return true
}

const filterRecords = (db, records, query) => {
  const { start, end } = parseDateRange(query)
  return records
    .filter((record) => {
      if (query.status === 'ACTIVE') return ['BORROWED', 'OVERDUE'].includes(record.status)
      if (query.status && query.status !== 'ALL' && record.status !== query.status) return false
      if (query.readerId && record.readerId !== Number(query.readerId)) return false
      if (query.bookId && record.bookId !== Number(query.bookId)) return false
      if (!isWithinRange(record.borrowDate, start, end)) return false
      return true
    })
    .map((record) => recordView(db, record))
    .filter((record) =>
      includesKeyword(
        [record.readerName, record.readerNo, record.bookTitle, record.bookIsbn],
        query.keyword,
      ),
    )
}

export const borrowRoutes = [
  {
    method: 'GET',
    path: '/borrow-records/my',
    handler: ({ db, query, user }) => {
      if (user.role !== 'READER') throw new MockError(400, '当前账号不是读者账号')
      refreshOverdue(db)
      const records = filterRecords(
        db,
        db.records.filter((record) => Number(record.readerId) === Number(user.readerId)),
        query,
      )
      records.sort((a, b) => {
        const activeDiff =
          Number(['BORROWED', 'OVERDUE'].includes(b.status)) -
          Number(['BORROWED', 'OVERDUE'].includes(a.status))
        if (activeDiff !== 0) return activeDiff
        return b.borrowDate.localeCompare(a.borrowDate)
      })
      const summary = {
        active: records.filter((record) => ['BORROWED', 'OVERDUE'].includes(record.status)).length,
        overdue: records.filter((record) => record.status === 'OVERDUE').length,
        returned: records.filter((record) => record.status === 'RETURNED').length,
        total: db.records.filter((record) => record.readerId === user.readerId).length,
      }
      return ok({ ...paginate(records, query.page, query.size), summary })
    },
  },
  {
    method: 'GET',
    path: '/borrow-records',
    handler: ({ db, query, user }) => {
      requireAdmin({ user })
      refreshOverdue(db)
      const records = filterRecords(db, db.records, query)
      records.sort((a, b) => {
        if (a.status === 'RETURNED' && b.status !== 'RETURNED') return 1
        if (a.status !== 'RETURNED' && b.status === 'RETURNED') return -1
        return b.borrowDate.localeCompare(a.borrowDate) || b.id - a.id
      })
      return ok(paginate(records, query.page, query.size))
    },
  },
  {
    method: 'GET',
    path: '/borrow-records/:id',
    handler: ({ db, params, user }) => {
      const record = loadRecord(db, params.id)
      canOperate(user, record)
      const penalty = db.penalties.find((item) => item.recordId === record.id)
      return ok({ ...recordView(db, record), penalty })
    },
  },
  {
    method: 'POST',
    path: '/borrow-records',
    handler: ({ db, body, user }) => {
      refreshOverdue(db)
      const readerId = user.role === 'READER' ? user.readerId : Number(body.readerId)
      if (user.role === 'READER' && body.readerId && Number(body.readerId) !== user.readerId) {
        throw new MockError(403, '读者只能为自己办理借阅')
      }
      if (!readerId) throw new MockError(400, '请选择借阅读者')
      if (!body.bookId) throw new MockError(400, '请选择要借阅的图书')

      const reader = db.readers.find((item) => item.id === Number(readerId))
      if (!reader) throw new MockError(404, '读者不存在')
      if (reader.cardStatus !== 'ACTIVE') {
        const label = { LOST: '挂失', CANCELLED: '注销' }[reader.cardStatus] || '异常'
        throw new MockError(400, `借阅证已${label}，暂不能借书，请联系图书馆管理员`)
      }

      const book = db.books.find((item) => item.id === Number(body.bookId))
      if (!book) throw new MockError(404, '图书不存在或已下架')
      if (book.availableCopies <= 0) {
        throw new MockError(400, '该书暂无可借副本，请选择其他图书')
      }

      const config = READER_TYPES[reader.type]
      const currentActive = activeRecords(db, reader.id)
      if (currentActive.length >= config.maxBorrow) {
        throw new MockError(
          400,
          `已达借阅上限（${config.maxBorrow} 本），请先归还部分图书`,
        )
      }
      if (currentActive.some((record) => record.bookId === book.id)) {
        throw new MockError(400, '该读者已借阅此书，请勿重复借阅')
      }
      if (currentActive.some((record) => record.status === 'OVERDUE')) {
        // 规则允许先归还；不拦截借新书，但提示已在返回数据中体现。
      }

      const record = {
        id: nextId(db.records),
        readerId: reader.id,
        bookId: book.id,
        borrowDate: todayString(),
        dueDate: today().add(config.borrowDays, 'day').format('YYYY-MM-DD'),
        returnDate: null,
        status: 'BORROWED',
        renewCount: 0,
        operatorId: user.id,
        readerName: reader.name,
        readerNo: reader.studentNo,
        bookTitle: book.title,
        bookIsbn: book.isbn,
        createdAt: nowIso(),
      }

      db.records.push(record)
      book.availableCopies -= 1
      persistDatabase()
      return ok(
        {
          record: recordView(db, record),
          reader: {
            ...reader,
            activeBorrowCount: currentActive.length + 1,
            availableQuota: config.maxBorrow - currentActive.length - 1,
          },
        },
        `借阅成功，应还日期为 ${dayjs(record.dueDate).format('M月D日')}`,
      )
    },
  },
  {
    method: 'POST',
    path: '/borrow-records/:id/return',
    handler: ({ db, params, user }) => {
      const record = loadRecord(db, params.id)
      canOperate(user, record)
      if (!['BORROWED', 'OVERDUE'].includes(record.status)) {
        throw new MockError(400, '该借阅记录已归还，请勿重复操作')
      }
      if (user.role === 'ADMIN' && !['BORROWED', 'OVERDUE'].includes(record.status)) {
        throw new MockError(400, '借阅记录状态异常')
      }

      const returnDate = todayString()
      const overdueDays = calculateOverdueDays(record.dueDate, returnDate)
      record.returnDate = returnDate
      record.status = 'RETURNED'
      record.returnedBy = user.id

      const book = db.books.find((item) => item.id === record.bookId)
      if (book) {
        book.availableCopies = Math.min(book.totalCopies, book.availableCopies + 1)
      }

      let penalty = db.penalties.find((item) => item.recordId === record.id)
      if (overdueDays > 0 && !penalty) {
        penalty = {
          ...makePenalty(record, overdueDays),
          id: nextId(db.penalties),
        }
        db.penalties.push(penalty)
      }

      persistDatabase()
      const message =
        overdueDays > 0
          ? `归还成功，已逾期 ${overdueDays} 天，产生罚款 ¥${(overdueDays * 0.5).toFixed(2)}`
          : '归还成功，感谢您按时归还图书'
      return ok(
        {
          record: recordView(db, record),
          penalty: penalty || null,
          book: book ? bookView(db, book) : null,
        },
        message,
      )
    },
  },
  {
    method: 'POST',
    path: '/borrow-records/:id/renew',
    handler: ({ db, params, user }) => {
      refreshOverdue(db)
      const record = loadRecord(db, params.id)
      canOperate(user, record)
      if (!['BORROWED', 'OVERDUE'].includes(record.status)) {
        throw new MockError(400, '该借阅记录已结束，无法续借')
      }
      if (record.status === 'OVERDUE') {
        throw new MockError(400, '图书已逾期，无法续借，请先归还并缴纳罚款')
      }
      if (record.renewCount >= 1) {
        throw new MockError(400, '每本图书最多续借 1 次')
      }
      const book = db.books.find((item) => item.id === record.bookId)
      if (!book) throw new MockError(404, '图书已下架，无法续借')
      if ((book.reservationCount || 0) > 0) {
        throw new MockError(400, '该书已被其他读者预约，暂不能续借')
      }
      const reader = db.readers.find((item) => item.id === record.readerId)
      const config = readerConfig(reader)
      record.dueDate = dayjs(record.dueDate).add(config.borrowDays, 'day').format('YYYY-MM-DD')
      record.renewCount += 1
      persistDatabase()
      return ok(recordView(db, record), `续借成功，新的应还日期为 ${dayjs(record.dueDate).format('M月D日')}`)
    },
  },
]
