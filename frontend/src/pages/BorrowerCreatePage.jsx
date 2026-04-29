import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { borrowersApi } from '../api/borrowers'

const NAME_REGEX = /^[A-Za-z ]+$/
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const normalizePhone = (value) => value.replace(/[^0-9]/g, '')

function getErrorMessage(err, fallback) {
  const backendMessage = err.response?.data?.message
  const status = err.response?.status
  const generic = err.message
  if (backendMessage && status) return `${backendMessage} (HTTP ${status})`
  return backendMessage || generic || fallback
}

function toReadableFieldName(field) {
  return field.replace(/([A-Z])/g, ' $1').replace(/^./, (c) => c.toUpperCase())
}

export default function BorrowerCreatePage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    dateOfBirth: '',
    address: '',
    createLogin: false,
    password: '',
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [showPassword, setShowPassword] = useState(false)

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value })

  const validateForm = () => {
    const errors = {}
    const firstName = form.firstName.trim()
    const lastName = form.lastName.trim()
    const email = form.email.trim()
    const phone = normalizePhone(form.phone)
    const address = form.address.trim()

    if (!firstName || firstName.length < 2 || !NAME_REGEX.test(firstName)) {
      errors.firstName = 'First name must be at least 2 characters and contain only letters and spaces'
    }
    if (!lastName || lastName.length < 2 || !NAME_REGEX.test(lastName)) {
      errors.lastName = 'Last name must be at least 2 characters and contain only letters and spaces'
    }
    if (!EMAIL_REGEX.test(email)) {
      errors.email = 'Invalid email format'
    }
    if (!(phone.length === 10 || (phone.length >= 11 && phone.length <= 15))) {
      errors.phone = 'Invalid phone number format'
    }

    if (!form.dateOfBirth) {
      errors.dateOfBirth = 'Date of birth is required'
    } else {
      const dob = new Date(form.dateOfBirth)
      const now = new Date()
      if (dob > now) {
        errors.dateOfBirth = 'Date of birth cannot be in the future'
      } else {
        let age = now.getFullYear() - dob.getFullYear()
        const monthDiff = now.getMonth() - dob.getMonth()
        if (monthDiff < 0 || (monthDiff === 0 && now.getDate() < dob.getDate())) age -= 1
        if (age < 18) errors.dateOfBirth = 'Borrower must be at least 18 years old'
      }
    }

    if (!address || address.length < 5) {
      errors.address = 'Address must be at least 5 characters'
    }

    if (form.createLogin && form.password.trim().length < 8) {
      errors.password = 'Password must be at least 8 characters when login access is enabled'
    }

    return errors
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError(null)
    setFieldErrors({})

    const clientErrors = validateForm()
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors)
      setError('Please fix the highlighted fields.')
      setLoading(false)
      return
    }

    try {
      await borrowersApi.create({
        ...form,
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        email: form.email.trim(),
        phone: form.phone.trim(),
        dateOfBirth: form.dateOfBirth,
        address: form.address.trim(),
        createLogin: form.createLogin,
        password: form.createLogin ? form.password : null,
      })

      navigate('/borrowers', { state: { message: 'Borrower added successfully.' } })
    } catch (err) {
      console.error('[BorrowerCreatePage] Failed to create borrower', err)
      const backendErrors = err.response?.data?.errors || {}
      if (backendErrors && typeof backendErrors === 'object') {
        setFieldErrors(backendErrors)
      }
      setError(getErrorMessage(err, 'Failed to create borrower.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white shadow-sm px-6 py-4 flex items-center justify-between">
        <h1 className="text-lg font-bold text-blue-700">LendOS</h1>
        <button className="btn-secondary" onClick={() => navigate('/borrowers')}>
          Back to Borrowers
        </button>
      </nav>

      <div className="max-w-2xl mx-auto p-8">
        <div className="card">
          <h2 className="text-xl font-semibold text-gray-800 mb-2">Add New Borrower</h2>
          <p className="text-sm text-gray-500 mb-6">Fill borrower details and save as draft.</p>

          {loading && <div className="alert-info mb-4">Loading...</div>}
          {error && <div className="alert-error mb-4">{error}</div>}
          {Object.keys(fieldErrors).length > 0 && (
            <div className="alert-error mb-4">
              {Object.entries(fieldErrors).map(([field, message]) => (
                <div key={field}>{toReadableFieldName(field)}: {message}</div>
              ))}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="label">First Name</label>
                <input
                  className="input"
                  value={form.firstName}
                  onChange={set('firstName')}
                  required
                />
              </div>
              <div>
                <label className="label">Last Name</label>
                <input
                  className="input"
                  value={form.lastName}
                  onChange={set('lastName')}
                  required
                />
              </div>
            </div>

            <div>
              <label className="label">Email</label>
              <input
                className="input"
                type="email"
                value={form.email}
                onChange={set('email')}
                required
              />
            </div>

            <div>
              <label className="label">Phone</label>
              <input
                className="input"
                value={form.phone}
                onChange={set('phone')}
                placeholder="+91-9876543210"
              />
            </div>

            <div>
              <label className="label">Date of Birth</label>
              <input
                className="input"
                type="date"
                value={form.dateOfBirth}
                onChange={set('dateOfBirth')}
              />
            </div>

            <div>
              <label className="label">Address</label>
              <textarea
                className="input"
                rows={3}
                value={form.address}
                onChange={set('address')}
              />
            </div>

            <div className="border border-gray-200 rounded-md p-4 bg-gray-50">
              <label className="inline-flex items-center gap-2 text-sm text-gray-700">
                <input
                  type="checkbox"
                  checked={form.createLogin}
                  onChange={(e) => setForm({ ...form, createLogin: e.target.checked })}
                />
                Create login access for this borrower
              </label>

              {form.createLogin && (
                <div className="mt-3">
                  <label className="label">Initial Password</label>
                  <div className="flex gap-2">
                    <input
                      className="input"
                      type={showPassword ? 'text' : 'password'}
                      value={form.password}
                      onChange={set('password')}
                      minLength={8}
                      required={form.createLogin}
                      placeholder="Minimum 8 characters"
                    />
                    <button
                      type="button"
                      className="btn-secondary whitespace-nowrap"
                      onClick={() => setShowPassword((prev) => !prev)}
                    >
                      {showPassword ? 'Hide' : 'Show'}
                    </button>
                  </div>
                </div>
              )}
            </div>

            <div className="flex gap-3 pt-2">
              <button type="submit" disabled={loading} className="btn-primary">
                {loading ? 'Saving...' : 'Save Borrower'}
              </button>
              <button
                type="button"
                onClick={() => navigate('/borrowers')}
                disabled={loading}
                className="btn-secondary"
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}
