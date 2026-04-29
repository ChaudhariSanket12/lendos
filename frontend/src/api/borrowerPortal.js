import apiClient from './apiClient'

const buildUrl = (path) => new URL(path, apiClient.defaults.baseURL).toString()

const logSuccess = (label, url, data) => {
  console.log(`[borrowerPortalApi] ${label} success`, { url, data })
}

const logError = (label, url, error) => {
  console.error(`[borrowerPortalApi] ${label} failed`, {
    url,
    status: error.response?.status,
    data: error.response?.data,
    message: error.message,
  })
}

export const borrowerPortalApi = {
  me: async () => {
    const path = '/api/v1/borrower/me'
    const url = buildUrl(path)
    console.log('[borrowerPortalApi] me request', { url })
    try {
      const response = await apiClient.get(path)
      logSuccess('me', url, response.data)
      return response.data
    } catch (error) {
      logError('me', url, error)
      throw error
    }
  },

  getProfile: async () => {
    const path = '/api/v1/borrower/me/profile'
    const url = buildUrl(path)
    console.log('[borrowerPortalApi] getProfile request', { url })
    try {
      const response = await apiClient.get(path)
      logSuccess('getProfile', url, response.data)
      return response.data
    } catch (error) {
      logError('getProfile', url, error)
      throw error
    }
  },

  updateProfile: async (payload) => {
    const path = '/api/v1/borrower/me/profile'
    const url = buildUrl(path)
    console.log('[borrowerPortalApi] updateProfile request', { url, payload })
    try {
      const response = await apiClient.put(path, payload)
      logSuccess('updateProfile', url, response.data)
      return response.data
    } catch (error) {
      logError('updateProfile', url, error)
      throw error
    }
  },

  completeProfile: async (payload) => {
    const path = '/api/v1/borrower/me/complete-profile'
    const url = buildUrl(path)
    console.log('[borrowerPortalApi] completeProfile request', { url, payload })
    try {
      const response = await apiClient.put(path, payload)
      logSuccess('completeProfile', url, response.data)
      return response.data
    } catch (error) {
      logError('completeProfile', url, error)
      throw error
    }
  },
}
