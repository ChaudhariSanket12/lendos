import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import RegisterPage from './pages/RegisterPage'
import TenantRegisterPage from './pages/TenantRegisterPage'
import BorrowersPage from './pages/BorrowersPage'
import BorrowerCreatePage from './pages/BorrowerCreatePage'
import BorrowerDetailPage from './pages/BorrowerDetailPage'
import BorrowerDashboardPage from './pages/BorrowerDashboardPage'
import BorrowerCompleteProfilePage from './pages/BorrowerCompleteProfilePage'
import BorrowerMyProfilePage from './pages/BorrowerMyProfilePage'
import BorrowerMyLoansPage from './pages/BorrowerMyLoansPage'
import BorrowerApplyForLoanPage from './pages/BorrowerApplyForLoanPage'

const ADMIN_ROLES = ['ADMIN', 'CREDIT_OFFICER', 'AUDITOR']

function getHomeRouteByRole(role) {
  return role === 'BORROWER' ? '/borrower/dashboard' : '/dashboard'
}

// ── Protected route wrapper ───────────────────────────────────
function ProtectedRoute({ children, allowedRoles }) {
  const { isAuthenticated, user } = useAuth()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  if (allowedRoles && !allowedRoles.includes(user?.role)) {
    return <Navigate to={getHomeRouteByRole(user?.role)} replace />
  }
  return children
}

// ── Public route wrapper (redirect if already logged in) ──────
function PublicRoute({ children }) {
  const { isAuthenticated, user } = useAuth()
  return isAuthenticated ? <Navigate to={getHomeRouteByRole(user?.role)} replace /> : children
}

function HomeRedirect() {
  const { isAuthenticated, user } = useAuth()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  return <Navigate to={getHomeRouteByRole(user?.role)} replace />
}

// ── Routes ────────────────────────────────────────────────────
function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<HomeRedirect />} />

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
        path="/register-tenant"
        element={
          <PublicRoute>
            <TenantRegisterPage />
          </PublicRoute>
        }
      />

      <Route
        path="/dashboard"
        element={
          <ProtectedRoute allowedRoles={ADMIN_ROLES}>
            <DashboardPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/borrowers"
        element={
          <ProtectedRoute allowedRoles={ADMIN_ROLES}>
            <BorrowersPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/borrowers/new"
        element={
          <ProtectedRoute allowedRoles={ADMIN_ROLES}>
            <BorrowerCreatePage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/borrowers/:borrowerId"
        element={
          <ProtectedRoute allowedRoles={ADMIN_ROLES}>
            <BorrowerDetailPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/borrower/dashboard"
        element={
          <ProtectedRoute allowedRoles={['BORROWER']}>
            <BorrowerDashboardPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/borrower/complete-profile"
        element={
          <ProtectedRoute allowedRoles={['BORROWER']}>
            <BorrowerCompleteProfilePage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/borrower/my-profile"
        element={
          <ProtectedRoute allowedRoles={['BORROWER']}>
            <BorrowerMyProfilePage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/borrower/my-loans"
        element={
          <ProtectedRoute allowedRoles={['BORROWER']}>
            <BorrowerMyLoansPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/borrower/apply-for-loan"
        element={
          <ProtectedRoute allowedRoles={['BORROWER']}>
            <BorrowerApplyForLoanPage />
          </ProtectedRoute>
        }
      />

      {/* Catch-all */}
      <Route path="*" element={<HomeRedirect />} />
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
