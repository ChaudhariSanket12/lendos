import { useEffect, useState } from 'react'
import BorrowerLayout from '../components/BorrowerLayout'
import { borrowerPortalApi } from '../api/borrowerPortal'

const employmentOptions = [
  { label: 'Salaried', value: 'SALARIED' },
  { label: 'Self Employed', value: 'SELF_EMPLOYED' },
  { label: 'Government', value: 'GOVERNMENT' },
  { label: 'Business', value: 'BUSINESS' },
]

function getErrorMessage(err, fallback) {
  const backendMessage = err.response?.data?.message
  const status = err.response?.status
  if (backendMessage && status) return `${backendMessage} (HTTP ${status})`
  return backendMessage || err.message || fallback
}

export default function BorrowerMyProfilePage() {
  const [profile, setProfile] = useState(null)
  const [form, setForm] = useState({
    monthlyIncome: '',
    employmentType: '',
    yearsInCurrentJob: '',
    existingMonthlyObligations: '0',
    panNumber: '',
  })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})

  const loadProfile = async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await borrowerPortalApi.getProfile()
      console.log('[BorrowerMyProfilePage] Loaded profile', data)
      setProfile(data)
      setForm({
        monthlyIncome: data.monthlyIncome ?? '',
        employmentType: data.employmentType || '',
        yearsInCurrentJob: data.yearsInCurrentJob ?? '',
        existingMonthlyObligations: data.existingMonthlyObligations ?? '0',
        panNumber: data.panNumber || '',
      })
    } catch (err) {
      console.error('[BorrowerMyProfilePage] Failed to load profile', err)
      setError(getErrorMessage(err, 'Failed to load profile.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadProfile()
  }, [])

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value })

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    setError(null)
    setSuccess(null)
    setFieldErrors({})

    try {
      const updated = await borrowerPortalApi.updateProfile({
        monthlyIncome: Number(form.monthlyIncome),
        employmentType: form.employmentType,
        yearsInCurrentJob: Number(form.yearsInCurrentJob),
        existingMonthlyObligations: Number(form.existingMonthlyObligations),
        panNumber: form.panNumber.trim() || null,
      })
      setProfile(updated)
      setSuccess('Profile updated successfully.')
    } catch (err) {
      console.error('[BorrowerMyProfilePage] Failed to update profile', err)
      const backendErrors = err.response?.data?.errors || {}
      if (backendErrors && typeof backendErrors === 'object') {
        setFieldErrors(backendErrors)
      }
      setError(getErrorMessage(err, 'Failed to update profile.'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <BorrowerLayout title="My Profile" subtitle="View and update your profile details">
      {loading ? (
        <div className="alert-info">Loading profile...</div>
      ) : (
        <div className="max-w-4xl space-y-5">
          {error && <div className="alert-error">{error}</div>}
          {success && <div className="alert-success">{success}</div>}
          {Object.keys(fieldErrors).length > 0 && (
            <div className="alert-error">
              {Object.entries(fieldErrors).map(([field, message]) => (
                <div key={field}>{field}: {message}</div>
              ))}
            </div>
          )}

          <div className="card">
            <h3 className="text-lg font-semibold text-gray-800 mb-4">Personal Information</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
              <div>
                <p className="text-gray-500">Name</p>
                <p className="text-gray-800">{profile?.fullName}</p>
              </div>
              <div>
                <p className="text-gray-500">Email</p>
                <p className="text-gray-800">{profile?.email}</p>
              </div>
              <div>
                <p className="text-gray-500">Phone</p>
                <p className="text-gray-800">{profile?.phone || '-'}</p>
              </div>
              <div>
                <p className="text-gray-500">Status</p>
                <p className="text-gray-800">{profile?.status || '-'}</p>
              </div>
              <div>
                <p className="text-gray-500">Date of Birth</p>
                <p className="text-gray-800">{profile?.dateOfBirth || '-'}</p>
              </div>
              <div className="md:col-span-2">
                <p className="text-gray-500">Address</p>
                <p className="text-gray-800">{profile?.address || '-'}</p>
              </div>
            </div>
          </div>

          <form className="card" onSubmit={handleSubmit}>
            <h3 className="text-lg font-semibold text-gray-800 mb-4">Financial Information</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="label">Monthly Income</label>
                <input className="input" type="number" min="0" step="0.01" value={form.monthlyIncome} onChange={set('monthlyIncome')} required />
              </div>
              <div>
                <label className="label">Employment Type</label>
                <select className="input" value={form.employmentType} onChange={set('employmentType')} required>
                  <option value="">Select employment type</option>
                  {employmentOptions.map((item) => (
                    <option key={item.value} value={item.value}>{item.label}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="label">Years in Current Job</label>
                <input className="input" type="number" min="0" step="0.1" value={form.yearsInCurrentJob} onChange={set('yearsInCurrentJob')} required />
              </div>
              <div>
                <label className="label">Existing Monthly Obligations</label>
                <input className="input" type="number" min="0" step="0.01" value={form.existingMonthlyObligations} onChange={set('existingMonthlyObligations')} required />
              </div>
              <div>
                <label className="label">PAN Number (Optional)</label>
                <input className="input" maxLength={10} value={form.panNumber} onChange={set('panNumber')} />
              </div>
            </div>
            <button type="submit" className="btn-primary mt-5" disabled={saving}>
              {saving ? 'Saving...' : 'Save Profile'}
            </button>
          </form>
        </div>
      )}
    </BorrowerLayout>
  )
}
