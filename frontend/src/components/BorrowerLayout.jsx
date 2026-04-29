import { useMemo } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const navItems = [
  { label: 'Dashboard', path: '/borrower/dashboard' },
  { label: 'My Profile', path: '/borrower/my-profile' },
  { label: 'Apply for Loan', path: '/borrower/apply-for-loan' },
  { label: 'My Loans', path: '/borrower/my-loans' },
]

function isActivePath(currentPath, itemPath) {
  return currentPath === itemPath
}

export default function BorrowerLayout({ title, subtitle, children }) {
  const navigate = useNavigate()
  const location = useLocation()
  const { user, logout } = useAuth()

  const activePath = useMemo(() => location.pathname, [location.pathname])

  return (
    <div className="min-h-screen bg-gray-50 md:flex">
      <aside className="md:w-64 bg-white border-r border-gray-200">
        <div className="px-5 py-5 border-b border-gray-200">
          <h1 className="text-lg font-bold text-blue-700">LendOS</h1>
          <p className="text-xs text-gray-500 mt-1">Borrower Portal</p>
        </div>

        <nav className="p-4 space-y-2">
          {navItems.map((item) => (
            <button
              key={item.path}
              type="button"
              onClick={() => navigate(item.path)}
              className={`w-full text-left px-3 py-2 rounded-md text-sm ${
                isActivePath(activePath, item.path)
                  ? 'bg-blue-100 text-blue-700 font-medium'
                  : 'text-gray-700 hover:bg-gray-100'
              }`}
            >
              {item.label}
            </button>
          ))}

          <button
            type="button"
            onClick={logout}
            className="w-full text-left px-3 py-2 rounded-md text-sm text-red-600 hover:bg-red-50"
          >
            Logout
          </button>
        </nav>
      </aside>

      <main className="flex-1">
        <header className="bg-white border-b border-gray-200 px-6 py-4">
          <h2 className="text-xl font-semibold text-gray-800">{title}</h2>
          <p className="text-sm text-gray-500 mt-1">
            {subtitle || `Welcome back, ${user?.fullName || 'Borrower'}`}
          </p>
        </header>
        <div className="p-6">{children}</div>
      </main>
    </div>
  )
}
