import apiClient from './apiClient'

const buildUrl = (path) => new URL(path, apiClient.defaults.baseURL).toString()

const logSuccess = (label, url, data) => {
  console.log(`[loansApi] ${label} success`, { url, data })
}

const logError = (label, url, error) => {
  console.error(`[loansApi] ${label} failed`, {
    url,
    status: error.response?.status,
    data: error.response?.data,
    message: error.message,
  })
}

export const loansApi = {
  apply: async (payload) => {
    const path = '/api/v1/borrower/me/loan-application'
    const url = buildUrl(path)
    console.log('[loansApi] apply request', { url, payload })
    try {
      const response = await apiClient.post(path, payload)
      logSuccess('apply', url, response.data)
      return response.data
    } catch (error) {
      logError('apply', url, error)
      throw error
    }
  },

  getMyLoans: async () => {
    const path = '/api/v1/borrower/me/loans'
    const url = buildUrl(path)
    console.log('[loansApi] getMyLoans request', { url })
    try {
      const response = await apiClient.get(path)
      logSuccess('getMyLoans', url, response.data)
      return response.data
    } catch (error) {
      logError('getMyLoans', url, error)
      throw error
    }
  },

  getMyLoanById: async (loanId) => {
    const path = `/api/v1/borrower/me/loans/${loanId}`
    const url = buildUrl(path)
    console.log('[loansApi] getMyLoanById request', { url })
    try {
      const response = await apiClient.get(path)
      logSuccess('getMyLoanById', url, response.data)
      return response.data
    } catch (error) {
      logError('getMyLoanById', url, error)
      throw error
    }
  },

  listAdminLoans: async (status) => {
    const path = '/api/v1/loans'
    const params = status ? { status } : {}
    const query = status ? `?status=${status}` : ''
    const url = `${buildUrl(path)}${query}`
    console.log('[loansApi] listAdminLoans request', { url })
    try {
      const response = await apiClient.get(path, { params })
      logSuccess('listAdminLoans', url, response.data)
      return response.data
    } catch (error) {
      logError('listAdminLoans', url, error)
      throw error
    }
  },

  getAdminLoanById: async (loanId) => {
    const path = `/api/v1/loans/${loanId}`
    const url = buildUrl(path)
    console.log('[loansApi] getAdminLoanById request', { url })
    try {
      const response = await apiClient.get(path)
      logSuccess('getAdminLoanById', url, response.data)
      return response.data
    } catch (error) {
      logError('getAdminLoanById', url, error)
      throw error
    }
  },

  updateLoanStatus: async (loanId, payload) => {
    const path = `/api/v1/loans/${loanId}/status`
    const url = buildUrl(path)
    console.log('[loansApi] updateLoanStatus request', { url, payload })
    try {
      const response = await apiClient.patch(path, payload)
      logSuccess('updateLoanStatus', url, response.data)
      return response.data
    } catch (error) {
      logError('updateLoanStatus', url, error)
      throw error
    }
  },
}
