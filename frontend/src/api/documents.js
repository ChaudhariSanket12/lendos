import apiClient from './apiClient'

const buildUrl = (path) => new URL(path, apiClient.defaults.baseURL).toString()

const logSuccess = (label, url, data) => {
  console.log(`[documentsApi] ${label} success`, { url, data })
}

const logError = (label, url, error) => {
  console.error(`[documentsApi] ${label} failed`, {
    url,
    status: error.response?.status,
    data: error.response?.data,
    message: error.message,
  })
}

export const documentsApi = {
  upload: async (formData) => {
    const path = '/api/v1/borrower/me/documents'
    const url = buildUrl(path)
    console.log('[documentsApi] upload request', { url })
    try {
      const response = await apiClient.post(path, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      logSuccess('upload', url, response.data)
      return response.data
    } catch (error) {
      logError('upload', url, error)
      throw error
    }
  },

  getAll: async () => {
    const path = '/api/v1/borrower/me/documents'
    const url = buildUrl(path)
    console.log('[documentsApi] getAll request', { url })
    try {
      const response = await apiClient.get(path)
      logSuccess('getAll', url, response.data)
      return response.data
    } catch (error) {
      logError('getAll', url, error)
      throw error
    }
  },

  getByType: async (type) => {
    const path = `/api/v1/borrower/me/documents/${type}`
    const url = buildUrl(path)
    console.log('[documentsApi] getByType request', { url })
    try {
      const response = await apiClient.get(path)
      logSuccess('getByType', url, response.data)
      return response.data
    } catch (error) {
      logError('getByType', url, error)
      throw error
    }
  },

  delete: async (id) => {
    const path = `/api/v1/borrower/me/documents/${id}`
    const url = buildUrl(path)
    console.log('[documentsApi] delete request', { url })
    try {
      const response = await apiClient.delete(path)
      logSuccess('delete', url, response.data)
      return response.data
    } catch (error) {
      logError('delete', url, error)
      throw error
    }
  },

  verifyDocument: async (documentId) => {
    const path = `/api/v1/admin/documents/${documentId}/verify`
    const url = buildUrl(path)
    console.log('[documentsApi] verifyDocument request', { url })
    try {
      const response = await apiClient.post(path)
      logSuccess('verifyDocument', url, response.data)
      return response.data
    } catch (error) {
      logError('verifyDocument', url, error)
      throw error
    }
  },
}
