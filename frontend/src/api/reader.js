import http from './http'

export const getReaders = (params) => http.get('/readers', { params })
export const getReader = (id) => http.get(`/readers/${id}`)
export const createReader = (data) => http.post('/readers', data)
export const updateReader = (id, data) => http.put(`/readers/${id}`, data)
export const removeReader = (id) => http.delete(`/readers/${id}`)
export const getMyCard = () => http.get('/readers/me')
export const updateCardStatus = (id, cardStatus) =>
  http.put(`/readers/${id}/card`, { cardStatus })
