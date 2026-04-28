import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import RegisterPage from './pages/RegisterPage'
import BorrowersPage from './pages/BorrowersPage'
import BorrowerCreatePage from './pages/BorrowerCreatePage'
import BorrowerDetailPage from './pages/BorrowerDetailPage'

// ── Protected route wrapper ───────────────────────────────────
function ProtectedRoute({ children }) {
  const { isAuthenticated } = useAuth()
  return isAuthenticated ? children : <Navigate to="/login" replace />
}

// ── Public route wrapper (redirect if already logged in) ──────
function PublicRoute({ children }) {
  const { isAuthenticated } = useAuth()
  return isAuthenticated ? <Navigate to="/dashboard" replace /> : children
}

// ── Routes ────────────────────────────────────────────────────
function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />

      <Route
        path="/login"
        element={
          <PublicRoute>
            <LoginPage />
          </PublicRoute>
        }
      />

      <Route
        path="/register"
        element={
          <PublicRoute>
            <RegisterPage />
          </PublicRoute>
        }
      />

      <Route
        path="/dashboard"
        element={
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/borrowers"
        element={
          <ProtectedRoute>
            <BorrowersPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/borrowers/new"
        element={
          <ProtectedRoute>
            <BorrowerCreatePage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/borrowers/:borrowerId"
        element={
          <ProtectedRoute>
            <BorrowerDetailPage />
          </ProtectedRoute>
        }
      />

      {/* Catch-all */}
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}

// ── Root App ──────────────────────────────────────────────────
export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  )
}
