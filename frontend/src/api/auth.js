import http from './http'

export const login = (data) => http.post('/auth/login', data)
export const fetchMe = () => http.get('/auth/me')
export const logout = () => http.post('/auth/logout')
export const changePassword = (data) => http.put('/auth/password', data)
