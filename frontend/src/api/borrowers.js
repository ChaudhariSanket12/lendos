import apiClient from './apiClient'

const buildUrl = (path) => new URL(path, apiClient.defaults.baseURL).toString()

const logSuccess = (label, url, data) => {
  console.log(`[borrowersApi] ${label} success`, { url, data })
}

const logError = (label, url, error) => {
  console.error(`[borrowersApi] ${label} failed`, {
    url,
    status: error.response?.status,
    data: error.response?.data,
    message: error.message,
  })
}

export const borrowersApi = {
  list: async (status) => {
    const params = status ? { status } : {}
    const path = '/api/v1/borrowers'
    const url = status ? `${buildUrl(path)}?status=${status}` : buildUrl(path)
    console.log('[borrowersApi] list request', { url })

    try {
      const response = await apiClient.get(path, { params })
      logSuccess('list', url, response.data)
      return response.data
    } catch (error) {
      logError('list', url, error)
      throw error
    }
  },

  create: async (data) => {
    const path = '/api/v1/borrowers'
    const url = buildUrl(path)
    console.log('[borrowersApi] create request', { url, payload: data })

    try {
      const response = await apiClient.post(path, data)
      logSuccess('create', url, response.data)
      return response.data
    } catch (error) {
      logError('create', url, error)
      throw error
    }
  },

  getById: async (borrowerId) => {
    const path = `/api/v1/borrowers/${borrowerId}`
    const url = buildUrl(path)
    console.log('[borrowersApi] getById request', { url })

    try {
      const response = await apiClient.get(path)
      logSuccess('getById', url, response.data)
      return response.data
    } catch (error) {
      logError('getById', url, error)
      throw error
    }
  },

  updateStatus: async (borrowerId, status) => {
    const path = `/api/v1/borrowers/${borrowerId}/status`
    const url = buildUrl(path)
    console.log('[borrowersApi] updateStatus request', { url, payload: { status } })

    try {
      const response = await apiClient.patch(path, { status })
      logSuccess('updateStatus', url, response.data)
      return response.data
    } catch (error) {
      logError('updateStatus', url, error)
      throw error
    }
  },

  delete: async (borrowerId) => {
    const path = `/api/v1/borrowers/${borrowerId}`
    const url = buildUrl(path)
    console.log('[borrowersApi] delete request', { url })

    try {
      const response = await apiClient.delete(path)
      logSuccess('delete', url, response.data)
      return response.data
    } catch (error) {
      logError('delete', url, error)
      throw error
    }
  },
}
