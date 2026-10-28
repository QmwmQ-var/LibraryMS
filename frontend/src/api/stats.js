import http from './http'

export const getOverview = (params) => http.get('/stats/overview', { params })
export const getTrends = (params) => http.get('/stats/trends', { params })
export const getRanking = (params) => http.get('/stats/ranking', { params })
export const getOverdueStats = (params) => http.get('/stats/overdue', { params })
export const getDashboard = () => http.get('/dashboard')
export const getNotifications = () => http.get('/notifications')
