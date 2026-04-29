import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { useNavigate } from 'react-router-dom'
import { tenantApi } from '../api/identity'

export default function DashboardPage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [firmCode, setFirmCode] = useState(null)
  const [copyStatus, setCopyStatus] = useState('')

  useEffect(() => {
    const loadFirmCode = async () => {
      if (user?.role !== 'ADMIN') return
      try {
        const data = await tenantApi.getMyFirmCode()
        console.log('[DashboardPage] Loaded firm code', data)
        setFirmCode(data.firmCode)
      } catch (err) {
        console.error('[DashboardPage] Failed to load firm code', err)
      }
    }
    loadFirmCode()
  }, [user?.role])

  const copyFirmCode = async () => {
    if (!firmCode) return
    try {
      await navigator.clipboard.writeText(firmCode)
      setCopyStatus('Copied')
      setTimeout(() => setCopyStatus(''), 1500)
    } catch (err) {
      console.error('[DashboardPage] Failed to copy firm code', err)
      setCopyStatus('Copy failed')
    }
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white shadow-sm px-6 py-4 flex items-center justify-between">
        <h1 className="text-lg font-bold text-blue-700">LendOS</h1>
        <div className="flex items-center gap-4">
          <span className="text-sm text-gray-600">{user?.fullName} ({user?.role})</span>
          <button
            onClick={logout}
            className="text-sm text-red-600 hover:underline"
          >
            Logout
          </button>
        </div>
      </nav>

      <div className="max-w-5xl mx-auto p-8">
        <h2 className="text-2xl font-semibold text-gray-800 mb-2">
          Welcome, {user?.fullName}
        </h2>
        <p className="text-gray-500 text-sm mb-8">
          Tenant: <span className="font-medium text-gray-700">{user?.tenantName}</span>
          &nbsp;·&nbsp; Role: <span className="font-medium text-gray-700">{user?.role}</span>
        </p>

        {user?.role === 'ADMIN' && (
          <div className="card mb-6">
            <h3 className="text-lg font-semibold text-gray-800">Your Firm Code: {firmCode || 'Loading...'}</h3>
            <p className="text-sm text-gray-500 mt-1">
              Share this code with borrowers so they can register under your firm.
            </p>
            <button className="btn-secondary mt-3" onClick={copyFirmCode} disabled={!firmCode}>
              {copyStatus || 'Copy'}
            </button>
          </div>
        )}

        {/* Module cards — expand as modules are built */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {[
            { title: 'Borrowers', desc: 'Manage borrower profiles', route: '/borrowers' },
            { title: 'Loans', desc: 'Loan lifecycle management', color: 'green' },
            { title: 'Payments', desc: 'EMI schedule & payments', color: 'purple' },
            { title: 'Ledger', desc: 'Double-entry accounting', color: 'orange' },
            { title: 'Risk Engine', desc: 'Credit risk assessment', color: 'red' },
            { title: 'Reports', desc: 'Portfolio analytics', color: 'gray' },
          ].map((card) => (
            <div
              key={card.title}
              onClick={() => card.route && navigate(card.route)}
              className="bg-white rounded-lg border border-gray-200 p-5 cursor-pointer hover:shadow-md transition-shadow"
            >
              <h3 className="font-semibold text-gray-800">{card.title}</h3>
              <p className="text-sm text-gray-500 mt-1">{card.desc}</p>
              {card.route ? (
                <span className="inline-block mt-3 text-xs bg-green-100 text-green-700 px-2 py-1 rounded">
                  Open module
                </span>
              ) : (
                <span className="inline-block mt-3 text-xs bg-gray-100 text-gray-600 px-2 py-1 rounded">
                  Coming soon
                </span>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
