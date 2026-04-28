import apiClient from './apiClient'

export const authApi = {
  login: (credentials) =>
    apiClient.post('/api/v1/auth/login', credentials).then((r) => r.data),

  registerTenant: (data) =>
    apiClient.post('/api/v1/auth/register', data).then((r) => r.data),

  refresh: (refreshToken) =>
    apiClient.post('/api/v1/auth/refresh', { refreshToken }).then((r) => r.data),

  logout: () =>
    apiClient.post('/api/v1/auth/logout').then((r) => r.data),
}

export const userApi = {
  me: () => apiClient.get('/api/v1/users/me').then((r) => r.data),
  list: () => apiClient.get('/api/v1/users').then((r) => r.data),
  create: (data) => apiClient.post('/api/v1/users', data).then((r) => r.data),
  updateStatus: (userId, status) =>
    apiClient.patch(`/api/v1/users/${userId}/status`, { status }).then((r) => r.data),
}
