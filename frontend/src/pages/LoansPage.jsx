import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { loansApi } from '../api/loans'

const statusOptions = [
  { label: 'All', value: '' },
  { label: 'Applied', value: 'APPLIED' },
  { label: 'Under Assessment', value: 'UNDER_ASSESSMENT' },
  { label: 'Approved', value: 'APPROVED' },
  { label: 'Rejected', value: 'REJECTED' },
  { label: 'Disbursed', value: 'DISBURSED' },
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Closed', value: 'CLOSED' },
]

const statusStyles = {
  APPLIED: 'bg-blue-100 text-blue-700',
  UNDER_ASSESSMENT: 'bg-yellow-100 text-yellow-800',
  APPROVED: 'bg-green-100 text-green-700',
  REJECTED: 'bg-red-100 text-red-700',
  DISBURSED: 'bg-purple-100 text-purple-700',
  ACTIVE: 'bg-green-100 text-green-700',
  CLOSED: 'bg-gray-100 text-gray-700',
  DEFAULTED: 'bg-red-100 text-red-700',
}

function getErrorMessage(err, fallback) {
  const backendMessage = err.response?.data?.message
  const status = err.response?.status
  if (backendMessage && status) return `${backendMessage} (HTTP ${status})`
  return backendMessage || err.message || fallback
}

function formatStatus(status) {
  return status ? status.replaceAll('_', ' ') : 'UNKNOWN'
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleDateString()
}

function formatCurrency(amount) {
  if (amount === null || amount === undefined) return '-'
  return `₹${Number(amount).toLocaleString('en-IN', { maximumFractionDigits: 2 })}`
}

export default function LoansPage() {
  const navigate = useNavigate()
  const [loans, setLoans] = useState([])
  const [statusFilter, setStatusFilter] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    const loadLoans = async () => {
      setLoading(true)
      setError(null)
      try {
        const data = await loansApi.listAdminLoans(statusFilter || undefined)
        console.log('[LoansPage] Loaded loans', data)
        setLoans(data)
      } catch (err) {
        console.error('[LoansPage] Failed to load loans', err)
        setError(getErrorMessage(err, 'Failed to load loan applications.'))
      } finally {
        setLoading(false)
      }
    }
    loadLoans()
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
            <h2 className="text-2xl font-semibold text-gray-800">Loan Applications</h2>
            <p className="text-sm text-gray-500 mt-1">Review and manage loan applications for your tenant.</p>
          </div>
          <select className="input w-56" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            {statusOptions.map((option) => (
              <option key={option.label} value={option.value}>{option.label}</option>
            ))}
          </select>
        </div>

        {error && <div className="alert-error mb-4">{error}</div>}

        <div className="card p-0 overflow-hidden">
          {loading ? (
            <div className="p-6 text-sm text-gray-600">Loading loan applications...</div>
          ) : loans.length === 0 ? (
            <div className="p-6 text-sm text-gray-600">No loan applications found for this filter.</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-50 border-b border-gray-200">
                  <tr>
                    <th className="text-left text-xs font-semibold text-gray-600 px-4 py-3">Borrower Name</th>
                    <th className="text-left text-xs font-semibold text-gray-600 px-4 py-3">Amount</th>
                    <th className="text-left text-xs font-semibold text-gray-600 px-4 py-3">Tenure</th>
                    <th className="text-left text-xs font-semibold text-gray-600 px-4 py-3">Purpose</th>
                    <th className="text-left text-xs font-semibold text-gray-600 px-4 py-3">Status</th>
                    <th className="text-left text-xs font-semibold text-gray-600 px-4 py-3">Applied Date</th>
                  </tr>
                </thead>
                <tbody>
                  {loans.map((loan) => (
                    <tr
                      key={loan.id}
                      onClick={() => navigate(`/loans/${loan.id}`)}
                      className="border-b border-gray-100 hover:bg-gray-50 cursor-pointer"
                    >
                      <td className="px-4 py-3 text-sm text-gray-800">
                        <div className="flex items-center gap-2">
                          <span>{loan.borrowerName || '-'}</span>
                          {loan.status === 'APPLIED' && (
                            <span className="inline-flex px-2 py-0.5 rounded text-[10px] font-semibold bg-orange-100 text-orange-700">
                              New
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-700">{formatCurrency(loan.loanAmount)}</td>
                      <td className="px-4 py-3 text-sm text-gray-700">{loan.tenureMonths} months</td>
                      <td className="px-4 py-3 text-sm text-gray-700">{formatStatus(loan.loanPurpose)}</td>
                      <td className="px-4 py-3 text-sm">
                        <span className={`inline-flex px-2 py-1 rounded text-xs font-medium ${statusStyles[loan.status] || 'bg-gray-100 text-gray-700'}`}>
                          {formatStatus(loan.status)}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-700">{formatDate(loan.appliedAt)}</td>
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
