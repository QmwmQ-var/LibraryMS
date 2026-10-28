import http from './http'

export const getPenalties = (params) => http.get('/penalties', { params })
export const getMyPenalties = (params) => http.get('/penalties/my', { params })
export const payPenalty = (id) => http.post(`/penalties/${id}/pay`)
export const getPenaltySummary = () => http.get('/penalties/summary')
