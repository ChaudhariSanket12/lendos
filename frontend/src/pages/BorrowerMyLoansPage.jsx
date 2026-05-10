import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import BorrowerLayout from '../components/BorrowerLayout'
import { loansApi } from '../api/loans'

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

function formatCurrency(amount) {
  if (amount === null || amount === undefined) return '-'
  return `₹${Number(amount).toLocaleString('en-IN', { maximumFractionDigits: 2 })}`
}

function shortLoanId(loanId) {
  return loanId ? loanId.slice(-6).toUpperCase() : '-'
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleDateString()
}

export default function BorrowerMyLoansPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [loans, setLoans] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [successMessage, setSuccessMessage] = useState(location.state?.message || null)

  useEffect(() => {
    if (location.state?.message) {
      navigate(location.pathname, { replace: true, state: {} })
    }
  }, [location.pathname, location.state, navigate])

  useEffect(() => {
    const loadLoans = async () => {
      setLoading(true)
      setError(null)
      try {
        const data = await loansApi.getMyLoans()
        console.log('[BorrowerMyLoansPage] Loaded loans', data)
        setLoans(data)
      } catch (err) {
        console.error('[BorrowerMyLoansPage] Failed to load loans', err)
        setError(getErrorMessage(err, 'Failed to load loans.'))
      } finally {
        setLoading(false)
      }
    }
    loadLoans()
  }, [])

  return (
    <BorrowerLayout title="My Loans" subtitle="Track all your loan applications and statuses">
      {successMessage && (
        <div className="alert-success mb-4 flex items-center justify-between">
          <span>{successMessage}</span>
          <button type="button" className="text-green-700 text-sm hover:underline" onClick={() => setSuccessMessage(null)}>
            Dismiss
          </button>
        </div>
      )}
      {error && <div className="alert-error mb-4">{error}</div>}

      <div className="card p-0 overflow-hidden">
        {loading ? (
          <div className="p-6 text-sm text-gray-600">Loading loans...</div>
        ) : loans.length === 0 ? (
          <div className="p-6">
            <h3 className="text-lg font-semibold text-gray-800">You haven&apos;t applied for any loans yet</h3>
            <p className="text-sm text-gray-500 mt-2">Start your first application to see it here.</p>
            <button type="button" className="btn-primary mt-4" onClick={() => navigate('/borrower/apply-for-loan')}>
              Apply Now
            </button>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="text-left text-xs font-semibold text-gray-600 px-4 py-3">Loan ID</th>
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
                    className="border-b border-gray-100 hover:bg-gray-50 cursor-pointer"
                    onClick={() => navigate(`/borrower/my-loans/${loan.id}`)}
                  >
                    <td className="px-4 py-3 text-sm font-mono text-gray-700">{shortLoanId(loan.id)}</td>
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
    </BorrowerLayout>
  )
}
