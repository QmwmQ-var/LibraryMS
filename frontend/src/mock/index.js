import { getDatabase, persistDatabase } from './db'
import { MockError } from './utils'
import { authRoutes } from './handlers/auth'
import { bookRoutes } from './handlers/books'
import { readerRoutes } from './handlers/readers'
import { borrowRoutes } from './handlers/borrow'
import { penaltyRoutes } from './handlers/penalties'
import { statsRoutes } from './handlers/stats'
import { dashboardRoutes } from './handlers/dashboard'

/**
 * 内置 Mock API。
 *
 * 通过 Axios 自定义 adapter 接管所有 /api 请求，业务页面仍然只依赖 api/*.js，
 * 后续切换 Spring Boot 后端时无需改动任何页面代码。
 */

const routeModules = [
  ...authRoutes,
  ...bookRoutes,
  ...readerRoutes,
  ...borrowRoutes,
  ...penaltyRoutes,
  ...statsRoutes,
  ...dashboardRoutes,
]

function compilePath(pattern) {
  const keys = []
  const regexSource = pattern.replace(/:([A-Za-z0-9_]+)/g, (_, key) => {
    keys.push(key)
    return '([^/]+)'
  })
  return {
    regex: new RegExp(`^${regexSource}/?$`),
    keys,
  }
}

const routes = routeModules.map((route) => ({
  ...route,
  ...compilePath(route.path),
}))

const parseBody = (data) => {
  if (!data) return {}
  if (typeof data === 'string') {
    try {
      return JSON.parse(data)
    } catch {
      return {}
    }
  }
  if (typeof FormData !== 'undefined' && data instanceof FormData) {
    return Object.fromEntries(data.entries())
  }
  return data
}

const getHeader = (config, name) => {
  const headers = config.headers
  if (!headers) return null
  if (typeof headers.get === 'function') return headers.get(name)
  return headers[name] || headers[name.toLowerCase()] || null
}

function authenticate(db, config) {
  const authorization = getHeader(config, 'Authorization') || ''
  const token = String(authorization).replace(/^Bearer\s+/i, '')
  if (!token.startsWith('mock_')) return null
  const [, rawId] = token.split('_')
  const user = db.users.find((item) => item.id === Number(rawId))
  if (!user || user.status !== 'ACTIVE') return null
  return user
}

const makeResponse = (config, payload) => ({
  data: payload,
  status: 200,
  statusText: 'OK',
  headers: { 'content-type': 'application/json; charset=utf-8' },
  config,
  request: { __mock: true },
})

const delay = () => new Promise((resolve) => window.setTimeout(resolve, 170 + Math.random() * 180))

async function handleRequest(config) {
  const db = getDatabase()
  const method = String(config.method || 'get').toUpperCase()
  const path = String(config.url || '')
    .replace(/^\/api/, '')
    .split('?')[0]
  const match = routes
    .map((route) => {
      // 同一路径可能同时存在 GET 与 POST/PUT/DELETE，必须同时匹配请求方法。
      if (route.method !== method) return null
      const result = route.regex.exec(path)
      return result ? { route, result } : null
    })
    .find(Boolean)

  if (!match) {
    return { code: 404, message: `Mock API 未实现：${method} ${path}`, data: null }
  }

  const { route, result } = match
  const params = {}
  route.keys.forEach((key, index) => {
    params[key] = decodeURIComponent(result[index + 1])
  })

  const user = authenticate(db, config)
  if (!route.public && !user) {
    return { code: 401, message: '登录状态已失效，请重新登录', data: null }
  }

  const query = config.params || {}
  const body = parseBody(config.data)

  try {
    return await route.handler({ db, params, query, body, user, config })
  } catch (error) {
    if (error instanceof MockError) {
      return { code: error.code, message: error.message, data: error.data }
    }
    console.error('[LibraryMS Mock]', error)
    return { code: 500, message: '演示服务出现异常，请重试', data: null }
  } finally {
    persistDatabase()
  }
}

export function installMock(instance) {
  instance.defaults.adapter = async (config) => {
    await delay()
    const payload = await handleRequest(config)
    return makeResponse(config, payload)
  }
  if (import.meta.env.DEV) {
    console.info(
      '%c LibraryMS ',
      'background:#17443f;color:#f4eadb;border-radius:3px;padding:2px 4px',
      'Mock API 已启用（修改 .env 中的 VITE_USE_MOCK 可切换真实后端）',
    )
  }
}
