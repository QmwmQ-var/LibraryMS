import http from './http'

export const getBorrowRecords = (params) => http.get('/borrow-records', { params })
export const getMyBorrows = (params) => http.get('/borrow-records/my', { params })
export const borrowBook = (data) => http.post('/borrow-records', data)
export const returnBook = (id) => http.post(`/borrow-records/${id}/return`)
export const renewBook = (id) => http.post(`/borrow-records/${id}/renew`)
