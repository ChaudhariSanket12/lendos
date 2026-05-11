import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import BorrowerLayout from '../components/BorrowerLayout'
import { borrowerPortalApi } from '../api/borrowerPortal'
import { loansApi } from '../api/loans'

const statusStyles = {
  DRAFT: 'bg-gray-100 text-gray-700',
  UNDER_REVIEW: 'bg-yellow-100 text-yellow-800',
  VERIFIED: 'bg-blue-100 text-blue-700',
  ACTIVE: 'bg-green-100 text-green-700',
  BLACKLISTED: 'bg-red-100 text-red-700',
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

export default function BorrowerDashboardPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [borrower, setBorrower] = useState(null)
  const [loans, setLoans] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [notice, setNotice] = useState(
    location.state?.message ? { type: 'success', text: location.state.message } : null
  )

  useEffect(() => {
    if (location.state?.message) {
      navigate(location.pathname, { replace: true, state: {} })
    }
  }, [location.pathname, location.state, navigate])

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      setError(null)
      try {
        const [borrowerData, loansData] = await Promise.all([
          borrowerPortalApi.me(),
          loansApi.getMyLoans().catch((err) => {
            console.error('[BorrowerDashboardPage] Failed to load loans list', err)
            return []
          }),
        ])
        console.log('[BorrowerDashboardPage] Loaded borrower summary', borrowerData)
        console.log('[BorrowerDashboardPage] Loaded loans summary', loansData)
        setBorrower(borrowerData)
        setLoans(Array.isArray(loansData) ? loansData : [])
      } catch (err) {
        console.error('[BorrowerDashboardPage] Failed to load dashboard', err)
        setError(getErrorMessage(err, 'Failed to load dashboard.'))
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  const isProfileComplete = useMemo(
    () => borrower?.status && borrower.status !== 'DRAFT',
    [borrower]
  )
  const canApplyForLoan = useMemo(
    () => borrower?.status === 'UNDER_REVIEW' || borrower?.status === 'VERIFIED',
    [borrower]
  )

  const activeLoansCount = useMemo(
    () => loans.filter((loan) => loan.status === 'ACTIVE').length,
    [loans]
  )
  const hasRejectedOrDeletedLoan = useMemo(
    () => loans.some((loan) => loan.status === 'REJECTED' || loan.isDeleted),
    [loans]
  )

  const cards = [
    { key: 'profile', title: 'My Profile', desc: 'View your profile details', route: '/borrower/my-profile' },
    { key: 'apply', title: 'Apply for Loan', desc: 'Start a new loan application', route: '/borrower/apply-for-loan' },
    { key: 'loans', title: 'My Loans', desc: 'Track all your loan applications', route: '/borrower/my-loans' },
    { key: 'schedule', title: 'Payment Schedule', desc: 'Installment details', comingSoon: true },
  ]

  const handleCardClick = (card) => {
    if (card.comingSoon) return
    if (card.key === 'apply' && !canApplyForLoan) {
      setNotice({
        type: 'info',
        text: isProfileComplete
          ? 'Loan application is currently unavailable for your profile status'
          : 'Please complete your profile first',
      })
      return
    }
    if (card.route) {
      setNotice(null)
      navigate(card.route)
    }
  }

  return (
    <BorrowerLayout title={`Welcome, ${borrower?.fullName || 'Borrower'}`} subtitle="Borrower dashboard">
      {loading && <div className="alert-info mb-4">Loading dashboard...</div>}
      {error && <div className="alert-error mb-4">{error}</div>}
      {notice && (
        <div className={`${notice.type === 'success' ? 'alert-success' : 'alert-info'} mb-4`}>
          {notice.text}
        </div>
      )}

      {!loading && !error && borrower && (
        <div className="card mb-6">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="text-sm text-gray-500">Current Status</p>
              <span className={`inline-flex mt-1 px-3 py-1 rounded text-xs font-medium ${statusStyles[borrower.status] || 'bg-gray-100 text-gray-700'}`}>
                {formatStatus(borrower.status)}
              </span>
            </div>
            {!isProfileComplete ? (
              <button className="btn-primary" onClick={() => navigate('/borrower/complete-profile')}>
                Complete Profile
              </button>
            ) : null}
          </div>
        </div>
      )}

      {!loading && !error && borrower?.status === 'DRAFT' && (
        <div className="alert-info mb-6 flex items-center justify-between gap-3">
          <span>⚠️ Complete your profile to apply for loans</span>
          <button className="btn-primary" onClick={() => navigate('/borrower/complete-profile')}>
            Complete Profile
          </button>
        </div>
      )}

      {!loading && !error && borrower?.status === 'UNDER_REVIEW' && (
        <div className="alert-success mb-6">✅ Profile Verified — Under Review</div>
      )}

      {!loading && !error && hasRejectedOrDeletedLoan && canApplyForLoan && (
        <div className="alert-info mb-6">Your last application was not approved. You can apply again.</div>
      )}

      {!loading && !error && isProfileComplete && (
        <div className="card mb-6">
          <h3 className="text-lg font-semibold text-gray-800 mb-2">Quick Actions</h3>
          <p className="text-sm text-gray-600 mb-4">
            {activeLoansCount > 0 ? `You have ${activeLoansCount} active loans` : 'No active loans'}
          </p>
          <div className="flex flex-wrap gap-3">
            {loans.length === 0 ? (
              canApplyForLoan ? (
                <button className="btn-primary" onClick={() => navigate('/borrower/apply-for-loan')}>
                  Apply for Your First Loan →
                </button>
              ) : (
                <button className="btn-secondary" onClick={() => navigate('/borrower/my-loans')}>
                  View My Loans →
                </button>
              )
            ) : (
              <>
                <button className="btn-secondary" onClick={() => navigate('/borrower/my-loans')}>
                  View My Loans →
                </button>
                {canApplyForLoan && (
                  <button className="btn-primary" onClick={() => navigate('/borrower/apply-for-loan')}>
                    {hasRejectedOrDeletedLoan ? 'Apply for Loan →' : 'Apply for Another Loan →'}
                  </button>
                )}
              </>
            )}
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {cards.map((card) => {
          const applyDisabled = card.key === 'apply' && !canApplyForLoan
          return (
            <button
              type="button"
              key={card.title}
              onClick={() => handleCardClick(card)}
              disabled={card.comingSoon}
              className={`card text-left hover:shadow-md transition-shadow disabled:opacity-80 disabled:cursor-not-allowed ${
                applyDisabled ? 'opacity-60 bg-gray-50 border-gray-300 cursor-not-allowed' : ''
              }`}
            >
              <h3 className="text-lg font-semibold text-gray-800">{card.title}</h3>
              <p className="text-sm text-gray-500 mt-1">{card.desc}</p>
              <span className={`inline-block mt-3 text-xs px-2 py-1 rounded ${
                card.comingSoon
                  ? 'bg-gray-100 text-gray-700'
                  : applyDisabled
                    ? 'bg-amber-100 text-amber-800'
                    : 'bg-green-100 text-green-700'
              }`}>
                {card.comingSoon ? 'Coming soon' : applyDisabled ? 'Complete profile first' : 'Open'}
              </span>
            </button>
          )
        })}
      </div>
    </BorrowerLayout>
  )
}
