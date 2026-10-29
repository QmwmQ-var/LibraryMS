import { nextId, persistDatabase } from '../db'
import {
  MockError,
  activeRecords,
  bookView,
  includesKeyword,
  nowIso,
  ok,
  paginate,
  refreshOverdue,
  requireAdmin,
} from '../utils'

const parseBookPayload = (body) => {
  const totalCopies = Number(body.totalCopies)
  const price = Number(body.price)
  const publishYear = Number(body.publishYear)
  return {
    isbn: String(body.isbn || '').replace(/[-\s]/g, ''),
    title: String(body.title || '').trim(),
    author: String(body.author || '').trim(),
    publisher: String(body.publisher || '').trim(),
    categoryId: Number(body.categoryId),
    totalCopies,
    price: Number.isFinite(price) ? price : 0,
    publishYear: Number.isFinite(publishYear) ? publishYear : new Date().getFullYear(),
    location: String(body.location || '').trim(),
    description: String(body.description || '').trim(),
  }
}

const validateBook = (db, payload, currentId = null) => {
  if (!payload.title) throw new MockError(400, '书名不能为空')
  if (!payload.author) throw new MockError(400, '作者不能为空')
  if (!payload.isbn) throw new MockError(400, 'ISBN 不能为空')
  if (!/^(97[89])?\d{9}[\dX]$/i.test(payload.isbn)) {
    throw new MockError(400, 'ISBN 格式不正确，应为 10 位或 13 位数字')
  }
  if (!Number.isInteger(payload.totalCopies) || payload.totalCopies < 1) {
    throw new MockError(400, '馆藏总量至少为 1 本')
  }
  if (!db.categories.some((category) => category.id === payload.categoryId)) {
    throw new MockError(400, '请选择有效的图书分类')
  }
  const duplicate = db.books.find(
    (book) => book.isbn === payload.isbn && (currentId === null || book.id !== currentId),
  )
  if (duplicate) throw new MockError(409, 'ISBN 已存在，请检查')

  if (currentId !== null) {
    const activeCount = activeRecords(db).filter((record) => record.bookId === currentId).length
    const readyCount = db.reservations.filter(
      (item) => item.bookId === currentId && item.status === 'READY',
    ).length
    if (payload.totalCopies < activeCount + readyCount) {
      throw new MockError(
        400,
        `当前有 ${activeCount} 本在借、${readyCount} 本已到书留书，馆藏总量不能低于两者之和`,
      )
    }
  }
}

export const bookRoutes = [
  {
    method: 'GET',
    path: '/books',
    handler: ({ db, query }) => {
      refreshOverdue(db)
      const categoryId = query.categoryId ? Number(query.categoryId) : null
      const availability = query.availability || ''
      const sort = query.sort || 'recommended'
      let books = db.books.map((book) => bookView(db, book))

      books = books.filter((book) => {
        if (categoryId && book.categoryId !== categoryId) return false
        if (availability === 'AVAILABLE' && book.availableCopies <= 0) return false
        if (availability === 'UNAVAILABLE' && book.availableCopies > 0) return false
        return includesKeyword(
          [book.title, book.author, book.isbn, book.publisher, book.categoryName],
          query.keyword,
        )
      })

      const borrowCount = (bookId) =>
        db.records.filter((record) => record.bookId === bookId).length

      if (sort === 'newest') {
        books.sort((a, b) => b.publishYear - a.publishYear || b.id - a.id)
      } else if (sort === 'rating') {
        books.sort((a, b) => b.rating - a.rating)
      } else if (sort === 'popular') {
        books.sort((a, b) => borrowCount(b.id) - borrowCount(a.id))
      } else if (sort === 'title') {
        books.sort((a, b) => a.title.localeCompare(b.title, 'zh-Hans-CN'))
      }

      return ok(paginate(books, query.page, query.size))
    },
  },
  {
    method: 'GET',
    path: '/books/:id',
    handler: ({ db, params }) => {
      const book = db.books.find((item) => item.id === Number(params.id))
      if (!book) throw new MockError(404, '图书不存在或已下架')
      return ok(bookView(db, book))
    },
  },
  {
    method: 'POST',
    path: '/books',
    handler: ({ db, body, user }) => {
      requireAdmin({ user })
      const payload = parseBookPayload(body)
      validateBook(db, payload)
      const book = {
        ...payload,
        id: nextId(db.books),
        availableCopies: payload.totalCopies,
        reservationCount: 0,
        rating: 4.5,
        createdAt: nowIso(),
      }
      db.books.push(book)
      persistDatabase()
      return ok(bookView(db, book), '图书新增成功')
    },
  },
  {
    method: 'PUT',
    path: '/books/:id',
    handler: ({ db, params, body, user }) => {
      requireAdmin({ user })
      const book = db.books.find((item) => item.id === Number(params.id))
      if (!book) throw new MockError(404, '图书不存在')
      const payload = parseBookPayload(body)
      validateBook(db, payload, book.id)
      const activeCount = activeRecords(db).filter((record) => record.bookId === book.id).length
      const readyCount = db.reservations.filter(
        (item) => item.bookId === book.id && item.status === 'READY',
      ).length
      Object.assign(book, payload, {
        availableCopies: payload.totalCopies - activeCount - readyCount,
      })
      persistDatabase()
      return ok(bookView(db, book), '图书信息已更新')
    },
  },
  {
    method: 'DELETE',
    path: '/books/:id',
    handler: ({ db, params, user }) => {
      requireAdmin({ user })
      const book = db.books.find((item) => item.id === Number(params.id))
      if (!book) throw new MockError(404, '图书不存在')
      const activeCount = activeRecords(db).filter((record) => record.bookId === book.id).length
      if (activeCount > 0) {
        throw new MockError(400, `该书仍有 ${activeCount} 本未归还，无法删除`)
      }
      const activeReservations = db.reservations.filter(
        (item) => item.bookId === book.id && ['PENDING', 'READY'].includes(item.status),
      ).length
      if (activeReservations > 0) {
        throw new MockError(400, `该书仍有 ${activeReservations} 个有效预约，请先处理`)
      }
      const index = db.books.findIndex((item) => item.id === book.id)
      db.books.splice(index, 1)
      persistDatabase()
      return ok(null, '图书已删除')
    },
  },
  {
    method: 'GET',
    path: '/categories',
    handler: ({ db }) => {
      const categories = db.categories.map((category) => {
        const books = db.books.filter((book) => book.categoryId === category.id)
        return {
          ...category,
          bookCount: books.length,
          copyCount: books.reduce((sum, book) => sum + book.totalCopies, 0),
          borrowCount: db.records.filter((record) =>
            books.some((book) => book.id === record.bookId),
          ).length,
        }
      })
      return ok(categories)
    },
  },
  {
    method: 'POST',
    path: '/categories',
    handler: ({ db, body, user }) => {
      requireAdmin({ user })
      const name = String(body.name || '').trim()
      const code = String(body.code || '').trim().toUpperCase()
      if (!name) throw new MockError(400, '分类名称不能为空')
      if (!code) throw new MockError(400, '分类代码不能为空')
      if (db.categories.some((item) => item.name === name)) {
        throw new MockError(409, '该分类名称已存在')
      }
      if (db.categories.some((item) => item.code === code)) {
        throw new MockError(409, '该分类代码已存在')
      }
      const category = {
        id: nextId(db.categories),
        name,
        code,
        color: body.color || '#2e7364',
        description: String(body.description || '').trim(),
      }
      db.categories.push(category)
      persistDatabase()
      return ok(category, '分类创建成功')
    },
  },
  {
    method: 'PUT',
    path: '/categories/:id',
    handler: ({ db, params, body, user }) => {
      requireAdmin({ user })
      const category = db.categories.find((item) => item.id === Number(params.id))
      if (!category) throw new MockError(404, '分类不存在')
      const name = String(body.name || '').trim()
      const code = String(body.code || '').trim().toUpperCase()
      if (!name || !code) throw new MockError(400, '分类名称与代码不能为空')
      if (db.categories.some((item) => item.id !== category.id && item.name === name)) {
        throw new MockError(409, '该分类名称已存在')
      }
      if (db.categories.some((item) => item.id !== category.id && item.code === code)) {
        throw new MockError(409, '该分类代码已存在')
      }
      Object.assign(category, {
        name,
        code,
        color: body.color || category.color,
        description: String(body.description || '').trim(),
      })
      persistDatabase()
      return ok(category, '分类信息已更新')
    },
  },
  {
    method: 'DELETE',
    path: '/categories/:id',
    handler: ({ db, params, user }) => {
      requireAdmin({ user })
      const category = db.categories.find((item) => item.id === Number(params.id))
      if (!category) throw new MockError(404, '分类不存在')
      if (db.books.some((book) => book.categoryId === category.id)) {
        throw new MockError(400, '该分类下仍有图书，无法删除')
      }
      db.categories.splice(db.categories.indexOf(category), 1)
      persistDatabase()
      return ok(null, '分类已删除')
    },
  },
]
