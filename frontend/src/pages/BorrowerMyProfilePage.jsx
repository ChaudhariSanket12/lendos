import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import BorrowerLayout from '../components/BorrowerLayout'
import { borrowerPortalApi } from '../api/borrowerPortal'

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

export default function BorrowerMyProfilePage() {
  const navigate = useNavigate()
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    const loadProfile = async () => {
      setLoading(true)
      setError(null)
      try {
        const data = await borrowerPortalApi.getProfile()
        console.log('[BorrowerMyProfilePage] Loaded profile', data)
        setProfile(data)
      } catch (err) {
        console.error('[BorrowerMyProfilePage] Failed to load profile', err)
        setError(getErrorMessage(err, 'Failed to load profile.'))
      } finally {
        setLoading(false)
      }
    }

    loadProfile()
  }, [])

  const canEditProfile = profile?.status === 'DRAFT' || profile?.status === 'UNDER_REVIEW'

  return (
    <BorrowerLayout title="My Profile" subtitle="View your submitted profile details">
      {loading ? (
        <div className="alert-info">Loading profile...</div>
      ) : (
        <div className="max-w-4xl space-y-5">
          {error && <div className="alert-error">{error}</div>}

          <div className="card">
            <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
              <h3 className="text-lg font-semibold text-gray-800">Personal Information</h3>
              <div className="flex items-center gap-2">
                <span
                  className={`inline-flex px-3 py-1 rounded text-xs font-medium ${
                    statusStyles[profile?.status] || 'bg-gray-100 text-gray-700'
                  }`}
                >
                  {formatStatus(profile?.status)}
                </span>
                {canEditProfile && (
                  <button
                    type="button"
                    className="btn-secondary"
                    onClick={() => navigate('/borrower/complete-profile')}
                  >
                    Edit Profile
                  </button>
                )}
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
              <div>
                <p className="text-gray-500">Name</p>
                <p className="text-gray-800">{profile?.fullName || '-'}</p>
              </div>
              <div>
                <p className="text-gray-500">Email</p>
                <p className="text-gray-800">{profile?.email || '-'}</p>
              </div>
              <div>
                <p className="text-gray-500">Phone</p>
                <p className="text-gray-800">{profile?.phone || '-'}</p>
              </div>
              <div>
                <p className="text-gray-500">Date of Birth</p>
                <p className="text-gray-800">{profile?.dateOfBirth || '-'}</p>
              </div>
              <div>
                <p className="text-gray-500">PAN Number</p>
                <p className="text-gray-800">{profile?.panNumber || '-'}</p>
              </div>
              <div>
                <p className="text-gray-500">Aadhaar Number</p>
                <p className="text-gray-800">{profile?.aadhaarNumber || '-'}</p>
              </div>
              <div className="md:col-span-2">
                <p className="text-gray-500">Address</p>
                <p className="text-gray-800 whitespace-pre-wrap">{profile?.address || '-'}</p>
              </div>
            </div>
          </div>

          <div className="card">
            <h3 className="text-lg font-semibold text-gray-800 mb-2">Financial Information</h3>
            <p className="text-sm text-gray-600">
              Not yet provided — complete during loan application.
            </p>
          </div>
        </div>
      )}
    </BorrowerLayout>
  )
}
