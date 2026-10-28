import dayjs from 'dayjs'
import { persistDatabase } from '../db'
import {
  MockError,
  includesKeyword,
  nowIso,
  ok,
  paginate,
  penaltyView,
  requireAdmin,
  requireReaderOrAdmin,
} from '../utils'

const buildSummary = (penalties) => {
  const unpaid = penalties.filter((item) => item.status === 'UNPAID')
  const paidThisMonth = penalties.filter(
    (item) =>
      item.status === 'PAID' && item.paidAt && dayjs(item.paidAt).isSame(dayjs(), 'month'),
  )
  return {
    pendingCount: unpaid.length,
    pendingAmount: Number(unpaid.reduce((sum, item) => sum + item.amount, 0).toFixed(2)),
    paidThisMonth: Number(paidThisMonth.reduce((sum, item) => sum + item.amount, 0).toFixed(2)),
    paidThisMonthCount: paidThisMonth.length,
    collectedTotal: Number(
      penalties
        .filter((item) => item.status === 'PAID')
        .reduce((sum, item) => sum + item.amount, 0)
        .toFixed(2),
    ),
    rate: 0.5,
  }
}

const filterPenalties = (db, penalties, query) =>
  penalties
    .map((penalty) => penaltyView(db, penalty))
    .filter((penalty) => {
      if (query.status && query.status !== 'ALL' && penalty.status !== query.status) return false
      return includesKeyword(
        [penalty.readerName, penalty.readerNo, penalty.bookTitle, penalty.bookIsbn],
        query.keyword,
      )
    })
    .sort((a, b) => {
      if (a.status !== b.status) return a.status === 'UNPAID' ? -1 : 1
      return String(b.createdAt).localeCompare(String(a.createdAt))
    })

export const penaltyRoutes = [
  {
    method: 'GET',
    path: '/penalties/summary',
    handler: ({ db, user }) => {
      const penalties =
        user.role === 'ADMIN'
          ? db.penalties
          : db.penalties.filter((item) => Number(item.readerId) === Number(user.readerId))
      return ok(buildSummary(penalties))
    },
  },
  {
    method: 'GET',
    path: '/penalties/my',
    handler: ({ db, query, user }) => {
      if (user.role !== 'READER') throw new MockError(400, '当前账号不是读者账号')
      const own = db.penalties.filter(
        (item) => Number(item.readerId) === Number(user.readerId),
      )
      const records = filterPenalties(db, own, query)
      return ok({
        ...paginate(records, query.page, query.size),
        summary: buildSummary(own),
      })
    },
  },
  {
    method: 'GET',
    path: '/penalties',
    handler: ({ db, query, user }) => {
      requireAdmin({ user })
      const records = filterPenalties(db, db.penalties, query)
      return ok({
        ...paginate(records, query.page, query.size),
        summary: buildSummary(db.penalties),
      })
    },
  },
  {
    method: 'POST',
    path: '/penalties/:id/pay',
    handler: ({ db, params, user }) => {
      const penalty = db.penalties.find((item) => item.id === Number(params.id))
      if (!penalty) throw new MockError(404, '罚款记录不存在')
      requireReaderOrAdmin({ user }, penalty.readerId)
      if (penalty.status === 'PAID') throw new MockError(400, '该罚款已缴纳，请勿重复操作')
      penalty.status = 'PAID'
      penalty.paidAt = nowIso()
      penalty.paidBy = user.id
      penalty.paymentChannel = user.role === 'ADMIN' ? 'CASH' : 'ONLINE'
      persistDatabase()
      return ok(
        penaltyView(db, penalty),
        user.role === 'ADMIN' ? '已确认缴纳，罚款状态已更新' : '缴纳成功，感谢您及时处理',
      )
    },
  },
]
