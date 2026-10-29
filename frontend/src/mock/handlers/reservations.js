import { nextId, persistDatabase } from '../db'
import {
  MockError,
  includesKeyword,
  ok,
  paginate,
  refreshOverdue,
  requireAdmin,
  requireReaderOrAdmin,
} from '../utils'
import {
  findReadyReservation,
  isActiveReservation,
  promoteNextReservation,
  refreshReservations,
  reservationSummary,
  reservationView,
  sortReservationQueue,
} from '../reservations'
import { MAX_ACTIVE_RESERVATIONS, RESERVATION_ACTIVE_STATUSES } from '@/utils/constants'

const reservationList = (db, list) =>
  list
    .map((item) => reservationView(db, item))
    .sort((a, b) => {
      const aActive = isActiveReservation(a.status)
      const bActive = isActiveReservation(b.status)
      if (aActive !== bActive) return aActive ? -1 : 1
      if (aActive) {
        return String(a.requestedAt).localeCompare(String(b.requestedAt)) || a.id - b.id
      }
      return String(b.requestedAt).localeCompare(String(a.requestedAt))
    })

const filterByStatus = (items, status) => {
  if (!status || status === 'ALL') return items
  if (status === 'ACTIVE') return items.filter((item) => isActiveReservation(item.status))
  return items.filter((item) => item.status === status)
}

export const reservationRoutes = [
  {
    method: 'GET',
    path: '/reservations/summary',
    handler: ({ db, user }) => {
      refreshReservations(db)
      const readerId = user.role === 'READER' ? user.readerId : null
      return ok(reservationSummary(db, readerId))
    },
  },
  {
    method: 'GET',
    path: '/reservations/my',
    handler: ({ db, query, user }) => {
      if (user.role !== 'READER') throw new MockError(400, '当前账号不是读者账号')
      refreshReservations(db)
      const own = db.reservations.filter((item) => item.readerId === Number(user.readerId))
      const filtered = filterByStatus(own, query.status).filter((item) => {
        const view = reservationView(db, item)
        return includesKeyword(
          [view.readerName, view.readerNo, view.bookTitle, view.bookIsbn],
          query.keyword,
        )
      })
      const records = reservationList(db, filtered)
      return ok({
        ...paginate(records, query.page, query.size),
        summary: reservationSummary(db, user.readerId),
      })
    },
  },
  {
    method: 'GET',
    path: '/reservations',
    handler: ({ db, query, user }) => {
      requireAdmin({ user })
      refreshReservations(db)
      const filtered = filterByStatus(db.reservations, query.status).filter((item) => {
        const view = reservationView(db, item)
        return includesKeyword(
          [view.readerName, view.readerNo, view.bookTitle, view.bookIsbn],
          query.keyword,
        )
      })
      const records = reservationList(db, filtered)
      return ok({
        ...paginate(records, query.page, query.size),
        summary: reservationSummary(db),
      })
    },
  },
  {
    method: 'POST',
    path: '/reservations',
    handler: ({ db, body, user }) => {
      if (user.role !== 'READER') throw new MockError(403, '预约仅支持读者账号发起')
      refreshReservations(db)
      const reader = db.readers.find((item) => item.id === Number(user.readerId))
      if (!reader) throw new MockError(404, '读者不存在')
      if (reader.cardStatus !== 'ACTIVE') {
        throw new MockError(400, '借阅证状态异常，暂不能预约，请联系图书馆管理员')
      }
      const book = db.books.find((item) => item.id === Number(body.bookId))
      if (!book) throw new MockError(404, '图书不存在或已下架')
      if (book.availableCopies > 0) {
        throw new MockError(400, '当前有可借副本，请直接借阅')
      }

      const existing = db.reservations.filter(
        (item) =>
          item.readerId === reader.id &&
          item.bookId === book.id &&
          isActiveReservation(item.status),
      )
      if (existing.some((item) => item.status === 'READY')) {
        throw new MockError(400, '该书已为您到书，请直接借阅')
      }
      if (existing.length) throw new MockError(400, '您已预约该书，请耐心等待')

      const activeBorrow = db.records.some(
        (item) =>
          item.readerId === reader.id &&
          item.bookId === book.id &&
          ['BORROWED', 'OVERDUE'].includes(item.status),
      )
      if (activeBorrow) throw new MockError(400, '您已借阅该书，请勿重复预约')

      const activeCount = db.reservations.filter(
        (item) => item.readerId === reader.id && isActiveReservation(item.status),
      ).length
      if (activeCount >= MAX_ACTIVE_RESERVATIONS) {
        throw new MockError(400, `同时最多持有 ${MAX_ACTIVE_RESERVATIONS} 个预约`)
      }

      const now = new Date().toISOString()
      const reservation = {
        id: nextId(db.reservations),
        readerId: reader.id,
        bookId: book.id,
        status: 'PENDING',
        requestedAt: now,
        readyAt: null,
        expireAt: null,
        closedAt: null,
        borrowRecordId: null,
        cancelReason: null,
        readerName: reader.name,
        readerNo: reader.studentNo,
        bookTitle: book.title,
        bookIsbn: book.isbn,
        createdAt: now,
        updatedAt: now,
      }
      db.reservations.push(reservation)
      book.reservationCount = (book.reservationCount || 0) + 1
      persistDatabase()
      const view = reservationView(db, reservation)
      return ok(view, `预约成功，当前排在第 ${view.queuePosition || 1} 位`)
    },
  },
  {
    method: 'POST',
    path: '/reservations/:id/cancel',
    handler: ({ db, params, user }) => {
      refreshReservations(db)
      const reservation = db.reservations.find((item) => item.id === Number(params.id))
      if (!reservation) throw new MockError(404, '预约记录不存在')
      requireReaderOrAdmin({ user }, reservation.readerId)
      if (!RESERVATION_ACTIVE_STATUSES.includes(reservation.status)) {
        throw new MockError(400, '该预约已结束，无法取消')
      }

      const wasReady = reservation.status === 'READY'
      reservation.status = 'CANCELLED'
      reservation.closedAt = new Date().toISOString()
      reservation.cancelReason = user.role === 'ADMIN' ? '管理员取消预约' : '读者主动取消'
      reservation.updatedAt = reservation.closedAt
      const book = db.books.find((item) => item.id === reservation.bookId)
      if (book) {
        book.reservationCount = Math.max(0, (book.reservationCount || 0) - 1)
        if (wasReady) {
          book.availableCopies = Math.min(book.totalCopies, book.availableCopies + 1)
          promoteNextReservation(db, book.id)
        }
      }
      persistDatabase()
      return ok(reservationView(db, reservation), '预约已取消')
    },
  },
]

export { findReadyReservation, promoteNextReservation, refreshReservations, reservationSummary }
