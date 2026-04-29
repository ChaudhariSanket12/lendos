import { createContext, useContext, useState, useCallback } from 'react'
import { authApi } from '../api/identity'

const AuthContext = createContext(null)
const ADMIN_ROLES = ['ADMIN', 'CREDIT_OFFICER', 'AUDITOR']

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('user')
    return stored ? JSON.parse(stored) : null
  })

  const login = useCallback(async (email, password) => {
    const data = await authApi.login({ email, password })
    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    localStorage.setItem('user', JSON.stringify(data.user))
    setUser(data.user)
    return data
  }, [])

  const registerBorrower = useCallback(async (payload) => {
    const data = await authApi.registerBorrower(payload)
    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    localStorage.setItem('user', JSON.stringify(data.user))
    setUser(data.user)
    return data
  }, [])

  const logout = useCallback(async () => {
    try { await authApi.logout() } catch (_) {}
    localStorage.clear()
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider
      value={{
        user,
        login,
        registerBorrower,
        logout,
        isAuthenticated: !!user,
        isBorrower: user?.role === 'BORROWER',
        isAdminUser: ADMIN_ROLES.includes(user?.role),
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
