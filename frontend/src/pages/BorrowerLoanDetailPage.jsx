import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import BorrowerLayout from '../components/BorrowerLayout'
import { loansApi } from '../api/loans'

const statusStyles = {
  APPLIED: 'bg-blue-100 text-blue-700',
  UNDER_ASSESSMENT: 'bg-yellow-100 text-yellow-800',
  APPROVED: 'bg-green-100 text-green-700',
  REJECTED: 'bg-red-100 text-red-700',
  DELETED: 'bg-gray-100 text-gray-700',
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

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}

export default function BorrowerLoanDetailPage() {
  const { loanId } = useParams()
  const navigate = useNavigate()
  const [loan, setLoan] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    const loadLoan = async () => {
      setLoading(true)
      setError(null)
      try {
        const data = await loansApi.getMyLoanById(loanId)
        console.log('[BorrowerLoanDetailPage] Loaded loan', data)
        setLoan(data)
      } catch (err) {
        console.error('[BorrowerLoanDetailPage] Failed to load loan detail', err)
        setError(getErrorMessage(err, 'Failed to load loan detail.'))
      } finally {
        setLoading(false)
      }
    }
    loadLoan()
  }, [loanId])

  const displayStatus = loan?.isDeleted ? 'DELETED' : loan?.status

  return (
    <BorrowerLayout title="Loan Detail" subtitle="Review your submitted loan application">
      <button type="button" className="btn-secondary mb-4" onClick={() => navigate('/borrower/my-loans')}>
        ← Back to My Loans
      </button>

      {loading && <div className="alert-info">Loading loan detail...</div>}
      {error && <div className="alert-error">{error}</div>}

      {!loading && !error && loan && (
        <div className="space-y-5">
          <div className="card">
            <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
              <h3 className="text-lg font-semibold text-gray-800">Loan Overview</h3>
              <span className={`inline-flex px-3 py-1 rounded text-xs font-medium ${statusStyles[displayStatus] || 'bg-gray-100 text-gray-700'}`}>
                {formatStatus(displayStatus)}
              </span>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
              <div>
                <p className="text-gray-500">Loan Amount</p>
                <p className="text-gray-800">{formatCurrency(loan.loanAmount)}</p>
              </div>
              <div>
                <p className="text-gray-500">Tenure</p>
                <p className="text-gray-800">{loan.tenureMonths} months</p>
              </div>
              <div>
                <p className="text-gray-500">Purpose</p>
                <p className="text-gray-800">{formatStatus(loan.loanPurpose)}</p>
              </div>
              <div>
                <p className="text-gray-500">Applied On</p>
                <p className="text-gray-800">{formatDate(loan.appliedAt)}</p>
              </div>
              <div>
                <p className="text-gray-500">Interest Rate</p>
                <p className="text-gray-800">{loan.annualInterestRate}%</p>
              </div>
              <div>
                <p className="text-gray-500">Disbursement Date</p>
                <p className="text-gray-800">{loan.disbursementDate || '-'}</p>
              </div>
            </div>
          </div>

          {loan.isDeleted ? (
            <div className="card bg-gray-50 border-gray-200">
              <h3 className="text-lg font-semibold text-gray-800 mb-3">Application Removed</h3>
              <div className="text-sm text-gray-700 space-y-1">
                <p>Your application has been removed from active records.</p>
                <p><span className="font-medium">Reason:</span> {loan.rejectionMessage || '-'}</p>
                <p><span className="font-medium">Deleted on:</span> {formatDate(loan.deletedAt)}</p>
              </div>
              <button
                type="button"
                className="btn-primary mt-4"
                onClick={() => navigate('/borrower/apply-for-loan')}
              >
                Apply for New Loan →
              </button>
            </div>
          ) : loan.status === 'REJECTED' ? (
            <div className="card bg-red-50 border-red-200">
              <h3 className="text-lg font-semibold text-red-800 mb-3">Application Rejected</h3>
              <div className="text-sm text-red-900 space-y-1">
                <p>Your application has been rejected.</p>
                <p><span className="font-medium">Reason:</span> {loan.rejectionMessage || '-'}</p>
                <p><span className="font-medium">Rejected on:</span> {formatDate(loan.rejectedAt)}</p>
                <p>All your uploaded documents have been deleted from our system.</p>
              </div>
              <button
                type="button"
                className="btn-primary mt-4"
                onClick={() => navigate('/borrower/apply-for-loan')}
              >
                Apply for New Loan →
              </button>
            </div>
          ) : null}

          {loan.risk && (
            <div className="card">
              <h3 className="text-lg font-semibold text-gray-800 mb-3">Risk Assessment</h3>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
                <div>
                  <p className="text-gray-500">Risk Score</p>
                  <p className="text-gray-800">{loan.risk.riskScore}/100</p>
                </div>
                <div>
                  <p className="text-gray-500">FOIR</p>
                  <p className="text-gray-800">{loan.risk.foir}%</p>
                </div>
                <div>
                  <p className="text-gray-500">Recommendation</p>
                  <p className="text-gray-800">{formatStatus(loan.risk.recommendation)}</p>
                </div>
              </div>
            </div>
          )}
        </div>
      )}
    </BorrowerLayout>
  )
}
