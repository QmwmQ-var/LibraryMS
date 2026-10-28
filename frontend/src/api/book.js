import http from './http'

export const getBooks = (params) => http.get('/books', { params })
export const getBook = (id) => http.get(`/books/${id}`)
export const createBook = (data) => http.post('/books', data)
export const updateBook = (id, data) => http.put(`/books/${id}`, data)
export const removeBook = (id) => http.delete(`/books/${id}`)

export const getCategories = (params) => http.get('/categories', { params })
export const createCategory = (data) => http.post('/categories', data)
export const updateCategory = (id, data) => http.put(`/categories/${id}`, data)
export const removeCategory = (id) => http.delete(`/categories/${id}`)
