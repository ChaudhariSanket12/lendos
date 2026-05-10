import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import BorrowerLayout from '../components/BorrowerLayout'
import { borrowerPortalApi } from '../api/borrowerPortal'

const STEP_ITEMS = [
  { id: 1, label: 'Identity', active: true },
  { id: 2, label: 'Documents', active: false },
  { id: 3, label: 'Loan Application', active: false },
]

const PAN_REGEX = /^[A-Z]{5}[0-9]{4}[A-Z]$/

function getErrorMessage(err, fallback) {
  const backendMessage = err.response?.data?.message
  const status = err.response?.status
  if (backendMessage && status) return `${backendMessage} (HTTP ${status})`
  return backendMessage || err.message || fallback
}

function normalizePhone(value) {
  return (value || '').replace(/[\s+-]/g, '').replace(/[^\d]/g, '')
}

function normalizePan(value) {
  return (value || '').trim().toUpperCase()
}

function normalizeAadhaar(value) {
  return (value || '').replace(/\s+/g, '')
}

function validateForm(form) {
  const errors = {}
  const normalizedPhone = normalizePhone(form.phone)
  const normalizedAddress = (form.address || '').trim()
  const normalizedPan = normalizePan(form.panNumber)
  const normalizedAadhaar = normalizeAadhaar(form.aadhaarNumber)
  const today = new Date()

  if (!normalizedPhone) {
    errors.phone = 'Phone number is required'
  } else if (!/^(\d{10}|\d{11,13})$/.test(normalizedPhone)) {
    errors.phone = 'Please enter a valid phone number'
  }

  if (!form.dateOfBirth) {
    errors.dateOfBirth = 'Date of birth is required'
  } else {
    const dateOfBirth = new Date(form.dateOfBirth)
    if (dateOfBirth > today) {
      errors.dateOfBirth = 'Date of birth cannot be in the future'
    } else {
      const eighteenthBirthday = new Date(dateOfBirth)
      eighteenthBirthday.setFullYear(eighteenthBirthday.getFullYear() + 18)
      if (eighteenthBirthday > today) {
        errors.dateOfBirth = 'You must be at least 18 years old'
      }
    }
  }

  if (!normalizedAddress) {
    errors.address = 'Address is required'
  } else if (normalizedAddress.length < 10) {
    errors.address = 'Address must be at least 10 characters'
  }

  if (!normalizedPan) {
    errors.panNumber = 'PAN number is required'
  } else if (!PAN_REGEX.test(normalizedPan)) {
    errors.panNumber = 'Invalid PAN format (e.g., ABCDE1234F)'
  }

  if (normalizedAadhaar && !/^\d{12}$/.test(normalizedAadhaar)) {
    errors.aadhaarNumber = 'Aadhaar must be 12 digits'
  }

  return errors
}

export default function BorrowerCompleteProfilePage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({
    phone: '',
    dateOfBirth: '',
    address: '',
    panNumber: '',
    aadhaarNumber: '',
  })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)
  const [submitted, setSubmitted] = useState(false)
  const [touched, setTouched] = useState({})
  const [backendErrors, setBackendErrors] = useState({})

  const clientErrors = useMemo(() => validateForm(form), [form])
  const isFormValid = Object.keys(clientErrors).length === 0

  useEffect(() => {
    const loadProfile = async () => {
      setLoading(true)
      setError(null)
      try {
        const profile = await borrowerPortalApi.getProfile()
        console.log('[BorrowerCompleteProfilePage] Loaded profile', profile)
        setForm({
          phone: profile.phone || '',
          dateOfBirth: profile.dateOfBirth || '',
          address: profile.address || '',
          panNumber: profile.panNumber || '',
          aadhaarNumber: profile.aadhaarNumber || '',
        })
      } catch (err) {
        console.error('[BorrowerCompleteProfilePage] Failed to load profile', err)
        setError(getErrorMessage(err, 'Failed to load profile.'))
      } finally {
        setLoading(false)
      }
    }

    loadProfile()
  }, [])

  const set = (field) => (e) => {
    const nextValue =
      field === 'panNumber'
        ? normalizePan(e.target.value)
        : e.target.value

    setForm((prev) => ({ ...prev, [field]: nextValue }))
    setBackendErrors((prev) => {
      if (!prev[field]) return prev
      const clone = { ...prev }
      delete clone[field]
      return clone
    })
  }

  const onBlur = (field) => () => {
    setTouched((prev) => ({ ...prev, [field]: true }))
  }

  const fieldError = (field) => {
    if (backendErrors[field]) return backendErrors[field]
    if (submitted || touched[field]) return clientErrors[field]
    return null
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSubmitted(true)
    setError(null)
    setBackendErrors({})

    if (!isFormValid) {
      return
    }

    const payload = {
      phone: form.phone.trim(),
      dateOfBirth: form.dateOfBirth,
      address: form.address.trim(),
      panNumber: normalizePan(form.panNumber),
      aadhaarNumber: normalizeAadhaar(form.aadhaarNumber) || null,
    }

    setSaving(true)
    try {
      console.log('[BorrowerCompleteProfilePage] completeProfile payload', payload)
      await borrowerPortalApi.completeProfile(payload)
      navigate('/borrower/dashboard', {
        state: { message: 'Profile submitted for review!' },
      })
    } catch (err) {
      console.error('[BorrowerCompleteProfilePage] Failed to complete profile', err)
      const errors = err.response?.data?.errors
      if (errors && typeof errors === 'object') {
        setBackendErrors(errors)
      }
      setError(getErrorMessage(err, 'Failed to complete profile.'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <BorrowerLayout
      title="Complete Your Profile"
      subtitle="Step 1 of 3: Identity Verification"
    >
      {loading ? (
        <div className="alert-info">Loading profile...</div>
      ) : (
        <div className="max-w-3xl">
          <div className="card mb-5">
            <div className="flex flex-wrap items-center gap-2 text-sm">
              {STEP_ITEMS.map((item, index) => (
                <div key={item.id} className="flex items-center gap-2">
                  <span
                    className={`inline-flex items-center justify-center w-6 h-6 rounded-full text-xs font-semibold ${
                      item.active ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-600'
                    }`}
                  >
                    {item.id}
                  </span>
                  <span className={item.active ? 'text-blue-700 font-semibold' : 'text-gray-500'}>
                    {item.label}
                  </span>
                  {index < STEP_ITEMS.length - 1 && <span className="text-gray-400">→</span>}
                </div>
              ))}
            </div>
          </div>

          {error && <div className="alert-error mb-4">{error}</div>}

          <form onSubmit={handleSubmit} className="card space-y-5">
            <h3 className="text-lg font-semibold text-gray-800">Identity Verification</h3>

            <div>
              <label className="label">Phone Number</label>
              <input
                className="input"
                value={form.phone}
                onChange={set('phone')}
                onBlur={onBlur('phone')}
                placeholder="+91 9876543210"
              />
              <p className="text-xs text-gray-500 mt-1">Use 10-digit Indian number or +91 format.</p>
              {fieldError('phone') && <p className="text-xs text-red-600 mt-1">{fieldError('phone')}</p>}
            </div>

            <div>
              <label className="label">Date of Birth</label>
              <input
                className="input"
                type="date"
                value={form.dateOfBirth}
                onChange={set('dateOfBirth')}
                onBlur={onBlur('dateOfBirth')}
              />
              <p className="text-xs text-gray-500 mt-1">Must be 18 years or older.</p>
              {fieldError('dateOfBirth') && <p className="text-xs text-red-600 mt-1">{fieldError('dateOfBirth')}</p>}
            </div>

            <div>
              <label className="label">Full Address</label>
              <textarea
                className="input"
                rows={3}
                value={form.address}
                onChange={set('address')}
                onBlur={onBlur('address')}
                placeholder="House/Street, Area, City, State, PIN"
              />
              <p className="text-xs text-gray-500 mt-1">Minimum 10 characters.</p>
              {fieldError('address') && <p className="text-xs text-red-600 mt-1">{fieldError('address')}</p>}
            </div>

            <div>
              <label className="label">PAN Number</label>
              <input
                className="input"
                value={form.panNumber}
                maxLength={10}
                onChange={set('panNumber')}
                onBlur={onBlur('panNumber')}
                placeholder="ABCDE1234F"
              />
              <p className="text-xs text-gray-500 mt-1">10 characters, uppercase format.</p>
              {fieldError('panNumber') && <p className="text-xs text-red-600 mt-1">{fieldError('panNumber')}</p>}
            </div>

            <div>
              <label className="label">Aadhaar Number (Optional)</label>
              <input
                className="input"
                value={form.aadhaarNumber}
                maxLength={14}
                onChange={set('aadhaarNumber')}
                onBlur={onBlur('aadhaarNumber')}
                placeholder="1234 5678 9123"
              />
              <p className="text-xs text-gray-500 mt-1">12 digits if provided.</p>
              {fieldError('aadhaarNumber') && <p className="text-xs text-red-600 mt-1">{fieldError('aadhaarNumber')}</p>}
            </div>

            <div className="flex items-center justify-between pt-2">
              <button
                type="button"
                className="btn-secondary"
                onClick={() => navigate('/borrower/dashboard')}
                disabled={saving}
              >
                ← Back
              </button>
              <button type="submit" className="btn-primary" disabled={saving || !isFormValid}>
                {saving ? 'Submitting...' : 'Complete Profile →'}
              </button>
            </div>
          </form>
        </div>
      )}
    </BorrowerLayout>
  )
}
