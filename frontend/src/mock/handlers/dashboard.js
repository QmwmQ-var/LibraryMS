import dayjs from 'dayjs'
import {
  activeRecords,
  bookView,
  ok,
  readerConfig,
  recordView,
  refreshOverdue,
  today,
  unpaidPenalties,
} from '../utils'
import {
  buildBookRanking,
  buildCategoryDistribution,
  buildMonthlyTrend,
  buildNotifications,
  buildOverview,
  buildRecentActivities,
} from '../analytics'

export const dashboardRoutes = [
  {
    method: 'GET',
    path: '/dashboard',
    handler: ({ db, user }) => {
      refreshOverdue(db)
      const overview = buildOverview(db, user)
      const announcements = db.announcements.slice(0, 3)

      if (user.role === 'READER') {
        const reader = db.readers.find((item) => item.id === user.readerId)
        const active = activeRecords(db, user.readerId)
          .map((record) => recordView(db, record))
          .sort((a, b) => a.dueDate.localeCompare(b.dueDate))
        const history = db.records
          .filter(
            (record) =>
              record.readerId === user.readerId && ['RETURNED', 'BORROWED', 'OVERDUE'].includes(record.status),
          )
          .sort((a, b) => b.borrowDate.localeCompare(a.borrowDate))
          .slice(0, 4)
          .map((record) => recordView(db, record))
        const recommendations = buildBookRanking(db, 8)
          .filter(
            (book) =>
              book.availableCopies > 0 &&
              !activeRecords(db, user.readerId).some((record) => record.bookId === book.id),
          )
          .slice(0, 4)
        const unpaid = unpaidPenalties(db, user.readerId)
        return ok({
          role: 'READER',
          reader: reader
            ? {
                ...reader,
                typeLabel: readerConfig(reader).label,
                maxBorrow: readerConfig(reader).maxBorrow,
                borrowDays: readerConfig(reader).borrowDays,
              }
            : null,
          overview,
          activeLoans: active,
          recentHistory: history,
          recommendations,
          unpaidPenalties: unpaid.map((penalty) => ({
            ...penalty,
            bookTitle: db.books.find((book) => book.id === penalty.bookId)?.title || '—',
          })),
          activities: buildRecentActivities(db, user, 5),
          announcements,
          trend: buildMonthlyTrend(db, 6),
        })
      }

      const active = activeRecords(db)
        .map((record) => recordView(db, record))
        .sort((a, b) => a.dueDate.localeCompare(b.dueDate))
      const dueSoon = active.filter((record) => {
        const diff = dayjs(record.dueDate).startOf('day').diff(today(), 'day')
        return diff >= 0 && diff <= 3
      })
      const lowStock = db.books
        .filter((book) => book.availableCopies <= 1)
        .slice(0, 5)
        .map((book) => bookView(db, book))

      return ok({
        role: 'ADMIN',
        overview,
        trend: buildMonthlyTrend(db, 8),
        categoryDistribution: buildCategoryDistribution(db).filter((item) => item.value > 0),
        popularBooks: buildBookRanking(db, 5),
        activeLoans: active.slice(0, 6),
        dueSoon,
        lowStock,
        activities: buildRecentActivities(db, user, 6),
        announcements,
      })
    },
  },
  {
    method: 'GET',
    path: '/notifications',
    handler: ({ db, user }) => {
      const items = buildNotifications(db, user)
      return ok({ items, unreadCount: items.length })
    },
  },
]
