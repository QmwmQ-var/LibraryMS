import dayjs from 'dayjs'
import {
  RESERVATION_ACTIVE_STATUSES,
  RESERVATION_HOLD_HOURS,
  RESERVATION_STATUS,
} from '@/utils/constants'

/**
 * 预约状态与库存联动工具。
 *
 * 与后端保持一致：
 * - READY 会从 availableCopies 中留出一册；
 * - 取消/过期 READY 时释放库存并顺延队首；
 * - 借阅 READY 时不再扣库存。
 */

export const isActiveReservation = (status) => RESERVATION_ACTIVE_STATUSES.includes(status)

export function sortReservationQueue(list) {
  return [...list].sort((a, b) => {
    const time = String(a.requestedAt).localeCompare(String(b.requestedAt))
    return time !== 0 ? time : a.id - b.id
  })
}

export function findReadyReservation(db, readerId, bookId) {
  return db.reservations.find(
    (item) =>
      item.status === 'READY' &&
      Number(item.readerId) === Number(readerId) &&
      Number(item.bookId) === Number(bookId),
  )
}

export function promoteNextReservation(db, bookId) {
  const book = db.books.find((item) => item.id === Number(bookId))
  if (!book || book.availableCopies <= 0) return null
  const next = sortReservationQueue(
    db.reservations.filter(
      (item) => item.bookId === Number(bookId) && item.status === 'PENDING',
    ),
  )[0]
  if (!next) return null

  book.availableCopies -= 1
  next.status = 'READY'
  next.readyAt = new Date().toISOString()
  next.expireAt = dayjs().add(RESERVATION_HOLD_HOURS, 'hour').toISOString()
  next.updatedAt = new Date().toISOString()
  return next
}

export function refreshReservations(db) {
  const now = dayjs()
  let changed = false
  const expired = db.reservations.filter(
    (item) =>
      item.status === 'READY' && item.expireAt && dayjs(item.expireAt).isBefore(now),
  )
  for (const reservation of expired) {
    reservation.status = 'EXPIRED'
    reservation.closedAt = now.toISOString()
    reservation.cancelReason = '到书保留超时'
    reservation.updatedAt = now.toISOString()
    const book = db.books.find((item) => item.id === reservation.bookId)
    if (book) {
      book.reservationCount = Math.max(0, (book.reservationCount || 0) - 1)
      book.availableCopies = Math.min(book.totalCopies, book.availableCopies + 1)
      promoteNextReservation(db, book.id)
    }
    changed = true
  }
  return changed
}

export function reservationSummary(db, readerId = null) {
  const list = readerId === null
    ? db.reservations
    : db.reservations.filter((item) => Number(item.readerId) === Number(readerId))
  const count = (status) => list.filter((item) => item.status === status).length
  return {
    pendingCount: count('PENDING'),
    readyCount: count('READY'),
    fulfilledCount: count('FULFILLED'),
    cancelledCount: count('CANCELLED'),
    expiredCount: count('EXPIRED'),
    activeCount: RESERVATION_ACTIVE_STATUSES.reduce((sum, status) => sum + count(status), 0),
    totalCount: list.length,
  }
}

export function reservationView(db, reservation) {
  const reader = db.readers.find((item) => item.id === Number(reservation.readerId))
  const book = db.books.find((item) => item.id === Number(reservation.bookId))
  const category = db.categories.find((item) => item.id === book?.categoryId)
  const activeQueue = sortReservationQueue(
    db.reservations.filter(
      (item) =>
        item.bookId === Number(reservation.bookId) &&
        isActiveReservation(item.status),
    ),
  )
  const queueIndex = activeQueue.findIndex((item) => item.id === reservation.id)
  const remainingMinutes =
    reservation.status === 'READY' && reservation.expireAt
      ? Math.max(dayjs(reservation.expireAt).diff(dayjs(), 'minute'), 0)
      : null

  return {
    ...reservation,
    statusLabel: RESERVATION_STATUS[reservation.status]?.label || reservation.status,
    readerName: reader?.name || reservation.readerName || '已注销读者',
    readerNo: reader?.studentNo || reservation.readerNo || '—',
    readerType: reader?.type || null,
    bookTitle: book?.title || reservation.bookTitle || '已下架图书',
    bookIsbn: book?.isbn || reservation.bookIsbn || '—',
    bookAuthor: book?.author || '—',
    categoryName: category?.name || '未分类',
    categoryColor: category?.color || '#6f7f79',
    availableCopies: book?.availableCopies || 0,
    reservationCount: book?.reservationCount || 0,
    queuePosition: queueIndex >= 0 ? queueIndex + 1 : null,
    aheadCount: queueIndex >= 0 ? queueIndex : null,
    remainingHours:
      remainingMinutes === null ? null : remainingMinutes === 0 ? 0 : Math.ceil(remainingMinutes / 60),
  }
}
