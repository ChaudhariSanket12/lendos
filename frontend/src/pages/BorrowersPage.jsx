import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { borrowersApi } from '../api/borrowers'

const statusOptions = [
  { label: 'All', value: '' },
  { label: 'Draft', value: 'DRAFT' },
  { label: 'Under Review', value: 'UNDER_REVIEW' },
  { label: 'Verified', value: 'VERIFIED' },
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Blacklisted', value: 'BLACKLISTED' },
]

const statusStyles = {
  DRAFT: 'bg-gray-100 text-gray-700',
  UNDER_REVIEW: 'bg-yellow-100 text-yellow-800',
  VERIFIED: 'bg-blue-100 text-blue-700',
  ACTIVE: 'bg-green-100 text-green-700',
  BLACKLISTED: 'bg-red-100 text-red-700',
}

function formatStatus(status) {
  if (!status) return 'UNKNOWN'
  return status.replace('_', ' ')
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleDateString()
}

function getErrorMessage(err, fallback) {
  const backendMessage = err.response?.data?.message
  const status = err.response?.status
  const generic = err.message
  if (backendMessage && status) return `${backendMessage} (HTTP ${status})`
  return backendMessage || generic || fallback
}

export default function BorrowersPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [borrowers, setBorrowers] = useState([])
  const [statusFilter, setStatusFilter] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [successMessage, setSuccessMessage] = useState(location.state?.message || null)

  useEffect(() => {
    if (location.state?.message) {
      navigate(location.pathname, { replace: true, state: {} })
    }
  }, [location.pathname, location.state, navigate])

  useEffect(() => {
    const loadBorrowers = async () => {
      setLoading(true)
      setError(null)
      try {
        const data = await borrowersApi.list(statusFilter || undefined)
        console.log('[BorrowersPage] Loaded borrowers', data)
        setBorrowers(data)
      } catch (err) {
        console.error('[BorrowersPage] Failed to load borrowers', err)
        setError(getErrorMessage(err, 'Failed to load borrowers.'))
      } finally {
        setLoading(false)
      }
    }

    loadBorrowers()
  }, [statusFilter])

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white shadow-sm px-6 py-4 flex items-center justify-between">
        <h1 className="text-lg font-bold text-blue-700">LendOS</h1>
        <button className="btn-secondary" onClick={() => navigate('/dashboard')}>
          Back to Dashboard
        </button>
      </nav>

      <div className="max-w-6xl mx-auto p-8">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-6">
          <div>
            <h2 className="text-2xl font-semibold text-gray-800">Borrowers</h2>
            <p className="text-sm text-gray-500 mt-1">Manage borrower profiles for your workspace.</p>
          </div>

          <div className="flex items-center gap-3">
            <select
              className="input w-48"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              {statusOptions.map((option) => (
                <option key={option.label} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>

            <button className="btn-primary" onClick={() => navigate('/borrowers/new')}>
              Add New Borrower
            </button>
          </div>
        </div>

        {successMessage && (
          <div className="alert-success mb-4 flex items-center justify-between">
            <span>{successMessage}</span>
            <button className="text-green-700 text-sm hover:underline" onClick={() => setSuccessMessage(null)}>
              Dismiss
            </button>
          </div>
        )}

        {error && <div className="alert-error mb-4">{error}</div>}

        <div className="card p-0 overflow-hidden">
          {loading ? (
            <div className="p-6 text-sm text-gray-600">Loading borrowers...</div>
          ) : borrowers.length === 0 ? (
            <div className="p-6 text-sm text-gray-600">No borrowers found for this filter.</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-50 border-b border-gray-200">
                  <tr>
                    <th className="text-left text-xs font-semibold text-gray-600 px-4 py-3">Name</th>
                    <th className="text-left text-xs font-semibold text-gray-600 px-4 py-3">Email</th>
                    <th className="text-left text-xs font-semibold text-gray-600 px-4 py-3">Phone</th>
                    <th className="text-left text-xs font-semibold text-gray-600 px-4 py-3">Status</th>
                    <th className="text-left text-xs font-semibold text-gray-600 px-4 py-3">Date Added</th>
                  </tr>
                </thead>
                <tbody>
                  {borrowers.map((borrower) => (
                    <tr
                      key={borrower.id}
                      onClick={() => navigate(`/borrowers/${borrower.id}`)}
                      className="border-b border-gray-100 hover:bg-gray-50 cursor-pointer"
                    >
                      <td className="px-4 py-3 text-sm text-gray-800">{borrower.fullName}</td>
                      <td className="px-4 py-3 text-sm text-gray-600">{borrower.email}</td>
                      <td className="px-4 py-3 text-sm text-gray-600">{borrower.phone || '-'}</td>
                      <td className="px-4 py-3 text-sm">
                        <span
                          className={`inline-flex px-2 py-1 rounded text-xs font-medium ${
                            statusStyles[borrower.status] || 'bg-gray-100 text-gray-700'
                          }`}
                        >
                          {formatStatus(borrower.status)}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-600">{formatDate(borrower.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
