import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
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

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}

export default function LoanDetailPage() {
  const { loanId } = useParams()
  const navigate = useNavigate()
  const [loan, setLoan] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)
  const [successMessage, setSuccessMessage] = useState(null)

  const loadLoan = async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await loansApi.getAdminLoanById(loanId)
      console.log('[LoanDetailPage] Loaded loan detail', data)
      setLoan(data)
    } catch (err) {
      console.error('[LoanDetailPage] Failed to load loan detail', err)
      setError(getErrorMessage(err, 'Failed to load loan detail.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadLoan()
  }, [loanId])

  const handleStatusUpdate = async (status, notes) => {
    setSaving(true)
    setError(null)
    setSuccessMessage(null)
    try {
      const updated = await loansApi.updateLoanStatus(loanId, { status, notes })
      setLoan(updated)
      setSuccessMessage(`Loan moved to ${formatStatus(status)}.`)
    } catch (err) {
      console.error('[LoanDetailPage] Failed to update loan status', err)
      setError(getErrorMessage(err, 'Failed to update loan status.'))
    } finally {
      setSaving(false)
    }
  }

  const renderActions = () => {
    if (!loan) return null

    if (loan.status === 'APPLIED') {
      return (
        <div className="flex flex-wrap gap-3">
          <button className="btn-primary" disabled={saving} onClick={() => handleStatusUpdate('UNDER_ASSESSMENT', 'Starting credit assessment')}>
            Start Assessment
          </button>
          <button className="btn-danger" disabled={saving} onClick={() => handleStatusUpdate('REJECTED', 'Rejected at initial review')}>
            Reject
          </button>
        </div>
      )
    }

    if (loan.status === 'UNDER_ASSESSMENT') {
      return (
        <div className="flex flex-wrap gap-3">
          <button className="btn-primary" disabled={saving} onClick={() => handleStatusUpdate('APPROVED', 'All checks passed')}>
            Approve
          </button>
          <button className="btn-danger" disabled={saving} onClick={() => handleStatusUpdate('REJECTED', 'Rejected after assessment')}>
            Reject
          </button>
        </div>
      )
    }

    if (loan.status === 'APPROVED') {
      return (
        <button className="btn-primary" disabled={saving} onClick={() => handleStatusUpdate('DISBURSED', 'Disbursement confirmed')}>
          Confirm Disbursement
        </button>
      )
    }

    return <p className="text-sm text-gray-600">No manual actions available for current status.</p>
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white shadow-sm px-6 py-4 flex items-center justify-between">
        <h1 className="text-lg font-bold text-blue-700">LendOS</h1>
        <button className="btn-secondary" onClick={() => navigate('/loans')}>
          Back to Loans
        </button>
      </nav>

      <div className="max-w-5xl mx-auto p-8 space-y-5">
        {loading && <div className="alert-info">Loading loan detail...</div>}
        {error && <div className="alert-error">{error}</div>}
        {successMessage && <div className="alert-success">{successMessage}</div>}

        {!loading && !error && loan && (
          <>
            <div className="card">
              <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
                <h2 className="text-xl font-semibold text-gray-800">Loan Details</h2>
                <span className={`inline-flex px-3 py-1 rounded text-xs font-medium ${statusStyles[loan.status] || 'bg-gray-100 text-gray-700'}`}>
                  {formatStatus(loan.status)}
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
                  <p className="text-gray-500">Loan Purpose</p>
                  <p className="text-gray-800">{formatStatus(loan.loanPurpose)}</p>
                </div>
                <div>
                  <p className="text-gray-500">Applied At</p>
                  <p className="text-gray-800">{formatDate(loan.appliedAt)}</p>
                </div>
                <div>
                  <p className="text-gray-500">Disbursement Date</p>
                  <p className="text-gray-800">{loan.disbursementDate || '-'}</p>
                </div>
                <div>
                  <p className="text-gray-500">Status Notes</p>
                  <p className="text-gray-800">{loan.statusNotes || '-'}</p>
                </div>
              </div>
            </div>

            <div className="card">
              <h3 className="text-lg font-semibold text-gray-800 mb-3">Borrower Profile Summary</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                <div>
                  <p className="text-gray-500">Name</p>
                  <p className="text-gray-800">{loan.borrower?.fullName || '-'}</p>
                </div>
                <div>
                  <p className="text-gray-500">Borrower Status</p>
                  <p className="text-gray-800">{formatStatus(loan.borrower?.status)}</p>
                </div>
                <div>
                  <p className="text-gray-500">Email</p>
                  <p className="text-gray-800">{loan.borrower?.email || '-'}</p>
                </div>
                <div>
                  <p className="text-gray-500">Phone</p>
                  <p className="text-gray-800">{loan.borrower?.phone || '-'}</p>
                </div>
                <div>
                  <p className="text-gray-500">PAN</p>
                  <p className="text-gray-800">{loan.borrower?.panNumber || '-'}</p>
                </div>
                <div>
                  <p className="text-gray-500">Aadhaar</p>
                  <p className="text-gray-800">{loan.borrower?.aadhaarNumber || '-'}</p>
                </div>
              </div>
            </div>

            <div className="card">
              <h3 className="text-lg font-semibold text-gray-800 mb-3">Financial Profile</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                <div>
                  <p className="text-gray-500">Monthly Income</p>
                  <p className="text-gray-800">{formatCurrency(loan.borrower?.monthlyIncome)}</p>
                </div>
                <div>
                  <p className="text-gray-500">Existing Obligations</p>
                  <p className="text-gray-800">{formatCurrency(loan.borrower?.existingMonthlyObligations)}</p>
                </div>
                <div>
                  <p className="text-gray-500">Employment Type</p>
                  <p className="text-gray-800">{formatStatus(loan.borrower?.employmentType)}</p>
                </div>
                <div>
                  <p className="text-gray-500">Years in Current Job</p>
                  <p className="text-gray-800">{loan.borrower?.yearsInCurrentJob ?? '-'}</p>
                </div>
                <div>
                  <p className="text-gray-500">Total Work Experience</p>
                  <p className="text-gray-800">{loan.borrower?.totalWorkExperience ?? '-'}</p>
                </div>
                <div>
                  <p className="text-gray-500">Residence Type</p>
                  <p className="text-gray-800">{formatStatus(loan.borrower?.residenceType)}</p>
                </div>
                <div>
                  <p className="text-gray-500">Years at Current Residence</p>
                  <p className="text-gray-800">{loan.borrower?.yearsAtCurrentResidence ?? '-'}</p>
                </div>
                <div>
                  <p className="text-gray-500">CIBIL Score</p>
                  <p className="text-gray-800">{loan.borrower?.cibilScore ?? '-'}</p>
                </div>
              </div>
            </div>

            {(loan.status !== 'APPLIED') && (
              <div className="card">
                <h3 className="text-lg font-semibold text-gray-800 mb-3">Risk Assessment</h3>
                {loan.risk ? (
                  <div className="grid grid-cols-1 md:grid-cols-4 gap-4 text-sm">
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
                    <div>
                      <p className="text-gray-500">Assessed At</p>
                      <p className="text-gray-800">{formatDate(loan.risk.assessedAt)}</p>
                    </div>
                  </div>
                ) : (
                  <p className="text-sm text-gray-600">Risk evaluation not available yet.</p>
                )}
              </div>
            )}

            <div className="card">
              <h3 className="text-lg font-semibold text-gray-800 mb-3">Actions</h3>
              {renderActions()}
            </div>
          </>
        )}
      </div>
    </div>
  )
}
