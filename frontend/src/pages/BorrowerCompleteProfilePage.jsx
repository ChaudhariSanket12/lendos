import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
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

export default function BorrowerCompleteProfilePage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({
    phone: '',
    dateOfBirth: '',
    address: '',
    monthlyIncome: '',
    employmentType: '',
    yearsInCurrentJob: '',
    existingMonthlyObligations: '0',
    panNumber: '',
  })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})

  useEffect(() => {
    const loadProfile = async () => {
      setLoading(true)
      setError(null)
      try {
        const profile = await borrowerPortalApi.getProfile()
        console.log('[BorrowerCompleteProfilePage] Loaded profile', profile)
        setForm((prev) => ({
          ...prev,
          phone: profile.phone || '',
          dateOfBirth: profile.dateOfBirth || '',
          address: profile.address || '',
          monthlyIncome: profile.monthlyIncome ?? '',
          employmentType: profile.employmentType || '',
          yearsInCurrentJob: profile.yearsInCurrentJob ?? '',
          existingMonthlyObligations: profile.existingMonthlyObligations ?? '0',
          panNumber: profile.panNumber || '',
        }))
      } catch (err) {
        console.error('[BorrowerCompleteProfilePage] Failed to load profile', err)
        setError(getErrorMessage(err, 'Failed to load profile.'))
      } finally {
        setLoading(false)
      }
    }

    loadProfile()
  }, [])

  const completedCount = useMemo(() => {
    const required = [
      form.phone,
      form.dateOfBirth,
      form.address,
      form.monthlyIncome,
      form.employmentType,
      form.yearsInCurrentJob,
      form.existingMonthlyObligations,
    ]
    return required.filter((value) => String(value ?? '').trim() !== '').length
  }, [form])

  const progress = Math.round((completedCount / 7) * 100)

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value })

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    setError(null)
    setFieldErrors({})
    try {
      await borrowerPortalApi.completeProfile({
        phone: form.phone.trim(),
        dateOfBirth: form.dateOfBirth,
        address: form.address.trim(),
        monthlyIncome: Number(form.monthlyIncome),
        employmentType: form.employmentType,
        yearsInCurrentJob: Number(form.yearsInCurrentJob),
        existingMonthlyObligations: Number(form.existingMonthlyObligations),
        panNumber: form.panNumber.trim() || null,
      })
      navigate('/borrower/dashboard')
    } catch (err) {
      console.error('[BorrowerCompleteProfilePage] Failed to complete profile', err)
      const backendErrors = err.response?.data?.errors || {}
      if (backendErrors && typeof backendErrors === 'object') {
        setFieldErrors(backendErrors)
      }
      setError(getErrorMessage(err, 'Failed to complete profile.'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <BorrowerLayout title="Complete Profile" subtitle="Finish your profile to move into review">
      {loading ? (
        <div className="alert-info">Loading profile...</div>
      ) : (
        <div className="max-w-3xl">
          <div className="card mb-5">
            <div className="flex items-center justify-between mb-2">
              <h3 className="text-sm font-semibold text-gray-700">Progress</h3>
              <span className="text-sm text-gray-600">{progress}%</span>
            </div>
            <div className="w-full h-2 bg-gray-200 rounded">
              <div className="h-2 bg-blue-600 rounded" style={{ width: `${progress}%` }} />
            </div>
          </div>

          {error && <div className="alert-error mb-4">{error}</div>}
          {Object.keys(fieldErrors).length > 0 && (
            <div className="alert-error mb-4">
              {Object.entries(fieldErrors).map(([field, message]) => (
                <div key={field}>{field}: {message}</div>
              ))}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="card">
              <h3 className="text-lg font-semibold text-gray-800 mb-4">Section 1: Personal Info</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="label">Phone</label>
                  <input className="input" value={form.phone} onChange={set('phone')} required />
                </div>
                <div>
                  <label className="label">Date of Birth</label>
                  <input className="input" type="date" value={form.dateOfBirth} onChange={set('dateOfBirth')} required />
                </div>
                <div className="md:col-span-2">
                  <label className="label">Address</label>
                  <textarea className="input" rows={3} value={form.address} onChange={set('address')} required />
                </div>
              </div>
            </div>

            <div className="card">
              <h3 className="text-lg font-semibold text-gray-800 mb-4">Section 2: Financial Info</h3>
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
              </div>
            </div>

            <div className="card">
              <h3 className="text-lg font-semibold text-gray-800 mb-4">Section 3: Identity</h3>
              <div>
                <label className="label">PAN Number (Optional)</label>
                <input
                  className="input"
                  value={form.panNumber}
                  maxLength={10}
                  onChange={set('panNumber')}
                  placeholder="ABCDE1234F"
                />
              </div>
            </div>

            <button type="submit" className="btn-primary" disabled={saving}>
              {saving ? 'Submitting...' : 'Submit Profile'}
            </button>
          </form>
        </div>
      )}
    </BorrowerLayout>
  )
}
