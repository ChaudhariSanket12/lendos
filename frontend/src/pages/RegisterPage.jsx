import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function RegisterPage() {
  const { registerBorrower } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({
    firmCode: '',
    fullName: '',
    email: '',
    password: '',
    confirmPassword: '',
  })
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value })

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError(null)

    if (form.password.length < 8) {
      setError('Password must be at least 8 characters.')
      setLoading(false)
      return
    }
    if (form.password !== form.confirmPassword) {
      setError('Passwords do not match.')
      setLoading(false)
      return
    }

    try {
      await registerBorrower({
        firmCode: form.firmCode.trim(),
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        password: form.password,
      })
      navigate('/borrower/dashboard')
    } catch (err) {
      setError(
        err.response?.data?.errors?.firmCode ||
        err.response?.data?.errors?.email ||
          (err.response?.data?.message === 'Invalid firm code. Please contact your lending institution.'
            ? 'Firm not found. Please check the code or contact your lender.'
            : null) ||
          err.response?.data?.message ||
          'Registration failed. Please try again.'
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12">
      <div className="max-w-md w-full bg-white rounded-lg shadow-md p-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-1">Borrower Registration</h1>
        <p className="text-gray-500 text-sm mb-6">
          Create your borrower account to complete your profile and apply for loans.
        </p>

        {error && (
          <div className="alert-error mb-4">{error}</div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="label">Firm Code</label>
            <input
              className="input"
              type="text"
              required
              placeholder="Enter code provided by your lender (e.g., DEMO-LEN-AB12)"
              value={form.firmCode}
              onChange={set('firmCode')}
            />
          </div>

          <div>
            <label className="label">Full Name</label>
            <input
              className="input"
              type="text"
              required
              placeholder="e.g. Rajesh Sharma"
              value={form.fullName}
              onChange={set('fullName')}
            />
          </div>

          <div>
            <label className="label">Email</label>
            <input
              className="input"
              type="email"
              required
              placeholder="you@example.com"
              value={form.email}
              onChange={set('email')}
            />
          </div>

          <div>
            <label className="label">Password</label>
            <input
              className="input"
              type="password"
              required
              minLength={8}
              placeholder="Minimum 8 characters"
              value={form.password}
              onChange={set('password')}
            />
          </div>

          <div>
            <label className="label">Confirm Password</label>
            <input
              className="input"
              type="password"
              required
              placeholder="Re-enter your password"
              value={form.confirmPassword}
              onChange={set('confirmPassword')}
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="btn-primary w-full"
          >
            {loading ? 'Creating account...' : 'Create Account'}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-gray-500">
          Already have an account?{' '}
          <Link to="/login" className="text-blue-600 hover:underline">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  )
}
