import http from './http'

export const getReservations = (params) => http.get('/reservations', { params })
export const getMyReservations = (params) => http.get('/reservations/my', { params })
export const getReservationSummary = () => http.get('/reservations/summary')
export const createReservation = (bookId) => http.post('/reservations', { bookId })
export const cancelReservation = (id) => http.post(`/reservations/${id}/cancel`)
