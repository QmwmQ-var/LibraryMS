import dayjs from 'dayjs'
import { FINE_RATE, READER_TYPES } from '@/utils/constants'
import { persistDatabase } from './db'

export class MockError extends Error {
  constructor(code, message, data = null) {
    super(message)
    this.name = 'MockError'
    this.code = code
    this.data = data
  }
}

export const ok = (data = null, message = 'success') => ({ code: 200, message, data })
export const fail = (code, message, data = null) => ({ code, message, data })

export const today = () => dayjs().startOf('day')
export const todayString = () => today().format('YYYY-MM-DD')
export const nowIso = () => new Date().toISOString()

export const paginate = (items, page = 1, size = 10) => {
  const currentPage = Math.max(Number(page) || 1, 1)
  const pageSize = Math.min(Math.max(Number(size) || 10, 1), 100)
  const start = (currentPage - 1) * pageSize
  return {
    records: items.slice(start, start + pageSize),
    total: items.length,
    page: currentPage,
    size: pageSize,
  }
}

export const normalizeKeyword = (value) => String(value ?? '').trim().toLowerCase()

export const includesKeyword = (fields, keyword) => {
  const term = normalizeKeyword(keyword)
  if (!term) return true
  return fields.some((field) => normalizeKeyword(field).includes(term))
}

export const readerConfig = (reader) => READER_TYPES[reader?.type] || READER_TYPES.UNDERGRADUATE

export function refreshOverdue(db) {
  const now = todayString()
  let changed = false
  for (const record of db.records) {
    if (record.status === 'BORROWED' && record.dueDate < now) {
      record.status = 'OVERDUE'
      changed = true
    }
  }
  if (changed) persistDatabase()
  return db
}

export function activeRecords(db, readerId = null) {
  return db.records.filter(
    (record) =>
      ['BORROWED', 'OVERDUE'].includes(record.status) &&
      (readerId === null || Number(record.readerId) === Number(readerId)),
  )
}

export function overdueRecords(db) {
  refreshOverdue(db)
  return db.records.filter((record) => record.status === 'OVERDUE')
}

export function unpaidPenalties(db, readerId = null) {
  return db.penalties.filter(
    (penalty) =>
      penalty.status === 'UNPAID' &&
      (readerId === null || Number(penalty.readerId) === Number(readerId)),
  )
}

export function bookView(db, book) {
  const category = db.categories.find((item) => item.id === book.categoryId)
  const activeLoans = activeRecords(db).filter((record) => record.bookId === book.id).length
  return {
    ...book,
    categoryName: category?.name || '未分类',
    categoryCode: category?.code || '',
    categoryColor: category?.color || '#6f7f79',
    activeLoans,
    available: book.availableCopies > 0,
  }
}

export function penaltyView(db, penalty) {
  const record = db.records.find((item) => item.id === penalty.recordId)
  const reader = db.readers.find((item) => item.id === penalty.readerId)
  const book = db.books.find((item) => item.id === penalty.bookId)
  return {
    ...penalty,
    readerName: reader?.name || record?.readerName || '—',
    readerNo: reader?.studentNo || record?.readerNo || '—',
    bookTitle: book?.title || record?.bookTitle || '—',
    bookIsbn: book?.isbn || record?.bookIsbn || '—',
    borrowDate: record?.borrowDate || null,
    dueDate: record?.dueDate || null,
    returnDate: record?.returnDate || null,
    recordStatus: record?.status || null,
  }
}

export function recordView(db, record) {
  const reader = db.readers.find((item) => item.id === record.readerId)
  const book = db.books.find((item) => item.id === record.bookId)
  const category = db.categories.find((item) => item.id === book?.categoryId)
  const overdueDays =
    ['BORROWED', 'OVERDUE'].includes(record.status) && record.dueDate < todayString()
      ? today().diff(dayjs(record.dueDate).startOf('day'), 'day')
      : 0
  return {
    ...record,
    readerName: reader?.name || record.readerName || '已注销读者',
    readerNo: reader?.studentNo || record.readerNo || '—',
    readerType: reader?.type || null,
    bookTitle: book?.title || record.bookTitle || '已下架图书',
    bookIsbn: book?.isbn || record.bookIsbn || '—',
    bookAuthor: book?.author || '—',
    categoryName: category?.name || '未分类',
    categoryColor: category?.color || '#6f7f79',
    overdueDays,
    canRenew:
      ['BORROWED', 'OVERDUE'].includes(record.status) &&
      record.renewCount < 1 &&
      record.status !== 'OVERDUE',
  }
}

export function readerView(db, reader) {
  const config = readerConfig(reader)
  const active = activeRecords(db, reader.id)
  const unpaid = unpaidPenalties(db, reader.id)
  const overdue = active.filter((record) => record.status === 'OVERDUE')
  return {
    ...reader,
    typeLabel: config.label,
    maxBorrow: config.maxBorrow,
    borrowDays: config.borrowDays,
    activeBorrowCount: active.length,
    overdueCount: overdue.length,
    availableQuota: Math.max(config.maxBorrow - active.length, 0),
    unpaidAmount: Number(unpaid.reduce((sum, item) => sum + item.amount, 0).toFixed(2)),
    totalBorrowCount: db.records.filter((record) => record.readerId === reader.id).length,
  }
}

export function publicUser(db, user) {
  const reader = user.role === 'READER' ? db.readers.find((item) => item.id === user.readerId) : null
  return {
    id: user.id,
    username: user.username,
    name: user.name,
    role: user.role,
    title: user.title,
    email: user.email,
    phone: user.phone,
    avatarColor: user.avatarColor,
    readerId: user.readerId || null,
    reader: reader ? readerView(db, reader) : null,
  }
}

export function requireAdmin(ctx) {
  if (ctx.user.role !== 'ADMIN') {
    throw new MockError(403, '该操作仅限图书管理员')
  }
}

export function requireReaderOrAdmin(ctx, readerId) {
  if (ctx.user.role === 'ADMIN') return
  if (Number(ctx.user.readerId) !== Number(readerId)) {
    throw new MockError(403, '无权访问其他读者的数据')
  }
}

export function calculateOverdueDays(dueDate, returnDate = todayString()) {
  const diff = dayjs(returnDate).startOf('day').diff(dayjs(dueDate).startOf('day'), 'day')
  return Math.max(diff, 0)
}

export function makePenalty(record, days) {
  return {
    id: 0,
    recordId: record.id,
    readerId: record.readerId,
    bookId: record.bookId,
    overdueDays: days,
    amount: Number((days * FINE_RATE).toFixed(2)),
    status: 'UNPAID',
    paidAt: null,
    paidBy: null,
    createdAt: nowIso(),
    ruleRate: FINE_RATE,
  }
}

export function parseDateRange(query) {
  const start = query.startDate ? dayjs(query.startDate) : null
  const end = query.endDate ? dayjs(query.endDate) : null
  return { start, end }
}

export function isWithinRange(value, start, end) {
  if (!value) return false
  const date = dayjs(value)
  if (start && date.isBefore(start, 'day')) return false
  if (end && date.isAfter(end, 'day')) return false
  return true
}
