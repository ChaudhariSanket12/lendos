import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { borrowersApi } from '../api/borrowers'

const statusStyles = {
  DRAFT: 'bg-gray-100 text-gray-700',
  UNDER_REVIEW: 'bg-yellow-100 text-yellow-800',
  VERIFIED: 'bg-blue-100 text-blue-700',
  ACTIVE: 'bg-green-100 text-green-700',
  BLACKLISTED: 'bg-red-100 text-red-700',
}

const actions = [
  { label: 'Send for Review', to: 'UNDER_REVIEW', from: ['DRAFT'] },
  { label: 'Verify', to: 'VERIFIED', from: ['UNDER_REVIEW'] },
  { label: 'Send Back', to: 'DRAFT', from: ['UNDER_REVIEW'] },
  { label: 'Activate', to: 'ACTIVE', from: ['VERIFIED'] },
  { label: 'Blacklist', to: 'BLACKLISTED', from: ['VERIFIED', 'ACTIVE'] },
  { label: 'Appeal', to: 'VERIFIED', from: ['BLACKLISTED'] },
]

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

export default function BorrowerDetailPage() {
  const { borrowerId } = useParams()
  const navigate = useNavigate()
  const [borrower, setBorrower] = useState(null)
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)
  const [error, setError] = useState(null)
  const [successMessage, setSuccessMessage] = useState(null)

  const availableActions = useMemo(() => {
    if (!borrower) return []
    return actions.filter((action) => action.from.includes(borrower.status))
  }, [borrower])

  const loadBorrower = async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await borrowersApi.getById(borrowerId)
      console.log('[BorrowerDetailPage] Loaded borrower', data)
      setBorrower(data)
    } catch (err) {
      console.error('[BorrowerDetailPage] Failed to load borrower', err)
      setError(getErrorMessage(err, 'Failed to load borrower details.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadBorrower()
  }, [borrowerId])

  const handleStatusChange = async (newStatus) => {
    setActionLoading(true)
    setError(null)
    setSuccessMessage(null)
    try {
      const updated = await borrowersApi.updateStatus(borrowerId, newStatus)
      setBorrower(updated)
      setSuccessMessage(`Status updated to ${formatStatus(newStatus)}.`)
    } catch (err) {
      console.error('[BorrowerDetailPage] Failed to update borrower status', err)
      setError(getErrorMessage(err, 'Failed to update borrower status.'))
    } finally {
      setActionLoading(false)
    }
  }

  const handleDelete = async () => {
    if (!window.confirm('Delete this borrower? This cannot be undone.')) return

    setActionLoading(true)
    setError(null)
    try {
      await borrowersApi.delete(borrowerId)
      navigate('/borrowers', { state: { message: 'Borrower deleted successfully.' } })
    } catch (err) {
      console.error('[BorrowerDetailPage] Failed to delete borrower', err)
      setError(getErrorMessage(err, 'Failed to delete borrower.'))
      setActionLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center text-sm text-gray-600">
        Loading borrower details...
      </div>
    )
  }

  if (!borrower) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center text-sm text-red-600">
        {error || 'Borrower not found.'}
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white shadow-sm px-6 py-4 flex items-center justify-between">
        <h1 className="text-lg font-bold text-blue-700">LendOS</h1>
        <button className="btn-secondary" onClick={() => navigate('/borrowers')}>
          Back to Borrowers
        </button>
      </nav>

      <div className="max-w-4xl mx-auto p-8 space-y-6">
        {actionLoading && <div className="alert-info">Loading...</div>}
        {error && <div className="alert-error">{error}</div>}
        {successMessage && <div className="alert-success">{successMessage}</div>}

        <div className="card">
          <div className="flex items-start justify-between gap-4 mb-4">
            <div>
              <h2 className="text-2xl font-semibold text-gray-800">{borrower.fullName}</h2>
              <p className="text-sm text-gray-500 mt-1">Borrower ID: {borrower.id}</p>
            </div>
            <span
              className={`inline-flex px-3 py-1 rounded text-xs font-medium ${
                statusStyles[borrower.status] || 'bg-gray-100 text-gray-700'
              }`}
            >
              {formatStatus(borrower.status)}
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
            <div>
              <p className="text-gray-500">Email</p>
              <p className="text-gray-800">{borrower.email}</p>
            </div>
            <div>
              <p className="text-gray-500">Phone</p>
              <p className="text-gray-800">{borrower.phone || '-'}</p>
            </div>
            <div>
              <p className="text-gray-500">Date of Birth</p>
              <p className="text-gray-800">{formatDate(borrower.dateOfBirth)}</p>
            </div>
            <div>
              <p className="text-gray-500">Date Added</p>
              <p className="text-gray-800">{formatDate(borrower.createdAt)}</p>
            </div>
            <div className="md:col-span-2">
              <p className="text-gray-500">Address</p>
              <p className="text-gray-800 whitespace-pre-wrap">{borrower.address || '-'}</p>
            </div>
          </div>
        </div>

        <div className="card">
          <h3 className="text-lg font-semibold text-gray-800 mb-3">Actions</h3>

          <div className="flex flex-wrap gap-3">
            {availableActions.map((action) => {
              const enabled = action.from.includes(borrower.status)
              return (
                <div key={action.label} className="flex flex-col gap-1">
                  <button
                    className={enabled ? 'btn-primary' : 'btn-secondary'}
                    disabled={actionLoading || !enabled}
                    onClick={() => handleStatusChange(action.to)}
                  >
                    {action.label}
                  </button>
                  {!enabled && (
                    <span className="text-xs text-gray-500">
                      Not available from current status.
                    </span>
                  )}
                </div>
              )
            })}
          </div>

          {availableActions.length === 0 && (
            <div className="text-sm text-gray-600">No status transition is available right now.</div>
          )}

          {borrower.status === 'DRAFT' && (
            <div className="mt-5 pt-5 border-t border-gray-200">
              <button
                className="btn-danger"
                disabled={actionLoading}
                onClick={handleDelete}
              >
                Delete Borrower
              </button>
              <p className="text-xs text-gray-500 mt-2">
                Delete is only available when borrower status is Draft.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
