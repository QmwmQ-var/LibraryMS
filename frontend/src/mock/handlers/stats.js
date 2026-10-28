import { ok, requireAdmin } from '../utils'
import {
  buildBookRanking,
  buildCategoryDistribution,
  buildMonthlyTrend,
  buildOverdueStats,
  buildOverview,
  buildReaderRanking,
} from '../analytics'

export const statsRoutes = [
  {
    method: 'GET',
    path: '/stats/overview',
    handler: ({ db, query, user }) => {
      requireAdmin({ user })
      return ok(
        buildOverview(db, user, {
          startDate: query.startDate,
          endDate: query.endDate,
        }),
      )
    },
  },
  {
    method: 'GET',
    path: '/stats/trends',
    handler: ({ db, user }) => {
      requireAdmin({ user })
      const months = 8
      return ok({
        monthly: buildMonthlyTrend(db, months),
        categories: buildCategoryDistribution(db),
      })
    },
  },
  {
    method: 'GET',
    path: '/stats/ranking',
    handler: ({ db, query, user }) => {
      requireAdmin({ user })
      const limit = Math.min(Number(query.limit) || 8, 20)
      return ok({
        books: buildBookRanking(db, limit, query.startDate, query.endDate),
        readers: buildReaderRanking(db, limit),
      })
    },
  },
  {
    method: 'GET',
    path: '/stats/overdue',
    handler: ({ db, user }) => {
      requireAdmin({ user })
      return ok(buildOverdueStats(db, 6))
    },
  },
]
