import apiClient from './apiClient'

const buildUrl = (path) => new URL(path, apiClient.defaults.baseURL).toString()

export const authApi = {
  login: (credentials) =>
    apiClient.post('/api/v1/auth/login', credentials).then((r) => r.data),

  registerBorrower: async (data) => {
    const path = '/api/v1/auth/borrower/register'
    const url = buildUrl(path)
    console.log('[authApi] registerBorrower request', { url, payload: { ...data, password: '***' } })
    try {
      const response = await apiClient.post(path, data)
      console.log('[authApi] registerBorrower success', { url, data: response.data })
      return response.data
    } catch (error) {
      console.error('[authApi] registerBorrower failed', {
        url,
        status: error.response?.status,
        data: error.response?.data,
        message: error.message,
      })
      throw error
    }
  },

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

export const tenantApi = {
  getMyFirmCode: () => apiClient.get('/api/v1/tenants/me/firm-code').then((r) => r.data),
}
