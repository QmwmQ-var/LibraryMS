import { nextId, persistDatabase } from '../db'
import {
  MockError,
  activeRecords,
  includesKeyword,
  nowIso,
  ok,
  paginate,
  penaltyView,
  readerConfig,
  readerView,
  recordView,
  requireAdmin,
  requireReaderOrAdmin,
} from '../utils'
import { READER_TYPES } from '@/utils/constants'

const allowedTypes = Object.keys(READER_TYPES)
const allowedCardStatus = ['ACTIVE', 'LOST', 'CANCELLED']

const validate = (db, body, currentId = null) => {
  const studentNo = String(body.studentNo || '').trim()
  const name = String(body.name || '').trim()
  if (!/^\d{6,12}$/.test(studentNo)) {
    throw new MockError(400, '学号应为 6–12 位数字')
  }
  if (!name) throw new MockError(400, '姓名不能为空')
  if (!allowedTypes.includes(body.type)) throw new MockError(400, '请选择有效的读者类型')
  if (!allowedCardStatus.includes(body.cardStatus)) {
    throw new MockError(400, '请选择有效的借阅证状态')
  }
  const duplicateReader = db.readers.find(
    (reader) => reader.studentNo === studentNo && reader.id !== currentId,
  )
  if (duplicateReader) throw new MockError(409, '学号已存在，请检查')
  if (!currentId && db.users.some((user) => user.username === studentNo)) {
    throw new MockError(409, '该学号已绑定登录账号')
  }
  return {
    studentNo,
    name,
    gender: body.gender || '未设置',
    type: body.type,
    cardStatus: body.cardStatus,
    department: String(body.department || '').trim(),
    major: String(body.major || '').trim(),
    email: String(body.email || '').trim(),
    phone: String(body.phone || '').trim(),
  }
}

export const readerRoutes = [
  {
    method: 'GET',
    path: '/readers/me',
    handler: ({ db, user }) => {
      if (user.role !== 'READER') throw new MockError(400, '当前账号不是读者账号')
      const reader = db.readers.find((item) => item.id === user.readerId)
      if (!reader) throw new MockError(404, '借阅信息不存在')
      return ok(readerView(db, reader))
    },
  },
  {
    method: 'GET',
    path: '/readers',
    handler: ({ db, query, user }) => {
      requireAdmin({ user })
      let readers = db.readers.map((reader) => readerView(db, reader))
      readers = readers.filter((reader) => {
        if (query.type && reader.type !== query.type) return false
        if (query.cardStatus && reader.cardStatus !== query.cardStatus) return false
        if (query.hasOverdue === 'true' && reader.overdueCount === 0) return false
        return includesKeyword(
          [reader.name, reader.studentNo, reader.department, reader.major, reader.phone],
          query.keyword,
        )
      })
      readers.sort((a, b) => b.id - a.id)
      const all = db.readers.map((reader) => readerView(db, reader))
      const summary = {
        total: all.length,
        active: all.filter((reader) => reader.cardStatus === 'ACTIVE').length,
        lost: all.filter((reader) => reader.cardStatus === 'LOST').length,
        cancelled: all.filter((reader) => reader.cardStatus === 'CANCELLED').length,
        undergraduate: all.filter((reader) => reader.type === 'UNDERGRADUATE').length,
        graduate: all.filter((reader) => reader.type === 'GRADUATE').length,
        teacher: all.filter((reader) => reader.type === 'TEACHER').length,
        activeLoans: all.reduce((sum, reader) => sum + reader.activeBorrowCount, 0),
        overdue: all.reduce((sum, reader) => sum + reader.overdueCount, 0),
      }
      return ok({ ...paginate(readers, query.page, query.size), summary })
    },
  },
  {
    method: 'GET',
    path: '/readers/:id',
    handler: ({ db, params, user }) => {
      const reader = db.readers.find((item) => item.id === Number(params.id))
      if (!reader) throw new MockError(404, '读者不存在')
      requireReaderOrAdmin({ user }, reader.id)
      const active = db.records
        .filter(
          (record) =>
            record.readerId === reader.id && ['BORROWED', 'OVERDUE'].includes(record.status),
        )
        .map((record) => recordView(db, record))
      const history = db.records
        .filter((record) => record.readerId === reader.id)
        .sort((a, b) => (a.borrowDate < b.borrowDate ? 1 : -1))
        .slice(0, 6)
        .map((record) => recordView(db, record))
      const penalties = db.penalties
        .filter((penalty) => penalty.readerId === reader.id)
        .map((penalty) => penaltyView(db, penalty))
      return ok({
        ...readerView(db, reader),
        activeBorrows: active,
        recentHistory: history,
        penalties,
      })
    },
  },
  {
    method: 'POST',
    path: '/readers',
    handler: ({ db, body, user }) => {
      requireAdmin({ user })
      const payload = validate(db, body)
      const config = readerConfig({ type: payload.type })
      const reader = {
        ...payload,
        id: nextId(db.readers),
        cardNo: `LIB${payload.studentNo}`,
        cardIssuedAt: new Date().toISOString().slice(0, 10),
        cardExpiresAt: new Date(Date.now() + 4 * 365 * 24 * 60 * 60 * 1000)
          .toISOString()
          .slice(0, 10),
        createdAt: nowIso(),
      }
      db.readers.push(reader)
      db.users.push({
        id: nextId(db.users),
        username: payload.studentNo,
        password: 'reader123',
        name: payload.name,
        role: 'READER',
        title: config.label,
        email: payload.email,
        phone: payload.phone,
        readerId: reader.id,
        avatarColor: '#527b70',
        status: 'ACTIVE',
        failedAttempts: 0,
        lockedUntil: null,
        createdAt: nowIso(),
      })
      persistDatabase()
      return ok(readerView(db, reader), '读者新增成功，初始密码为 reader123')
    },
  },
  {
    method: 'PUT',
    path: '/readers/:id',
    handler: ({ db, params, body, user }) => {
      requireAdmin({ user })
      const reader = db.readers.find((item) => item.id === Number(params.id))
      if (!reader) throw new MockError(404, '读者不存在')
      const payload = validate(db, body, reader.id)
      if (payload.studentNo !== reader.studentNo) {
        throw new MockError(400, '学号作为登录账号不可修改')
      }
      Object.assign(reader, payload)
      const linkedUser = db.users.find((item) => item.readerId === reader.id)
      if (linkedUser) {
        Object.assign(linkedUser, {
          name: payload.name,
          email: payload.email,
          phone: payload.phone,
          title: READER_TYPES[payload.type].label,
        })
      }
      persistDatabase()
      return ok(readerView(db, reader), '读者信息已更新')
    },
  },
  {
    method: 'DELETE',
    path: '/readers/:id',
    handler: ({ db, params, user }) => {
      requireAdmin({ user })
      const reader = db.readers.find((item) => item.id === Number(params.id))
      if (!reader) throw new MockError(404, '读者不存在')
      const active = activeRecords(db, reader.id)
      if (active.length > 0) {
        throw new MockError(400, `该读者尚有 ${active.length} 本图书未归还，无法删除`)
      }
      const activeReservations = db.reservations.filter(
        (item) => item.readerId === reader.id && ['PENDING', 'READY'].includes(item.status),
      ).length
      if (activeReservations > 0) {
        throw new MockError(400, `该读者仍有 ${activeReservations} 个有效预约，请先取消`)
      }
      const index = db.readers.indexOf(reader)
      db.readers.splice(index, 1)
      const linkedUser = db.users.find((item) => item.readerId === reader.id)
      if (linkedUser) linkedUser.status = 'DISABLED'
      persistDatabase()
      return ok(null, '读者已删除，历史借阅记录已保留')
    },
  },
  {
    method: 'PUT',
    path: '/readers/:id/card',
    handler: ({ db, params, body, user }) => {
      requireAdmin({ user })
      const reader = db.readers.find((item) => item.id === Number(params.id))
      if (!reader) throw new MockError(404, '读者不存在')
      if (!allowedCardStatus.includes(body.cardStatus)) {
        throw new MockError(400, '借阅证状态不合法')
      }
      reader.cardStatus = body.cardStatus
      persistDatabase()
      return ok(readerView(db, reader), '借阅证状态已更新')
    },
  },
]
