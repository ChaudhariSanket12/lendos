import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import BorrowerLayout from '../components/BorrowerLayout'
import { borrowerPortalApi } from '../api/borrowerPortal'

function getErrorMessage(err, fallback) {
  const backendMessage = err.response?.data?.message
  const status = err.response?.status
  if (backendMessage && status) return `${backendMessage} (HTTP ${status})`
  return backendMessage || err.message || fallback
}

export default function BorrowerDashboardPage() {
  const navigate = useNavigate()
  const [borrower, setBorrower] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      setError(null)
      try {
        const data = await borrowerPortalApi.me()
        console.log('[BorrowerDashboardPage] Loaded borrower summary', data)
        setBorrower(data)
      } catch (err) {
        console.error('[BorrowerDashboardPage] Failed to load borrower summary', err)
        setError(getErrorMessage(err, 'Failed to load dashboard.'))
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  const cards = [
    { title: 'My Profile', desc: 'View and edit your profile', route: '/borrower/my-profile' },
    { title: 'Apply for Loan', desc: 'Start a new loan application', route: '/borrower/apply-for-loan' },
    { title: 'My Loans', desc: 'Track all your loan applications', route: '/borrower/my-loans' },
    { title: 'Payment Schedule', desc: 'Installment details', comingSoon: true },
  ]

  return (
    <BorrowerLayout title={`Welcome, ${borrower?.fullName || 'Borrower'}`} subtitle="Borrower dashboard">
      {loading && <div className="alert-info mb-4">Loading dashboard...</div>}
      {error && <div className="alert-error mb-4">{error}</div>}

      {!loading && !error && borrower?.status === 'DRAFT' && (
        <div className="alert-info mb-6 flex items-center justify-between gap-3">
          <span>Your profile is in draft. Complete it to move into review.</span>
          <button className="btn-primary" onClick={() => navigate('/borrower/complete-profile')}>
            Complete Profile
          </button>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {cards.map((card) => (
          <button
            type="button"
            key={card.title}
            onClick={() => card.route && navigate(card.route)}
            disabled={card.comingSoon}
            className="card text-left hover:shadow-md transition-shadow disabled:opacity-80 disabled:cursor-not-allowed"
          >
            <h3 className="text-lg font-semibold text-gray-800">{card.title}</h3>
            <p className="text-sm text-gray-500 mt-1">{card.desc}</p>
            <span className="inline-block mt-3 text-xs bg-gray-100 text-gray-700 px-2 py-1 rounded">
              {card.comingSoon ? 'Coming soon' : 'Open'}
            </span>
          </button>
        ))}
      </div>
    </BorrowerLayout>
  )
}
