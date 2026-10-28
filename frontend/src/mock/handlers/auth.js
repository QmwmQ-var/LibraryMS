import { nextId, persistDatabase } from '../db'
import { MockError, nowIso, ok, publicUser } from '../utils'

const createToken = (user) =>
  `mock_${user.id}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`

export const authRoutes = [
  {
    method: 'POST',
    path: '/auth/login',
    public: true,
    handler: ({ db, body }) => {
      const username = String(body.username || '').trim()
      const password = String(body.password || '')
      if (!username || !password) {
        throw new MockError(400, '账号和密码不能为空')
      }

      const user = db.users.find((item) => item.username === username)
      if (!user) {
        throw new MockError(401, '账号不存在，请检查后重试')
      }
      if (user.status !== 'ACTIVE') {
        throw new MockError(403, '该账号已停用，请联系图书馆管理员')
      }

      const lockedUntil = user.lockedUntil ? new Date(user.lockedUntil) : null
      if (lockedUntil && lockedUntil.getTime() > Date.now()) {
        const minutes = Math.max(1, Math.ceil((lockedUntil.getTime() - Date.now()) / 60000))
        throw new MockError(423, `账号已锁定，请在 ${minutes} 分钟后再试`)
      }

      if (user.password !== password) {
        user.failedAttempts = (user.failedAttempts || 0) + 1
        if (user.failedAttempts >= 3) {
          user.lockedUntil = new Date(Date.now() + 15 * 60 * 1000).toISOString()
          user.failedAttempts = 0
          db.loginLogs.push({
            id: nextId(db.loginLogs),
            username,
            role: user.role,
            ip: '127.0.0.1',
            success: false,
            createdAt: nowIso(),
          })
          persistDatabase()
          throw new MockError(423, '连续 3 次密码错误，账号已锁定 15 分钟')
        }
        const remaining = 3 - user.failedAttempts
        persistDatabase()
        db.loginLogs.push({
          id: nextId(db.loginLogs),
          username,
          role: user.role,
          ip: '127.0.0.1',
          success: false,
          createdAt: nowIso(),
        })
        persistDatabase()
        throw new MockError(401, `密码错误，您还有 ${remaining} 次尝试机会`)
      }

      user.failedAttempts = 0
      user.lockedUntil = null
      db.loginLogs.push({
        id: nextId(db.loginLogs),
        username,
        role: user.role,
        ip: '127.0.0.1',
        success: true,
        createdAt: nowIso(),
      })
      persistDatabase()
      return ok({ token: createToken(user), user: publicUser(db, user) }, '登录成功')
    },
  },
  {
    method: 'GET',
    path: '/auth/me',
    handler: ({ db, user }) => ok(publicUser(db, user)),
  },
  {
    method: 'POST',
    path: '/auth/logout',
    handler: () => ok(null, '已安全退出'),
  },
  {
    method: 'PUT',
    path: '/auth/password',
    handler: ({ db, user, body }) => {
      const oldPassword = String(body.oldPassword || '')
      const newPassword = String(body.newPassword || '')
      if (oldPassword !== user.password) {
        throw new MockError(400, '原密码不正确')
      }
      if (newPassword.length < 6 || newPassword.length > 32) {
        throw new MockError(400, '新密码长度需为 6–32 位')
      }
      if (oldPassword === newPassword) {
        throw new MockError(400, '新密码不能与原密码相同')
      }
      user.password = newPassword
      persistDatabase()
      return ok(null, '密码修改成功，请保管好新密码')
    },
  },
]
